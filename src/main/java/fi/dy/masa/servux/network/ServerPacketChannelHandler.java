package fi.dy.masa.servux.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import com.google.common.base.Charsets;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.packet.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;
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
            for (Identifier regChannel : getChannels(accessor.getData()))
            {
                handler = this.subscribableHandlers.get(regChannel);

                if (handler != null)
                {
                    handler.subscribe(netHandler);
                }
            }

            return true;
        }
        else if (channel.equals(UNREGISTER))
        {
            for (Identifier unregChannel : getChannels(accessor.getData()))
            {
                handler = this.subscribableHandlers.get(unregChannel);

                if (handler != null)
                {
                    handler.unsubscribe(netHandler);
                }
            }

            return true;
        }

        return false;
    }

    private static List<Identifier> getChannels(PacketByteBuf buff)
    {
        buff.readerIndex(0);
        byte[] bytes = new byte[buff.readableBytes()];
        buff.readBytes(bytes);
        String channelString = new String(bytes, Charsets.UTF_8);
        return Arrays.stream(channelString.split("\0")).map(Identifier::new).collect(Collectors.toList());
    }
}
