package fi.dy.masa.servux.util;

import net.minecraft.entity.Entity;

public class EntityUtils
{
    public static int getDimension(Entity entity)
    {
        return WorldUtils.getDimensionId(entity.getEntityWorld());
    }
}
