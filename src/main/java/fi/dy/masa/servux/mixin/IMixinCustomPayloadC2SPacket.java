package fi.dy.masa.servux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;

@Mixin(CustomPayloadC2SPacket.class)
public interface IMixinCustomPayloadC2SPacket
{
    @Accessor("channel")
    Identifier servux_getChannel();

    @Accessor("data")
    PacketByteBuf servux_getData();
}
