package fi.dy.masa.servux.mixin.placement;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.servux.config.Configs;

@Mixin(DispenserBlock.class)
public abstract class DispenserBlockMixin
{
    @Inject(method = "method_26570", at = @At("HEAD"), cancellable = true)
    private void servux_fixRotation(CallbackInfo ci)
    {
        if (Configs.Generic.PLACEMENT_PROTOCOL_ENABLED.getBooleanValue())
        {
            ci.cancel();
        }
    }

    @Redirect(method = "onPlaced",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private boolean servux_fixRotation(World world, BlockPos pos, BlockState state, int flags,
                                       World worldIn, BlockPos posIn, BlockState stateIn, LivingEntity placerIn, ItemStack stackIn)
    {
        if (Configs.Generic.PLACEMENT_PROTOCOL_ENABLED.getBooleanValue())
        {
            // Use the original state from getStateForPlacement() instead of
            // overriding the facing with one calculated from the placer
            return world.setBlockState(pos, stateIn, flags);
        }

        return world.setBlockState(pos, state, flags);
    }
}
