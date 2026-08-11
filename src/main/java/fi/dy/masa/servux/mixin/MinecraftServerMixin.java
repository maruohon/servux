package fi.dy.masa.servux.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.servux.dataprovider.DataProviderManager;

import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin
{
    @Shadow @Final public Profiler profiler;
    @Shadow private int tickCounter;

    @Inject(method = "tick", at = @At("RETURN"))
    private void servux_onTickEnd(CallbackInfo ci)
    {
        this.profiler.startSection("servux_tick");
        DataProviderManager.INSTANCE.tickProviders((MinecraftServer) (Object) this, this.tickCounter);
        this.profiler.endSection();
    }
}
