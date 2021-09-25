package fi.dy.masa.servux.mixin.infosub;

import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.ScheduledTick;

@Mixin(ServerWorld.class)
public interface IMixinServerWorld
{
    @Accessor("field_31702")
    Set<ScheduledTick> servux_getScheduledTicks();
}
