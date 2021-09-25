package fi.dy.masa.servux.mixin.structure;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.world.gen.chunk.NetherChunkGenerator;
import net.minecraft.world.gen.feature.NetherFortressFeature;

@Mixin(NetherChunkGenerator.class)
public interface IMixinNetherChunkGenerator
{
    @Accessor("netherFortressFeature")
    NetherFortressFeature servux_getFortressFeature();
}
