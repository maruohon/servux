package fi.dy.masa.servux.util.data;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;

public class MobCaps
{
    public static final SpawnGroup[] ENTITY_CATEGORIES = SpawnGroup.values();

    protected final LimitedValue[] caps = new LimitedValue[ENTITY_CATEGORIES.length];

    public MobCaps()
    {
        for (int i = 0; i < this.caps.length; ++i)
        {
            this.caps[i] = new LimitedValue();
        }
    }

    public LimitedValue getMobCap(SpawnGroup group)
    {
        return this.caps[group.ordinal()];
    }

    public void update(ServerWorld world)
    {
        int spawnableChunks = getSpawnableChunksCount(world);
        int divisor = 17 * 17;

        for (SpawnGroup group : ENTITY_CATEGORIES)
        {
            LimitedValue val = this.getMobCap(group);
            int value = world.method_26030(group.getType());
            int limit = group.getCapacity() * spawnableChunks / divisor;
            val.setBoth(value, limit);
        }
    }

    public static int getSpawnableChunksCount(ServerWorld world)
    {
        Set<ChunkPos> eligibleChunksForSpawning = new HashSet<>();
        int chunkCount = 0;

        for (PlayerEntity player : world.playerList)
        {
            if (player.isSpectator() == false)
            {
                int cx = MathHelper.floor(player.x / 16.0D);
                int cz = MathHelper.floor(player.z / 16.0D);
                int chunkRadius = 8;

                for (int cxOff = -chunkRadius; cxOff <= chunkRadius; ++cxOff)
                {
                    for (int czOff = -chunkRadius; czOff <= chunkRadius; ++czOff)
                    {
                        boolean edgeChunk = cxOff == -chunkRadius || cxOff == chunkRadius || czOff == -chunkRadius || czOff == chunkRadius;
                        ChunkPos chunkPos = new ChunkPos(cxOff + cx, czOff + cz);

                        if (eligibleChunksForSpawning.contains(chunkPos) == false)
                        {
                            ++chunkCount;

                            if (edgeChunk == false && world.getWorldBorder().method_27301(chunkPos))
                            {
                                PlayerChunkWatchingEntry entry = world.getRaidManager().getEntry(chunkPos.x, chunkPos.z);

                                if (entry != null && entry.isSynced())
                                {
                                    eligibleChunksForSpawning.add(chunkPos);
                                }
                            }
                        }
                    }
                }
            }
        }

        return chunkCount;
    }
}
