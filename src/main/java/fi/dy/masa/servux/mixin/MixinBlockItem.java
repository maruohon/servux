package fi.dy.masa.servux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import fi.dy.masa.servux.util.PlacementHandler;
import fi.dy.masa.servux.util.PlacementHandler.UseContext;

@Mixin(value = BlockItem.class, priority = 999)
public abstract class MixinBlockItem extends Item
{
    private MixinBlockItem(Item.Properties builder)
    {
        super(builder);
    }

    @Shadow protected abstract boolean canPlace(BlockItemUseContext context, BlockState state);
    @Shadow public abstract Block getBlock();

    @Inject(method = "getStateForPlacement", at = @At("HEAD"), cancellable = true)
    private void modifyPlacementState(BlockItemUseContext ctx, CallbackInfoReturnable<BlockState> cir)
    {
        BlockState stateOrig = this.getBlock().getStateForPlacement(ctx);

        if (stateOrig != null && this.canPlace(ctx, stateOrig))
        {
            UseContext context = UseContext.from(ctx, ctx.getHand());
            cir.setReturnValue(PlacementHandler.applyPlacementProtocolToPlacementState(stateOrig, context));
        }
    }
}
