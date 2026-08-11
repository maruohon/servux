package fi.dy.masa.servux.network.util;

import java.util.Objects;
import com.google.gson.JsonElement;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.util.ResourceLocation;

public class PacketUtils
{
    /**
     * Wraps the newly created buf from {@code buf.slice()} in a PacketByteBuf.
     *
     * @param buf the original ByteBuf
     * @return a slice of the buffer
     * @see io.netty.buffer.ByteBuf#slice()
     */
    public static PacketBuffer slice(ByteBuf buf)
    {
        Objects.requireNonNull(buf, "PacketUtils#slice(): ByteBuf cannot be null");
        return new PacketBuffer(buf.slice());
    }

    /**
     * Wraps the newly created buf from {@code buf.retainedSlice()} in a PacketByteBuf.
     *
     * @param buf the original ByteBuf
     * @return a slice of the buffer
     * @see io.netty.buffer.ByteBuf#retainedSlice()
     */
    public static PacketBuffer retainedSlice(ByteBuf buf)
    {
        Objects.requireNonNull(buf, "PacketUtils#retainedSlice(): ByteBuf cannot be null");
        return new PacketBuffer(buf.retainedSlice());
    }

    /**
     * Sends a packet type ID as a VarInt, and then the given Compound tag.
     */
    public static void sendPacketTypeAndCompound(ResourceLocation channel,
                                                 int packetType,
                                                 NBTTagCompound data,
                                                 NetHandlerPlayServer networkHandler)
    {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeVarInt(packetType);
        buf.writeCompoundTag(data);

        networkHandler.sendPacket(new SPacketCustomPayload(channel.toString(), buf));
    }

    public static void send(ResourceLocation channel, PacketBuffer packet, NetHandlerPlayServer networkHandler)
    {
        networkHandler.sendPacket(new SPacketCustomPayload(channel.toString(), packet));
    }

    public static void sendJson(ResourceLocation channel, JsonElement element, NetHandlerPlayServer networkHandler)
    {
        sendString(channel, element.toString(), networkHandler);
    }

    public static void sendString(ResourceLocation channel, String str, NetHandlerPlayServer networkHandler)
    {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeString(str);
        networkHandler.sendPacket(new SPacketCustomPayload(channel.toString(), buf));
    }
}
