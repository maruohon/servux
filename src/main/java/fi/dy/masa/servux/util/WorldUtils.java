package fi.dy.masa.servux.util;

import net.minecraft.world.World;

public class WorldUtils
{
    public static int getDimensionId(World world)
    {
        return world.dimension.getType().getRawId();
    }
}
