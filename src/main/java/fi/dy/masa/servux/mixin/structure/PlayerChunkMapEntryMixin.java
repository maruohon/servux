package fi.dy.masa.servux.mixin.structure;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.servux.dataprovider.StructureDataProvider;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.world.chunk.Chunk;

@Mixin(PlayerChunkMapEntry.class)
public abstract class PlayerChunkMapEntryMixin
{
    @Shadow private Chunk chunk;

    @Inject(method = "addPlayer",
            at = @At(value = "INVOKE", remap = false,
                     target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private void servux_onAdd(EntityPlayerMP player, CallbackInfo ci)
    {
        if (StructureDataProvider.INSTANCE.isEnabled())
        {
            StructureDataProvider.INSTANCE.onStartedWatchingChunk(player, this.chunk);
        }
    }
}
