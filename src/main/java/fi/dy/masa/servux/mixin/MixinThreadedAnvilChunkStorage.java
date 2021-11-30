package fi.dy.masa.servux.mixin;

import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class MixinThreadedAnvilChunkStorage
{
    @Inject(method = "sendChunkDataPackets", at = @At("HEAD"))
    private void onSendChunkPacket(ServerPlayerEntity player, MutableObject<ChunkDataS2CPacket> cachedDataPacket,
                                   WorldChunk chunk, CallbackInfo ci)
    {
        if (StructureDataProvider.INSTANCE.isEnabled())
        {
            StructureDataProvider.INSTANCE.onStartedWatchingChunk(player, chunk);
        }
    }
}
