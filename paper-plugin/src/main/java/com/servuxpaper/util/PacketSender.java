package com.servuxpaper.util;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;

public final class PacketSender {
    public static final int MAX_PLUGIN_MESSAGE_SIZE = 32767;

    private PacketSender() {}

    public static void sendNbt(Plugin plugin, Player player, String channel, int packetType, CompoundTag nbt) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(packetType);
        buf.writeNbt(nbt);
        byte[] bytes = ByteBufUtil.getBytes(buf);
        buf.release();
        player.sendPluginMessage(plugin, channel, bytes);
    }

    public static void sendBuffer(Plugin plugin, Player player, String channel, int packetType, byte[] buffer) {
        int headerSize = VarInts.sizeOfVarInt(packetType);
        byte[] out = new byte[headerSize + buffer.length];
        ByteArrayOutputStream baos = new ByteArrayOutputStream(out.length);
        VarInts.writeVarInt(baos, packetType);
        baos.writeBytes(buffer);
        player.sendPluginMessage(plugin, channel, baos.toByteArray());
    }

    public static void sendSplitBuffer(Plugin plugin, Player player, String channel, int packetType, byte[] buffer) {
        int totalLen = buffer.length;
        int offset = 0;
        boolean first = true;
        while (offset < totalLen) {
            int headerSize = VarInts.sizeOfVarInt(packetType) + (first ? VarInts.sizeOfVarInt(totalLen) : 0);
            int maxData = MAX_PLUGIN_MESSAGE_SIZE - headerSize;
            int len = Math.min(maxData, totalLen - offset);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(headerSize + len);
            VarInts.writeVarInt(baos, packetType);
            if (first) {
                VarInts.writeVarInt(baos, totalLen);
            }
            baos.write(buffer, offset, len);
            player.sendPluginMessage(plugin, channel, baos.toByteArray());
            offset += len;
            first = false;
        }
    }
}
