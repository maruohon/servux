package fi.dy.masa.abpt.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.abpt.util.PlacementHandler;
import fi.dy.masa.abpt.util.PlacementHandler.UseContext;

@Mixin(value = BlockItem.class, priority = 999)
public abstract class MixinBlockItem extends Item
{
    private MixinBlockItem(Item.Properties builder)
    {
        super(builder);
    }

    @Shadow protected abstract boolean canPlace(BlockPlaceContext context, BlockState state);
    @Shadow public abstract Block getBlock();

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void abpt_modifyPlacementState(BlockPlaceContext ctx, CallbackInfoReturnable<BlockState> cir)
    {
        BlockState stateOrig = this.getBlock().getStateForPlacement(ctx);

        if (stateOrig != null && this.canPlace(ctx, stateOrig))
        {
            //System.out.printf("abpt_modifyPlacementState @ %s\n", ctx.getClickedPos());
            UseContext context = UseContext.from(ctx, ctx.getHand());
            cir.setReturnValue(PlacementHandler.applyPlacementProtocolToPlacementState(stateOrig, context));
        }
    }
}
