package fi.dy.masa.servux.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import fi.dy.masa.servux.network.PacketChannelHandler;

@Mixin(value = ServerPlayNetworkHandler.class, priority = 998)
public abstract class MixinServerPlayNetworkHandler
{
    @Inject(method = "onCustomPayload", at = @At("HEAD"))
    private void servux_onCustomPayload(CustomPayloadC2SPacket packet, CallbackInfo ci)
    {
        PacketChannelHandler.INSTANCE.processPacketFromClient(packet, (ServerPlayNetworkHandler) (Object) this);
    }
}
