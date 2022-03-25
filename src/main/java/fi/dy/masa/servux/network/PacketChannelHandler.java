package fi.dy.masa.servux.network;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import com.google.common.base.Charsets;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.PacketByteBuf;
import fi.dy.masa.servux.mixin.network.IMixinCustomPayloadC2SPacket;
import fi.dy.masa.servux.network.util.PacketUtils;

public class PacketChannelHandler
{
    public static final String REGISTER = "minecraft:register";
    public static final String UNREGISTER = "minecraft:unregister";

    public static final PacketChannelHandler INSTANCE = new PacketChannelHandler();

    private final HashMap<String, ClientPacketHandler> handlers = new HashMap<>();
    private final HashMap<String, ClientPacketHandler> subscribableHandlers = new HashMap<>();

    public void registerServerChannelHandler(ClientPacketHandler handler)
    {
        synchronized (this.handlers)
        {
            String channel = handler.getChannel().toString();

            if (this.handlers.containsKey(channel) == false)
            {
                this.handlers.put(channel, handler);

                if (handler.canSubscribe())
                {
                    this.subscribableHandlers.put(channel, handler);
                }
            }
        }
    }

    public void unregisterServerChannelHandler(ClientPacketHandler handler)
    {
        synchronized (this.handlers)
        {
            String channel = handler.getChannel().toString();

            if (this.handlers.remove(channel, handler))
            {
                this.subscribableHandlers.remove(channel);
            }
        }
    }

    protected void handleReceivedData(String channel,
                                      PacketByteBuf data,
                                      ClientPacketHandler handler,
                                      ServerPlayNetworkHandler netHandler)
    {
        PacketByteBuf buf = PacketSplitter.receive(channel, data, netHandler);
        data.release();

        // Finished the complete packet
        if (buf != null)
        {
            handler.onPacketReceived(buf, netHandler);
            buf.release();
            //System.out.printf("received data handled for channel '%s'\n", channel);
        }
    }

    protected void updateRegistrationForChannels(List<String> channels,
                                                 BiConsumer<ClientPacketHandler, ServerPlayNetworkHandler> action,
                                                 ServerPlayNetworkHandler netHandler)
    {
        for (String channel : channels)
        {
            ClientPacketHandler handler = this.subscribableHandlers.get(channel);

            if (handler != null)
            {
                action.accept(handler, netHandler);
                //System.out.printf("updateRegistrationForChannels: '%s'\n", channel);
            }
        }
    }

    public void processPacketFromClient(CustomPayloadC2SPacket packet, ServerPlayNetworkHandler netHandler)
    {
        IMixinCustomPayloadC2SPacket accessor = ((IMixinCustomPayloadC2SPacket) packet);
        String channel = accessor.servux_getChannel();
        PacketByteBuf data = accessor.servux_getData();
        ClientPacketHandler handler;

        synchronized (this.handlers)
        {
            handler = this.handlers.get(channel);
        }

        if (handler != null)
        {
            final PacketByteBuf slice = PacketUtils.retainedSlice(data);
            schedule(() -> this.handleReceivedData(channel, slice, handler, netHandler), netHandler);
        }
        else if (channel.equals(REGISTER))
        {
            final List<String> channels = getChannels(data);
            schedule(() -> this.updateRegistrationForChannels(channels, ClientPacketHandler::subscribe, netHandler), netHandler);
        }
        else if (channel.equals(UNREGISTER))
        {
            final List<String> channels = getChannels(data);
            schedule(() -> this.updateRegistrationForChannels(channels, ClientPacketHandler::unsubscribe, netHandler), netHandler);
        }
    }

    public static void schedule(Runnable task, ServerPlayNetworkHandler netHandler)
    {
        netHandler.player.server.addTask(task);
    }

    public static List<String> getChannels(PacketByteBuf buf)
    {
        buf = PacketUtils.slice(buf);
        buf.readerIndex(0);

        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        String channelString = new String(bytes, Charsets.UTF_8);
        return Arrays.asList(channelString.split("\0"));
    }
}
