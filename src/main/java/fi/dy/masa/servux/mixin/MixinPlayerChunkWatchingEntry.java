package fi.dy.masa.servux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingEntry;
import net.minecraft.world.chunk.Chunk;
import fi.dy.masa.servux.dataprovider.StructureDataProvider;

@Mixin(PlayerChunkWatchingEntry.class)
public abstract class MixinPlayerChunkWatchingEntry
{
    @Shadow private Chunk chunk;

    @Inject(method = "add",
            at = @At(value = "INVOKE", remap = false,
                     target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private void servux_onSendChunkPacket(ServerPlayerEntity player, CallbackInfo ci)
    {
        if (StructureDataProvider.INSTANCE.isEnabled())
        {
            StructureDataProvider.INSTANCE.onStartedWatchingChunk(player, this.chunk);
        }
    }
}
