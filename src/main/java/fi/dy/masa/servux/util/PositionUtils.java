package fi.dy.masa.servux.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;

public class PositionUtils
{
    public static ChunkPos chunkPosFrom(Entity entity)
    {
        return new ChunkPos((int) Math.floor(entity.x / 16.0),
                            (int) Math.floor(entity.z / 16.0));
    }
}
