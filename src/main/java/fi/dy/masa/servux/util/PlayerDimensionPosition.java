package fi.dy.masa.servux.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;

public class PlayerDimensionPosition
{
    protected DimensionType dimensionType;
    protected BlockPos pos;

    public PlayerDimensionPosition(PlayerEntity player)
    {
        this.setPosition(player);
    }

    public boolean dimensionChanged(PlayerEntity player)
    {
        return this.dimensionType != player.world.dimension.getType();
    }

    public boolean needsUpdate(PlayerEntity player, int distanceThreshold)
    {
        if (this.dimensionChanged(player))
        {
            return true;
        }

        return Math.abs(player.x - this.pos.getX()) > distanceThreshold ||
               Math.abs(player.y - this.pos.getY()) > distanceThreshold ||
               Math.abs(player.z - this.pos.getZ()) > distanceThreshold;
    }

    public void setPosition(PlayerEntity player)
    {
        this.dimensionType = player.world.dimension.getType();
        this.pos = new BlockPos(player);
    }
}
