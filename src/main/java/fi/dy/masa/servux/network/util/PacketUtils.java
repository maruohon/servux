package fi.dy.masa.servux.network.util;

import java.util.Objects;
import com.google.gson.JsonElement;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

public class PacketUtils
{
    /**
     * Wraps the newly created buf from {@code buf.slice()} in a PacketByteBuf.
     *
     * @param buf the original ByteBuf
     * @return a slice of the buffer
     * @see io.netty.buffer.ByteBuf#slice()
     */
    public static PacketByteBuf slice(ByteBuf buf)
    {
        Objects.requireNonNull(buf, "PacketUtils#slice(): ByteBuf cannot be null");
        return new PacketByteBuf(buf.slice());
    }

    /**
     * Wraps the newly created buf from {@code buf.retainedSlice()} in a PacketByteBuf.
     *
     * @param buf the original ByteBuf
     * @return a slice of the buffer
     * @see io.netty.buffer.ByteBuf#retainedSlice()
     */
    public static PacketByteBuf retainedSlice(ByteBuf buf)
    {
        Objects.requireNonNull(buf, "PacketUtils#retainedSlice(): ByteBuf cannot be null");
        return new PacketByteBuf(buf.retainedSlice());
    }

    /**
     * Sends a packet type ID as a VarInt, and then the given Compound tag.
     */
    public static void sendPacketTypeAndCompound(Identifier channel,
                                                 int packetType,
                                                 CompoundTag data,
                                                 ServerPlayNetworkHandler networkHandler)
    {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(packetType);
        buf.writeCompoundTag(data);

        networkHandler.sendPacket(new CustomPayloadS2CPacket(channel.toString(), buf));
    }

    public static void send(Identifier channel, PacketByteBuf packet, ServerPlayNetworkHandler networkHandler)
    {
        networkHandler.sendPacket(new CustomPayloadS2CPacket(channel.toString(), packet));
    }

    public static void sendJson(Identifier channel, JsonElement element, ServerPlayNetworkHandler networkHandler)
    {
        sendString(channel, element.toString(), networkHandler);
    }

    public static void sendString(Identifier channel, String str, ServerPlayNetworkHandler networkHandler)
    {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(str);
        networkHandler.sendPacket(new CustomPayloadS2CPacket(channel.toString(), buf));
    }
}
