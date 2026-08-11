package fi.dy.masa.servux.util.data;

import fi.dy.masa.servux.mixin.infosub.ServerWorldMixin;

import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;

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

    public void update(WorldServer world)
    {
        this.loadedChunks = ((ChunkProviderServer) world.getChunkProvider()).getLoadedChunkCount();
        this.loadedEntities = world.loadedEntityList.size();
        this.loadedBlockEntities = world.loadedTileEntityList.size();
        this.loadedBlockEntitiesTicking = world.tickableTileEntities.size();
        this.scheduledTicks = ((ServerWorldMixin) world).servux_getScheduledTicks().size();
    }
}
