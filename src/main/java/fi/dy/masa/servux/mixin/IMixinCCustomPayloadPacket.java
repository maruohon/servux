package fi.dy.masa.servux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CCustomPayloadPacket;
import net.minecraft.util.ResourceLocation;

@Mixin(CCustomPayloadPacket.class)
public interface IMixinCCustomPayloadPacket
{
    @Accessor("channel")
    ResourceLocation servux_getChannel();

    @Accessor("data")
    PacketBuffer servux_getData();
}
