package fi.dy.masa.servux.mixin.structure;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureOceanMonument;
import net.minecraft.world.gen.structure.WoodlandMansion;

@Mixin(ChunkGeneratorOverworld.class)
public interface ChunkGeneratorOverworldMixin
{
    @Accessor("oceanMonumentGenerator")
    StructureOceanMonument servux_getOceanMonumentFeature();

    @Accessor("strongholdGenerator")
    MapGenStronghold servux_getStrongholdFeature();

    @Accessor("scatteredFeatureGenerator")
    MapGenScatteredFeature servux_getTempleFeature();

    @Accessor("villageGenerator")
    MapGenVillage servux_getVillageFeature();

    @Accessor("woodlandMansionGenerator")
    WoodlandMansion servux_getWoodlandMansionFeature();
}
