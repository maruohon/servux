package fi.dy.masa.servux.mixin.structure;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.structure.StructureStart;
import net.minecraft.world.gen.feature.StructureFeature;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

@Mixin(StructureFeature.class)
public interface IMixinStructureFeature
{
    @Accessor("field_26039")
    Long2ObjectMap<StructureStart> servux_getStructureMap();
}
