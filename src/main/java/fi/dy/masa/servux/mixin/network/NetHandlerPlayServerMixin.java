package fi.dy.masa.servux.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.servux.network.PacketChannelHandler;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.CPacketCustomPayload;

@Mixin(value = NetHandlerPlayServer.class, priority = 998)
public abstract class NetHandlerPlayServerMixin
{
    @Inject(method = "processCustomPayload", at = @At("HEAD"))
    private void servux_onCustomPayload(CPacketCustomPayload packet, CallbackInfo ci)
    {
        PacketChannelHandler.INSTANCE.processPacketFromClient(packet, (NetHandlerPlayServer) (Object) this);
    }
}
