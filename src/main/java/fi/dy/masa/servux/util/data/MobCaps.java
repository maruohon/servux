package fi.dy.masa.servux.util.data;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.border.WorldBorder;

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
            int value = world.method_26030(group.getType());
            int limit = group.getCapacity() * spawnableChunks / divisor;
            this.getMobCap(group).setBoth(value, limit);
        }
    }

    public static int getSpawnableChunksCount(ServerWorld world)
    {
        LongOpenHashSet eligibleChunksForSpawning = new LongOpenHashSet();
        int chunkCount = 0;

        for (PlayerEntity player : world.playerList)
        {
            if (player.isSpectator())
            {
                continue;
            }

            int centerX = MathHelper.floor(player.x / 16.0D);
            int centerZ = MathHelper.floor(player.z / 16.0D);
            int chunkRadius = 8;

            for (int cxOff = -chunkRadius; cxOff <= chunkRadius; ++cxOff)
            {
                for (int czOff = -chunkRadius; czOff <= chunkRadius; ++czOff)
                {
                    boolean edgeChunk = cxOff == -chunkRadius || cxOff == chunkRadius || czOff == -chunkRadius || czOff == chunkRadius;
                    int cx = centerX + cxOff;
                    int cz = centerZ + czOff;
                    long cpLong = ChunkPos.toLong(cx, cz);

                    if (eligibleChunksForSpawning.contains(cpLong))
                    {
                        continue;
                    }

                    ++chunkCount;

                    if (edgeChunk == false && worldBorderIntersectsChunk(cx, cz, world.getWorldBorder()))
                    {
                        PlayerChunkWatchingEntry entry = world.getRaidManager().getEntry(cx, cz);

                        if (entry != null && entry.isSynced())
                        {
                            eligibleChunksForSpawning.add(cpLong);
                        }
                    }
                }
            }
        }

        return chunkCount;
    }

    protected static boolean worldBorderIntersectsChunk(int cx, int cz, WorldBorder border)
    {
        int startX = (cx << 4);
        int startZ = (cz << 4);

        return startX + 15 > border.method_27306() &&
               startX      < border.method_27314() &&
               startZ + 15 > border.method_27310() &&
               startZ      < border.method_27315();
    }
}
