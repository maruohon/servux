package fi.dy.masa.servux.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import com.google.common.base.Charsets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.Identifier;
import fi.dy.masa.servux.mixin.IMixinCustomPayloadC2SPacket;
import fi.dy.masa.servux.network.util.PacketUtils;

public class ServerPacketChannelHandler
{
    public static final Identifier REGISTER = new Identifier("minecraft:register");
    public static final Identifier UNREGISTER = new Identifier("minecraft:unregister");

    public static final ServerPacketChannelHandler INSTANCE = new ServerPacketChannelHandler();

    private final HashMap<Identifier, IPluginChannelHandler> handlers = new HashMap<>();
    private final HashMap<Identifier, IPluginChannelHandler> subscribableHandlers = new HashMap<>();

    private ServerPacketChannelHandler()
    {
    }

    public void registerServerChannelHandler(IPluginChannelHandler handler)
    {
        synchronized (this.handlers)
        {
            List<Identifier> toRegister = new ArrayList<>();
            Identifier channel = handler.getChannel();

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
            List<Identifier> toUnRegister = new ArrayList<>();
            Identifier channel = handler.getChannel();

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
        Identifier channel = accessor.servux_getChannel();
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
            final List<Identifier> channels = getChannels(data);
            this.schedule(() -> this.updateRegistrationForChannels(channels, IPluginChannelHandler::subscribe, netHandler), netHandler);
        }
        else if (channel.equals(UNREGISTER))
        {
            final List<Identifier> channels = getChannels(data);
            this.schedule(() -> this.updateRegistrationForChannels(channels, IPluginChannelHandler::unsubscribe, netHandler), netHandler);
        }
    }

    private void updateRegistrationForChannels(List<Identifier> channels,
                                               BiConsumer<IPluginChannelHandler, ServerPlayNetworkHandler> action,
                                               ServerPlayNetworkHandler netHandler)
    {
        for (Identifier channel : channels)
        {
            IPluginChannelHandler handler = this.subscribableHandlers.get(channel);

            if (handler != null)
            {
                action.accept(handler, netHandler);
            }
        }
    }

    private void handleReceivedData(Identifier channel, PacketByteBuf data,
                                    IPluginChannelHandler handler, ServerPlayNetworkHandler netHandler)
    {
        PacketByteBuf buf = PacketSplitter.receive(channel, data, netHandler);
        data.release();

        // Finished the complete packet
        if (buf != null)
        {
            handler.onPacketReceived(buf, netHandler);
            buf.release();
        }
    }

    private void schedule(Runnable task, ServerPlayNetworkHandler netHandler)
    {
        netHandler.player.server.execute(task);
    }

    private static List<Identifier> getChannels(PacketByteBuf buf)
    {
        buf = PacketUtils.slice(buf);
        buf.readerIndex(0);

        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        String channelString = new String(bytes, Charsets.UTF_8);
        List<Identifier> channels = new ArrayList<>();

        for (String channel : channelString.split("\0"))
        {
            try
            {
                Identifier id = new Identifier(channel);
                channels.add(id);
            }
            catch (Exception ignore)
            {
            }
        }

        return channels;
    }
}
