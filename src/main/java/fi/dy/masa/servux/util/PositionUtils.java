package fi.dy.masa.servux.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public class PositionUtils
{
    public static ChunkPos chunkPosFrom(Entity entity)
    {
        return new ChunkPos((int) Math.floor(entity.posX / 16.0),
                            (int) Math.floor(entity.posZ / 16.0));
    }

    public static boolean isBoxWithinRange(StructureBoundingBox box, BlockPos pos, int maxRange)
    {
        return pos.getX() >= (box.minX - maxRange) &&
               pos.getX() <= (box.maxX + maxRange) &&
               pos.getZ() >= (box.minZ - maxRange) &&
               pos.getZ() <= (box.maxZ + maxRange);
    }
}
