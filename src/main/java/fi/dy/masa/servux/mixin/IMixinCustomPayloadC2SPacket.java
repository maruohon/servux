package fi.dy.masa.servux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.util.PacketByteBuf;

@Mixin(CustomPayloadC2SPacket.class)
public interface IMixinCustomPayloadC2SPacket
{
    @Accessor("field_31341")
    String servux_getChannel();

    @Accessor("data")
    PacketByteBuf servux_getData();
}
