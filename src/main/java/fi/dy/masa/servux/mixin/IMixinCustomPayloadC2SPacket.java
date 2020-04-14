package fi.dy.masa.servux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.server.network.packet.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

@Mixin(CustomPayloadC2SPacket.class)
public interface IMixinCustomPayloadC2SPacket
{
    @Accessor("channel")
    Identifier getChannel();

    @Accessor("data")
    PacketByteBuf getData();
}
