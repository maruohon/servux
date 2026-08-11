package fi.dy.masa.servux.util.data;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;

public class PlayerDimensionPosition
{
    protected DimensionType dimensionType;
    protected BlockPos pos;

    public PlayerDimensionPosition(EntityPlayer player)
    {
        this.setPosition(player);
    }

    public boolean dimensionChanged(EntityPlayer player)
    {
        return this.dimensionType != player.world.provider.getDimensionType();
    }

    public boolean needsUpdate(EntityPlayer player, int distanceThreshold)
    {
        if (this.dimensionChanged(player))
        {
            return true;
        }

        return Math.abs(player.posX - this.pos.getX()) > distanceThreshold ||
               Math.abs(player.posY - this.pos.getY()) > distanceThreshold ||
               Math.abs(player.posZ - this.pos.getZ()) > distanceThreshold;
    }

    public void setPosition(EntityPlayer player)
    {
        this.dimensionType = player.world.provider.getDimensionType();
        this.pos = new BlockPos(player);
    }
}
