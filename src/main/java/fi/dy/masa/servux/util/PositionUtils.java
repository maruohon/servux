package fi.dy.masa.servux.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class PositionUtils
{
    public static ChunkPos chunkPosFrom(Entity entity)
    {
        return new ChunkPos((int) Math.floor(entity.x / 16.0),
                            (int) Math.floor(entity.z / 16.0));
    }

    public static boolean isBoxWithinRange(BlockBox box, BlockPos pos, int maxRange)
    {
        return pos.getX() >= (box.minX - maxRange) &&
               pos.getX() <= (box.maxX + maxRange) &&
               pos.getZ() >= (box.minZ - maxRange) &&
               pos.getZ() <= (box.maxZ + maxRange);
    }
}
