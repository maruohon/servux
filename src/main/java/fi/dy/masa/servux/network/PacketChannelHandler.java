package fi.dy.masa.servux.network;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import com.google.common.base.Charsets;
import fi.dy.masa.servux.network.util.PacketUtils;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;

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
                                      PacketBuffer data,
                                      ClientPacketHandler handler,
                                      NetHandlerPlayServer netHandler)
    {
        PacketBuffer buf;

        if (handler.usePacketSplitter())
        {
            buf = PacketSplitter.receive(channel, data, netHandler);
            data.release();
        }
        else
        {
            buf = data;
        }

        // Finished the complete packet
        if (buf != null)
        {
            handler.onPacketReceived(buf, netHandler);
            buf.release();
            //System.out.printf("received data handled for channel '%s'\n", channel);
        }
    }

    protected void updateRegistrationForChannels(List<String> channels,
                                                 BiConsumer<ClientPacketHandler, NetHandlerPlayServer> action,
                                                 NetHandlerPlayServer netHandler)
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

    public void processPacketFromClient(CPacketCustomPayload packet, NetHandlerPlayServer netHandler)
    {
        String channel = packet.getChannelName();
        PacketBuffer data = packet.getBufferData();
        ClientPacketHandler handler;

        synchronized (this.handlers)
        {
            handler = this.handlers.get(channel);
        }

        if (handler != null)
        {
            final PacketBuffer slice = PacketUtils.retainedSlice(data);
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

    public static void schedule(Runnable task, NetHandlerPlayServer netHandler)
    {
        netHandler.player.server.addScheduledTask(task);
    }

    public static List<String> getChannels(PacketBuffer buf)
    {
        buf = PacketUtils.slice(buf);
        buf.readerIndex(0);

        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        String channelString = new String(bytes, Charsets.UTF_8);
        return Arrays.asList(channelString.split("\0"));
    }
}
