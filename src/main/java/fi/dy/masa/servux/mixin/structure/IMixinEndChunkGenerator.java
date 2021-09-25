package fi.dy.masa.servux.mixin.structure;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.world.gen.chunk.EndChunkGenerator;
import net.minecraft.world.gen.feature.EndCityFeature;

@Mixin(EndChunkGenerator.class)
public interface IMixinEndChunkGenerator
{
    @Accessor("endCityFeature")
    EndCityFeature servux_getEndCityFeature();
}
