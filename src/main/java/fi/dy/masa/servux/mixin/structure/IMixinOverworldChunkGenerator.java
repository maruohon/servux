package fi.dy.masa.servux.mixin.structure;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.world.gen.chunk.OverworldChunkGenerator;
import net.minecraft.world.gen.feature.AbstractTempleFeature;
import net.minecraft.world.gen.feature.OceanMonumentFeature;
import net.minecraft.world.gen.feature.StrongholdFeature;
import net.minecraft.world.gen.feature.VillageFeature;
import net.minecraft.world.gen.feature.WoodlandMansionFeature;

@Mixin(OverworldChunkGenerator.class)
public interface IMixinOverworldChunkGenerator
{
    @Accessor("oceanMonumentFeature")
    OceanMonumentFeature servux_getOceanMonumentFeature();

    @Accessor("strongholdFeature")
    StrongholdFeature servux_getStrongholdFeature();

    @Accessor("templeFeature")
    AbstractTempleFeature servux_getTempleFeature();

    @Accessor("villageFeature")
    VillageFeature servux_getVillageFeature();

    @Accessor("woodlandMansionFeature")
    WoodlandMansionFeature servux_getWoodlandMansionFeature();
}
