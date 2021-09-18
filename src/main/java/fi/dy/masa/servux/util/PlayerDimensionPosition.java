package fi.dy.masa.servux.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;

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
        return this.dimensionType != player.getEntityWorld().getDimensionType();
    }

    public boolean needsUpdate(PlayerEntity player, int distanceThreshold)
    {
        if (player.getEntityWorld().getDimensionType() != this.dimensionType)
        {
            return true;
        }

        BlockPos pos = player.getPosition();

        return Math.abs(pos.getX() - this.pos.getX()) > distanceThreshold ||
               Math.abs(pos.getY() - this.pos.getY()) > distanceThreshold ||
               Math.abs(pos.getZ() - this.pos.getZ()) > distanceThreshold;
    }

    public void setPosition(PlayerEntity player)
    {
        this.dimensionType = player.getEntityWorld().getDimensionType();
        this.pos = player.getPosition();
    }
}
