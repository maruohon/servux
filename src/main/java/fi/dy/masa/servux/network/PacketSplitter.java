package fi.dy.masa.servux.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import fi.dy.masa.servux.network.util.PacketUtils;

/**
 * Network packet splitter code from QuickCarpet by skyrising
 * @author skyrising
 */
public class PacketSplitter
{
    public static final int MAX_TOTAL_PER_PACKET_S2C = 1048576;
    public static final int MAX_PAYLOAD_PER_PACKET_S2C = MAX_TOTAL_PER_PACKET_S2C - 5;
    public static final int DEFAULT_MAX_RECEIVE_SIZE_C2S = 1048576;

    private static final Map<Pair<PacketListener, Identifier>, ReadingSession> READING_SESSIONS = new HashMap<>();

    public static void send(PacketByteBuf packet, Identifier channel, ServerPlayNetworkHandler networkHandler)
    {
        send(packet, MAX_PAYLOAD_PER_PACKET_S2C, buf -> networkHandler.sendPacket(new CustomPayloadS2CPacket(channel, buf)));
    }

    private static void send(PacketByteBuf packet, int payloadLimit, Consumer<PacketByteBuf> sender)
    {
        int len = packet.writerIndex();

        packet.readerIndex(0);

        for (int offset = 0; offset < len; offset += payloadLimit)
        {
            int thisLen = Math.min(len - offset, payloadLimit);
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer(thisLen));

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
    public static PacketByteBuf receive(Identifier channel, PacketByteBuf data, ServerPlayNetworkHandler networkHandler)
    {
        return receive(channel, data, DEFAULT_MAX_RECEIVE_SIZE_C2S, networkHandler);
    }

    @Nullable
    private static PacketByteBuf receive(Identifier channel, PacketByteBuf data, int maxLength, ServerPlayNetworkHandler networkHandler)
    {
        Pair<PacketListener, Identifier> key = Pair.of(networkHandler, channel);
        return READING_SESSIONS.computeIfAbsent(key, ReadingSession::new).receive(data, maxLength);
    }

    /**
     * Sends a packet type ID as a VarInt, and then the given Compound tag.
     */
    public static void sendPacketTypeAndCompound(Identifier channel, int packetType, NbtCompound data, ServerPlayerEntity player)
    {
        sendPacketTypeAndCompound(channel, packetType, data, player.networkHandler);
    }

    /**
     * Sends a packet type ID as a VarInt, and then the given Compound tag.
     */
    public static void sendPacketTypeAndCompound(Identifier channel, int packetType, NbtCompound data, ServerPlayNetworkHandler networkHandler)
    {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(packetType);
        buf.writeNbt(data);

        send(buf, channel, networkHandler);
    }

    private static class ReadingSession
    {
        private final Pair<PacketListener, Identifier> key;
        private int expectedSize = -1;
        private PacketByteBuf received;

        private ReadingSession(Pair<PacketListener, Identifier> key)
        {
            this.key = key;
        }

        @Nullable
        private PacketByteBuf receive(PacketByteBuf data, int maxLength)
        {
            data.readerIndex(0);
            data = PacketUtils.slice(data);

            if (this.expectedSize < 0)
            {
                this.expectedSize = data.readVarInt();

                if (this.expectedSize > maxLength)
                {
                    throw new IllegalArgumentException("Payload too large");
                }

                this.received = new PacketByteBuf(Unpooled.buffer(this.expectedSize));
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
