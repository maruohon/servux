package fi.dy.masa.servux.network.util;

import java.util.Objects;
import net.minecraft.network.PacketBuffer;
import io.netty.buffer.ByteBuf;

public class PacketUtils
{
    /**
     * Wraps the newly created buf from {@code buf.slice()} in a PacketByteBuf.
     *
     * @param buf the original ByteBuf
     * @return a slice of the buffer
     * @see ByteBuf#slice()
     */
    public static PacketBuffer slice(ByteBuf buf)
    {
        Objects.requireNonNull(buf, "ByteBuf cannot be null");
        return new PacketBuffer(buf.slice());
    }

    /**
     * Wraps the newly created buf from {@code buf.retainedSlice()} in a PacketByteBuf.
     *
     * @param buf the original ByteBuf
     * @return a slice of the buffer
     * @see ByteBuf#retainedSlice()
     */
    public static PacketBuffer retainedSlice(ByteBuf buf)
    {
        Objects.requireNonNull(buf, "ByteBuf cannot be null");
        return new PacketBuffer(buf.retainedSlice());
    }
}
