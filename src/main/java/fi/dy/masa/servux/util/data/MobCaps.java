package fi.dy.masa.servux.util.data;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.border.WorldBorder;

public class MobCaps
{
    public static final EnumCreatureType[] ENTITY_CATEGORIES = EnumCreatureType.values();

    protected final LimitedValue[] caps = new LimitedValue[ENTITY_CATEGORIES.length];

    public MobCaps()
    {
        for (int i = 0; i < this.caps.length; ++i)
        {
            this.caps[i] = new LimitedValue();
        }
    }

    public LimitedValue getMobCap(EnumCreatureType group)
    {
        return this.caps[group.ordinal()];
    }

    public void update(WorldServer world)
    {
        int spawnableChunks = getSpawnableChunksCount(world);
        int divisor = 17 * 17;

        for (EnumCreatureType group : ENTITY_CATEGORIES)
        {
            int value = world.countEntities(group.getCreatureClass());
            int limit = group.getMaxNumberOfCreature() * spawnableChunks / divisor;
            this.getMobCap(group).setBoth(value, limit);
        }
    }

    public static int getSpawnableChunksCount(WorldServer world)
    {
        LongOpenHashSet eligibleChunksForSpawning = new LongOpenHashSet();
        int chunkCount = 0;

        for (EntityPlayer player : world.playerEntities)
        {
            if (player.isSpectator())
            {
                continue;
            }

            int centerX = MathHelper.floor(player.posX / 16.0D);
            int centerZ = MathHelper.floor(player.posZ / 16.0D);
            int chunkRadius = 8;

            for (int cxOff = -chunkRadius; cxOff <= chunkRadius; ++cxOff)
            {
                for (int czOff = -chunkRadius; czOff <= chunkRadius; ++czOff)
                {
                    boolean edgeChunk = cxOff == -chunkRadius || cxOff == chunkRadius || czOff == -chunkRadius || czOff == chunkRadius;
                    int cx = centerX + cxOff;
                    int cz = centerZ + czOff;
                    long cpLong = ChunkPos.asLong(cx, cz);

                    if (eligibleChunksForSpawning.contains(cpLong))
                    {
                        continue;
                    }

                    ++chunkCount;

                    if (edgeChunk == false && worldBorderIntersectsChunk(cx, cz, world.getWorldBorder()))
                    {
                        PlayerChunkMapEntry entry = world.getPlayerChunkMap().getEntry(cx, cz);

                        if (entry != null && entry.isSentToPlayers())
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

        return startX + 15 > border.minX() &&
               startX      < border.maxX() &&
               startZ + 15 > border.minZ() &&
               startZ      < border.maxZ();
    }
}
