package fi.dy.masa.servux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CCustomPayloadPacket;
import fi.dy.masa.servux.network.ServerPacketChannelHandler;

@Mixin(value = ServerPlayNetHandler.class, priority = 998)
public abstract class MixinServerPlayNetHandler
{
    @Inject(method = "processCustomPayload", at = @At("HEAD"))
    private void servux_handleCustomPayload(CCustomPayloadPacket packet, CallbackInfo ci)
    {
        ServerPacketChannelHandler.INSTANCE.processPacketFromClient(packet, (ServerPlayNetHandler) (Object) this);
    }
}
