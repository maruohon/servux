package fi.dy.masa.servux.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.tuple.Pair;
import fi.dy.masa.servux.network.util.PacketUtils;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.util.ResourceLocation;

/**
 * Network packet splitter code from QuickCarpet by skyrising
 * @author skyrising
 */
public class PacketSplitter
{
    public static final int MAX_TOTAL_PER_PACKET_S2C = 1048576;
    public static final int MAX_PAYLOAD_PER_PACKET_S2C = MAX_TOTAL_PER_PACKET_S2C - 5;
    public static final int DEFAULT_MAX_RECEIVE_SIZE_C2S = 1048576;

    private static final Map<Pair<INetHandler, String>, ReadingSession> READING_SESSIONS = new HashMap<>();

    public static void send(ResourceLocation channel, PacketBuffer packet, NetHandlerPlayServer networkHandler)
    {
        send(packet, MAX_PAYLOAD_PER_PACKET_S2C, buf -> networkHandler.sendPacket(new SPacketCustomPayload(channel.toString(), buf)));
    }

    private static void send(PacketBuffer packet, int payloadLimit, Consumer<PacketBuffer> sender)
    {
        int len = packet.writerIndex();

        packet.readerIndex(0);

        for (int offset = 0; offset < len; offset += payloadLimit)
        {
            int thisLen = Math.min(len - offset, payloadLimit);
            PacketBuffer buf = new PacketBuffer(Unpooled.buffer(thisLen));

            if (offset == 0)
            {
                buf.writeVarInt(len);
            }

            buf.writeBytes(packet, thisLen);
            sender.accept(buf);
        }

        packet.release();
    }

    @Nullable
    public static PacketBuffer receive(String channel, PacketBuffer data, NetHandlerPlayServer networkHandler)
    {
        return receive(channel, data, DEFAULT_MAX_RECEIVE_SIZE_C2S, networkHandler);
    }

    @Nullable
    private static PacketBuffer receive(String channel, PacketBuffer data, int maxLength, NetHandlerPlayServer networkHandler)
    {
        Pair<INetHandler, String> key = Pair.of(networkHandler, channel);
        return READING_SESSIONS.computeIfAbsent(key, ReadingSession::new).receive(data, maxLength);
    }

    /**
     * Sends a packet type ID as a VarInt, and then the given Compound tag.
     */
    public static void sendPacketTypeAndCompound(ResourceLocation channel, int packetType, NBTTagCompound data, EntityPlayerMP player)
    {
        sendPacketTypeAndCompound(channel, packetType, data, player.connection);
    }

    /**
     * Sends a packet type ID as a VarInt, and then the given Compound tag.
     */
    public static void sendPacketTypeAndCompound(ResourceLocation channel, int packetType, NBTTagCompound data, NetHandlerPlayServer networkHandler)
    {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeVarInt(packetType);
        buf.writeCompoundTag(data);

        send(channel, buf, networkHandler);
    }

    private static class ReadingSession
    {
        private final Pair<INetHandler, String> key;
        private int expectedSize = -1;
        private PacketBuffer received;

        private ReadingSession(Pair<INetHandler, String> key)
        {
            this.key = key;
        }

        @Nullable
        private PacketBuffer receive(PacketBuffer data, int maxLength)
        {
            data = PacketUtils.slice(data);
            data.readerIndex(0);

            if (this.expectedSize < 0)
            {
                this.expectedSize = data.readVarInt();

                if (this.expectedSize > maxLength)
                {
                    throw new IllegalArgumentException("Payload too large");
                }

                this.received = new PacketBuffer(Unpooled.buffer(this.expectedSize));
            }

            this.received.writeBytes(data.readBytes(data.readableBytes()));

            if (this.received.writerIndex() >= this.expectedSize)
            {
                READING_SESSIONS.remove(this.key);
                return this.received;
            }

            return null;
        }
    }
}
