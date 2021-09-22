package fi.dy.masa.servux.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import com.google.common.base.Charsets;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.PacketByteBuf;
import fi.dy.masa.servux.mixin.IMixinCustomPayloadC2SPacket;
import fi.dy.masa.servux.network.util.PacketUtils;
import fi.dy.masa.servux.util.MessageUtils;

public class ServerPacketChannelHandler
{
    public static final String REGISTER = "minecraft:register";
    public static final String UNREGISTER = "minecraft:unregister";

    public static final ServerPacketChannelHandler INSTANCE = new ServerPacketChannelHandler();

    private final HashMap<String, IPluginChannelHandler> handlers = new HashMap<>();
    private final HashMap<String, IPluginChannelHandler> subscribableHandlers = new HashMap<>();

    private ServerPacketChannelHandler()
    {
    }

    public void registerServerChannelHandler(IPluginChannelHandler handler)
    {
        synchronized (this.handlers)
        {
            List<String> toRegister = new ArrayList<>();
            String channel = handler.getChannel().toString();

            if (this.handlers.containsKey(channel) == false)
            {
                this.handlers.put(channel, handler);
                toRegister.add(channel);

                if (handler.isSubscribable())
                {
                    this.subscribableHandlers.put(channel, handler);
                }
            }
        }
    }

    public void unregisterServerChannelHandler(IPluginChannelHandler handler)
    {
        synchronized (this.handlers)
        {
            List<String> toUnRegister = new ArrayList<>();
            String channel = handler.getChannel().toString();

            if (this.handlers.remove(channel, handler))
            {
                toUnRegister.add(channel);
                this.subscribableHandlers.remove(channel);
            }
        }
    }

    public void processPacketFromClient(CustomPayloadC2SPacket packet, ServerPlayNetworkHandler netHandler)
    {
        IMixinCustomPayloadC2SPacket accessor = ((IMixinCustomPayloadC2SPacket) packet);
        String channel = accessor.servux_getChannel();
        PacketByteBuf data = accessor.servux_getData();

        IPluginChannelHandler handler;

        synchronized (this.handlers)
        {
            handler = this.handlers.get(channel);
        }

        if (handler != null)
        {
            final PacketByteBuf slice = PacketUtils.retainedSlice(data);
            this.schedule(() -> this.handleReceivedData(channel, slice, handler, netHandler), netHandler);
        }
        else if (channel.equals(REGISTER))
        {
            final List<String> channels = getChannels(data);
            this.schedule(() -> this.updateRegistrationForChannels(channels, IPluginChannelHandler::subscribe, netHandler), netHandler);
        }
        else if (channel.equals(UNREGISTER))
        {
            final List<String> channels = getChannels(data);
            this.schedule(() -> this.updateRegistrationForChannels(channels, IPluginChannelHandler::unsubscribe, netHandler), netHandler);
        }
    }

    private void updateRegistrationForChannels(List<String> channels,
                                               BiConsumer<IPluginChannelHandler, ServerPlayNetworkHandler> action,
                                               ServerPlayNetworkHandler netHandler)
    {
        for (String channel : channels)
        {
            IPluginChannelHandler handler = this.subscribableHandlers.get(channel);

            if (handler != null)
            {
                action.accept(handler, netHandler);
                System.out.printf("updateRegistrationForChannels: '%s'\n", channel);
            }
        }
    }

    private void handleReceivedData(String channel, PacketByteBuf data,
                                    IPluginChannelHandler handler, ServerPlayNetworkHandler netHandler)
    {
        PacketByteBuf buf = PacketSplitter.receive(channel, data, netHandler);
        data.release();

        // Finished the complete packet
        if (buf != null)
        {
            handler.onPacketReceived(buf, netHandler);
            buf.release();
            System.out.printf("received data handled for channel '%s'\n", channel);
        }
    }

    private void schedule(Runnable task, ServerPlayNetworkHandler netHandler)
    {
        netHandler.player.server.addTask(task);
    }

    private static List<String> getChannels(PacketByteBuf buf)
    {
        buf = PacketUtils.slice(buf);
        buf.readerIndex(0);

        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        String channelString = new String(bytes, Charsets.UTF_8);
        return Arrays.asList(channelString.split("\0"));
    }
}
