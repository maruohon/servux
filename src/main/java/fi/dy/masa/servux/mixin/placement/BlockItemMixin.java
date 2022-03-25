package fi.dy.masa.servux.mixin.placement;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.PlaceableItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import fi.dy.masa.servux.config.Configs;
import fi.dy.masa.servux.util.PlacementHandler;

@Mixin({BlockItem.class, PlaceableItem.class })
public abstract class BlockItemMixin
{
    @Redirect(method = "useOnBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/Block;getPlacementState(" +
                             "Lnet/minecraft/world/World;" +
                             "Lnet/minecraft/util/math/BlockPos;" +
                             "Lnet/minecraft/util/math/Direction;" +
                             "FFFI" +
                             "Lnet/minecraft/entity/LivingEntity;)" +
                             "Lnet/minecraft/block/BlockState;"))
    private BlockState servux_modifyPlacementState(Block block, World world, BlockPos pos, Direction facing,
                                                   float hitX, float hitY, float hitZ, int meta, LivingEntity placer,
                                                   PlayerEntity playerIn,
                                                   World worldIn, BlockPos posIn, Hand handIn, Direction facingIn,
                                                   float hitXIn, float hitYIn, float hitZIn)
    {
        BlockState stateOriginal = block.getPlacementState(world, pos, facing, hitX, hitY, hitZ, meta, placer);

        if (Configs.Generic.PLACEMENT_PROTOCOL_ENABLED.getBooleanValue())
        {
            PlacementHandler.UseContext context = PlacementHandler.UseContext.of(world, pos, facing, new Vec3d(hitX, hitY, hitZ), placer, handIn);
            return PlacementHandler.applyPlacementProtocolToPlacementState(stateOriginal, context);
        }

        return stateOriginal;
    }
}
