package fi.dy.masa.servux.dataprovider;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import fi.dy.masa.servux.network.ClientPacketHandler;
import fi.dy.masa.servux.network.PacketSplitter;
import fi.dy.masa.servux.network.packet.StructureDataPacketHandler;
import fi.dy.masa.servux.util.data.PlayerDimensionPosition;
import fi.dy.masa.servux.util.PositionUtils;
import fi.dy.masa.servux.util.data.Timeout;
import it.unimi.dsi.fastutil.longs.LongSet;

public class StructureDataProvider extends BaseDataProvider
{
    public static final StructureDataProvider INSTANCE = new StructureDataProvider();

    protected final Map<UUID, PlayerDimensionPosition> registeredPlayers = new HashMap<>();
    protected final Map<UUID, Map<ChunkPos, Timeout>> timeouts = new HashMap<>();
    protected final CompoundTag metadata = new CompoundTag();
    protected int timeout = 30 * 20;
    protected int retainDistance;

    protected StructureDataProvider()
    {
        super("structure_bounding_boxes",
              StructureDataPacketHandler.PROTOCOL_VERSION,
              "Structure Bounding Boxes data for structures such as Witch Huts, Ocean Monuments, Nether Fortresses etc.");

        this.metadata.putString("id", StructureDataPacketHandler.CHANNEL.toString());
        this.metadata.putInt("timeout", this.timeout);
        this.metadata.putInt("version", StructureDataPacketHandler.PROTOCOL_VERSION);

        //this.shouldTick = true;
    }

    @Override
    public ClientPacketHandler getPacketHandler()
    {
        return StructureDataPacketHandler.INSTANCE;
    }

