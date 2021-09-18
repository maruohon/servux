package fi.dy.masa.servux.mixin;

import java.util.function.BooleanSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.profiler.IProfiler;
import net.minecraft.server.MinecraftServer;
import fi.dy.masa.servux.dataproviders.DataProviderManager;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer
{
    @Shadow private IProfiler profiler;
    @Shadow private int tickCounter;

    @Inject(method = "tick", at = @At("RETURN"))
    private void servux_onTickEnd(BooleanSupplier supplier, CallbackInfo ci)
    {
        this.profiler.startSection("servux_tick");
        DataProviderManager.INSTANCE.tickProviders((MinecraftServer) (Object) this, this.tickCounter);
        this.profiler.endSection();
    }
}
