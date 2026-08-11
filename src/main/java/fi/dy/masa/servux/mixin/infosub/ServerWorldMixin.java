package fi.dy.masa.servux.mixin.infosub;

import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;

@Mixin(WorldServer.class)
public interface ServerWorldMixin
{
    @Accessor("pendingTickListEntriesHashSet")
    Set<NextTickListEntry> servux_getScheduledTicks();
}
