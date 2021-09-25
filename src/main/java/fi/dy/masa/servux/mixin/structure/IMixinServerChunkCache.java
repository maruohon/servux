package fi.dy.masa.servux.mixin.structure;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.server.world.ServerChunkCache;
import net.minecraft.world.chunk.ChunkGenerator;

@Mixin(ServerChunkCache.class)
public interface IMixinServerChunkCache
{
    @Accessor("generator")
    ChunkGenerator servux_getChunkGenerator();
}
