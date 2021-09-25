package fi.dy.masa.servux.mixin.structure;

import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.world.gen.chunk.FlatChunkGenerator;
import net.minecraft.world.gen.feature.StructureFeature;

@Mixin(FlatChunkGenerator.class)
public interface IMixinFlatChunkGenerator
{
    @Accessor("features")
    Map<String, StructureFeature> servux_getStructureFeatureMap();
}
