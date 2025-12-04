package fi.dy.masa.abpt.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 1010)
public class MixinServerGamePacketListenerImpl
{
    @Redirect(method = "handleUseItemOn", require = 0,
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 abpt_removeHitPosCheck(Vec3 hitVec, Vec3 blockCenter)
    {
        //System.out.printf("abpt_removeHitPosCheck\n");
        return Vec3.ZERO;
    }
}
