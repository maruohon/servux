package fi.dy.masa.servux.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.INetHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraft.util.ResourceLocation;
import fi.dy.masa.servux.network.util.PacketUtils;
import io.netty.buffer.Unpooled;

/**
 * Network packet splitter code from QuickCarpet by skyrising
 * @author skyrising
 */
public class PacketSplitter
{
    public static final int MAX_TOTAL_PER_PACKET_S2C = 1048576;
    public static final int MAX_PAYLOAD_PER_PACKET_S2C = MAX_TOTAL_PER_PACKET_S2C - 5;
    public static final int DEFAULT_MAX_RECEIVE_SIZE_C2S = 1048576;

    private static final Map<Pair<INetHandler, ResourceLocation>, ReadingSession> READING_SESSIONS = new HashMap<>();

    public static void send(PacketBuffer packet, ResourceLocation channel, ServerPlayNetHandler networkHandler)
    {
        send(packet, MAX_PAYLOAD_PER_PACKET_S2C, buf -> networkHandler.sendPacket(new SCustomPayloadPlayPacket(channel, buf)));
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
    public static PacketBuffer receive(ResourceLocation channel, PacketBuffer data, ServerPlayNetHandler networkHandler)
    {
        return receive(channel, data, DEFAULT_MAX_RECEIVE_SIZE_C2S, networkHandler);
    }

    @Nullable
    private static PacketBuffer receive(ResourceLocation channel, PacketBuffer data, int maxLength, ServerPlayNetHandler networkHandler)
    {
        Pair<INetHandler, ResourceLocation> key = Pair.of(networkHandler, channel);
        return READING_SESSIONS.computeIfAbsent(key, ReadingSession::new).receive(data, maxLength);
    }

    /**
     * Sends a packet type ID as a VarInt, and then the given Compound tag.
     */
    public static void sendPacketTypeAndCompound(ResourceLocation channel, int packetType, CompoundNBT data, ServerPlayerEntity player)
    {
        sendPacketTypeAndCompound(channel, packetType, data, player.connection);
    }

    /**
     * Sends a packet type ID as a VarInt, and then the given Compound tag.
     */
    public static void sendPacketTypeAndCompound(ResourceLocation channel, int packetType, CompoundNBT data, ServerPlayNetHandler networkHandler)
    {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeVarInt(packetType);
        buf.writeCompoundTag(data);

        send(buf, channel, networkHandler);
    }

    private static class ReadingSession
    {
        private final Pair<INetHandler, ResourceLocation> key;
        private int expectedSize = -1;
        private PacketBuffer received;

        private ReadingSession(Pair<INetHandler, ResourceLocation> key)
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
