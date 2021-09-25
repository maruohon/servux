package fi.dy.masa.servux.util.data;

import net.minecraft.server.world.ServerWorld;
import fi.dy.masa.servux.mixin.infosub.IMixinServerWorld;

public class PerDimensionObjectCounts
{
    protected int droppedChunksHashSize;
    protected int loadedChunks;
    protected int loadedEntities;
    protected int loadedBlockEntities;
    protected int loadedBlockEntitiesTicking;
    protected int scheduledTicks;

    public int getDroppedChunksHashSize()
    {
        return this.droppedChunksHashSize;
    }

    public int getLoadedChunks()
    {
        return this.loadedChunks;
    }

    public int getLoadedEntities()
    {
        return this.loadedEntities;
    }

    public int getLoadedBlockEntities()
    {
        return this.loadedBlockEntities;
    }

    public int getLoadedBlockEntitiesTicking()
    {
        return this.loadedBlockEntitiesTicking;
    }

    public int getScheduledTicks()
    {
        return this.scheduledTicks;
    }

    public void update(ServerWorld world)
    {
        this.loadedChunks = world.getChunkManager().size();
        this.loadedEntities = world.field_23572.size();
        this.loadedBlockEntities = world.blockEntities.size();
        this.loadedBlockEntitiesTicking = world.tickingBlockEntities.size();
        this.scheduledTicks = ((IMixinServerWorld) world).servux_getScheduledTicks().size();
    }
}
