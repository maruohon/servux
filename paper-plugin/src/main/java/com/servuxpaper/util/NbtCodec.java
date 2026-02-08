package com.servuxpaper.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public final class NbtCodec {
    private NbtCodec() {}

    public static byte[] encodeNbt(CompoundTag tag) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeNbt(tag);
        byte[] bytes = ByteBufUtil.getBytes((ByteBuf) buf);
        buf.release();
        return bytes;
    }

    public static CompoundTag decodeNbt(byte[] bytes) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        CompoundTag tag = buf.readNbt();
        buf.release();
        return tag;
    }
}