    @Override
    public void tick(MinecraftServer server, int tickCounter)
    {
        if (this.registeredPlayers.isEmpty() == false)
        {
            // System.out.printf("=======================\n");
            // System.out.printf("tick: %d - %s\n", tickCounter, this.isEnabled());
            this.retainDistance = server.getPlayerManager().getViewDistance() + 2;
            Iterator<UUID> uuidIter = this.registeredPlayers.keySet().iterator();

            while (uuidIter.hasNext())
            {
                UUID uuid = uuidIter.next();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);

                if (player != null)
                {
                    this.checkForDimensionChange(player);
                    this.refreshTrackedChunks(player, tickCounter);
                }
                else
                {
                    this.timeouts.remove(uuid);
                    uuidIter.remove();
                }
            }
        }
    }

    public void onStartedWatchingChunk(ServerPlayerEntity player, Chunk chunk)
    {
        UUID uuid = player.getUuid();

        if (this.registeredPlayers.containsKey(uuid))
        {
            this.addChunkTimeoutIfHasReferences(uuid, chunk, player.server.getTicks());
        }
    }

    public boolean register(ServerPlayerEntity player)
    {
        // System.out.printf("register\n");
        boolean registered = false;
        UUID uuid = player.getUuid();

        if (this.registeredPlayers.containsKey(uuid) == false)
        {
            PacketSplitter.sendPacketTypeAndCompound(StructureDataPacketHandler.CHANNEL, StructureDataPacketHandler.PACKET_S2C_METADATA, this.metadata, player);

            this.registeredPlayers.put(uuid, new PlayerDimensionPosition(player));
            int tickCounter = player.server.getTicks();
            this.initialSyncStructuresToPlayerWithinRange(player, player.server.getPlayerManager().getViewDistance(), tickCounter);

            registered = true;
        }

        return registered;
    }

    public boolean unregister(ServerPlayerEntity player)
    {
        // System.out.printf("unregister\n");
        return this.registeredPlayers.remove(player.getUuid()) != null;
    }

    protected void initialSyncStructuresToPlayerWithinRange(ServerPlayerEntity player, int chunkRadius, int tickCounter)
    {
        UUID uuid = player.getUuid();
        ChunkPos center = PositionUtils.chunkPosFrom(player);
        Map<String, LongSet> references = this.getStructureReferencesWithinRange(player.getServerWorld(), center, chunkRadius);

        this.timeouts.remove(uuid);
        this.registeredPlayers.computeIfAbsent(uuid, (u) -> new PlayerDimensionPosition(player)).setPosition(player);

        // System.out.printf("initialSyncStructuresToPlayerWithinRange: references: %d\n", references.size());
        this.sendStructures(player, references, tickCounter);
    }

    protected void addChunkTimeoutIfHasReferences(final UUID uuid, Chunk chunk, final int tickCounter)
    {
        final ChunkPos pos = chunk.method_27417();

        /*
        if (this.chunkHasStructureReferences(pos.x, pos.z, chunk.getWorld()))
        {
            final Map<ChunkPos, Timeout> map = this.timeouts.computeIfAbsent(uuid, (u) -> new HashMap<>());
            final int timeout = this.timeout;

            //System.out.printf("addChunkTimeoutIfHasReferences: %s\n", pos);
            // Set the timeout so it's already expired and will cause the chunk to be sent on the next update tick
            map.computeIfAbsent(pos, (p) -> new Timeout(tickCounter - timeout));
        }
        */
    }

    protected void checkForDimensionChange(ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        PlayerDimensionPosition playerPos = this.registeredPlayers.get(uuid);

        if (playerPos == null || playerPos.dimensionChanged(player))
        {
            this.timeouts.remove(uuid);
            this.registeredPlayers.computeIfAbsent(uuid, (u) -> new PlayerDimensionPosition(player)).setPosition(player);
        }
    }

    protected void addOrRefreshTimeouts(final UUID uuid, final Map<String, LongSet> references, final int tickCounter)
    {
        // System.out.printf("addOrRefreshTimeouts: references: %d\n", references.size());
        Map<ChunkPos, Timeout> map = this.timeouts.computeIfAbsent(uuid, (u) -> new HashMap<>());

        for (LongSet chunks : references.values())
        {
            for (long chunkPosLong : chunks)
            {
                final ChunkPos pos = new ChunkPos((int) chunkPosLong, (int) (chunkPosLong >>> 32));
                map.computeIfAbsent(pos, (p) -> new Timeout(tickCounter)).setLastSync(tickCounter);
            }
        }
    }

    protected void refreshTrackedChunks(ServerPlayerEntity player, int tickCounter)
    {
        UUID uuid = player.getUuid();
        Map<ChunkPos, Timeout> map = this.timeouts.get(uuid);

        if (map != null)
        {
            // System.out.printf("refreshTrackedChunks: timeouts: %d\n", map.size());
            this.sendAndRefreshExpiredStructures(player, map, tickCounter);
        }
    }

    protected boolean isOutOfRange(ChunkPos pos, ChunkPos center)
    {
        int chunkRadius = this.retainDistance;

        return Math.abs(pos.x - center.x) > chunkRadius ||
               Math.abs(pos.z - center.z) > chunkRadius;
    }

    protected void sendAndRefreshExpiredStructures(ServerPlayerEntity player, Map<ChunkPos, Timeout> map, int tickCounter)
    {
        /*
        Set<ChunkPos> positionsToUpdate = new HashSet<>();

        for (Map.Entry<ChunkPos, Timeout> entry : map.entrySet())
        {
            Timeout timeout = entry.getValue();

            if (timeout.needsUpdate(tickCounter, this.timeout))
            {
                positionsToUpdate.add(entry.getKey());
            }
        }

        if (positionsToUpdate.isEmpty() == false)
        {
            ServerWorld world = player.getServerWorld();
            ChunkPos center = PositionUtils.chunkPosFrom(player);
            Map<String, LongSet> references = new HashMap<>();

            for (ChunkPos pos : positionsToUpdate)
            {
                if (this.isOutOfRange(pos, center))
                {
                    map.remove(pos);
                }
                else
                {
                    this.getStructureReferencesFromChunk(pos.x, pos.z, world, references);

                    Timeout timeout = map.get(pos);

                    if (timeout != null)
                    {
                        timeout.setLastSync(tickCounter);
                    }
                }
            }

            // System.out.printf("sendAndRefreshExpiredStructures: positionsToUpdate: %d -> references: %d, to: %d\n", positionsToUpdate.size(), references.size(), this.timeout);

            if (references.isEmpty() == false)
            {
                this.sendStructures(player, references, tickCounter);
            }
        }
        */
    }

    protected Map<String, LongSet> getStructureReferencesWithinRange(ServerWorld world, ChunkPos center, int chunkRadius)
    {
        Map<String, LongSet> references = new HashMap<>();

        for (int cx = center.x - chunkRadius; cx <= center.x + chunkRadius; ++cx)
        {
            for (int cz = center.z - chunkRadius; cz <= center.z + chunkRadius; ++cz)
            {
                //this.getStructureReferencesFromChunk(cx, cz, world, references);
            }
        }

        // System.out.printf("getStructureReferencesWithinRange: references: %d\n", references.size());
        return references;
    }

    protected void sendStructures(ServerPlayerEntity player, Map<String, LongSet> references, int tickCounter)
    {
        /*
        Map<ChunkPos, StructureStart> starts = this.getStructureStartsFromReferences(player.getServerWorld(), references);

        if (starts.isEmpty() == false)
        {
            this.addOrRefreshTimeouts(player.getUuid(), references, tickCounter);

            ListTag structureList = this.getStructureList(starts);
            // System.out.printf("sendStructures: starts: %d -> structureList: %d. refs: %s\n", starts.size(), structureList.size(), references.keySet());

            CompoundTag tag = new CompoundTag();
            tag.put("Structures", structureList);

            PacketSplitter.sendPacketTypeAndCompound(StructureDataPacketHandler.CHANNEL, StructureDataPacketHandler.PACKET_S2C_STRUCTURE_DATA, tag, player);
        }
        */
    }


    protected ListTag getStructureList(Map<ChunkPos, StructureStart> structures)
    {
        ListTag list = new ListTag();

        for (Map.Entry<ChunkPos, StructureStart> entry : structures.entrySet())
        {
            ChunkPos pos = entry.getKey();
            list.add(entry.getValue().method_27890(pos.x,  pos.z)); // toTag
        }

        return list;
    }
}
