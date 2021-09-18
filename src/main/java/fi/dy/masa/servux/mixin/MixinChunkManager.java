package fi.dy.masa.servux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ChunkManager;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;

@Mixin(ChunkManager.class)
public abstract class MixinChunkManager
{
    @Inject(method = "sendChunkData", at = @At("HEAD"))
    private void onSendChunkPacket(ServerPlayerEntity player, IPacket<?>[] packets, Chunk chunk, CallbackInfo ci)
    {
        if (StructureDataProvider.INSTANCE.isEnabled())
        {
            StructureDataProvider.INSTANCE.onStartedWatchingChunk(player, chunk);
        }
    }
}
