package fi.dy.masa.servux.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.google.common.base.Charsets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.Identifier;
import fi.dy.masa.servux.mixin.IMixinCustomPayloadC2SPacket;

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

    public void unregisterServerChannelHandler(IPluginChannelHandler handler)
    {
        List<Identifier> toUnRegister = new ArrayList<>();
        Identifier channel = handler.getChannel();

        if (this.handlers.remove(channel, handler))
        {
            toUnRegister.add(channel);
            this.subscribableHandlers.remove(channel);
        }
    }

    public boolean processPacketFromClient(CustomPayloadC2SPacket packet, ServerPlayNetworkHandler netHandler)
    {
        IMixinCustomPayloadC2SPacket accessor = ((IMixinCustomPayloadC2SPacket) packet);
        Identifier channel = accessor.getChannel();
        PacketByteBuf data = accessor.getData();

        IPluginChannelHandler handler = this.handlers.get(channel);

        if (handler != null)
        {
            PacketByteBuf buf = PacketSplitter.receive(netHandler, packet);

            // Finished the complete packet
            if (buf != null)
            {
                handler.onPacketReceived(buf, netHandler);
            }

            return true;
        }
        else if (channel.equals(REGISTER))
        {
            data.readerIndex(0);

            for (Identifier regChannel : getChannels(data))
            {
                handler = this.subscribableHandlers.get(regChannel);

                if (handler != null)
                {
                    handler.subscribe(netHandler);
                }
            }

            data.readerIndex(0);

            return true;
        }
        else if (channel.equals(UNREGISTER))
        {
            data.readerIndex(0);

            for (Identifier unregChannel : getChannels(data))
            {
                handler = this.subscribableHandlers.get(unregChannel);

                if (handler != null)
                {
                    handler.unsubscribe(netHandler);
                }
            }

            data.readerIndex(0);

            return true;
        }

        return false;
    }

    private static List<Identifier> getChannels(PacketByteBuf buf)
    {
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
