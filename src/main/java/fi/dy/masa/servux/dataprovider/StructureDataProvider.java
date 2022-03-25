package fi.dy.masa.servux.dataprovider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.Structures;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.EndChunkGenerator;
import net.minecraft.world.gen.chunk.FlatChunkGenerator;
import net.minecraft.world.gen.chunk.NetherChunkGenerator;
import net.minecraft.world.gen.chunk.OverworldChunkGenerator;
import net.minecraft.world.gen.feature.AbstractTempleFeature;
import net.minecraft.world.gen.feature.OceanMonumentFeature;
import net.minecraft.world.gen.feature.StrongholdFeature;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.VillageFeature;
import fi.dy.masa.servux.mixin.structure.EndChunkGeneratorMixin;
import fi.dy.masa.servux.mixin.structure.FlatChunkGeneratorMixin;
import fi.dy.masa.servux.mixin.structure.NetherChunkGeneratorMixin;
import fi.dy.masa.servux.mixin.structure.OverworldChunkGeneratorMixin;
import fi.dy.masa.servux.mixin.structure.ServerChunkCacheMixin;
import fi.dy.masa.servux.mixin.structure.StructureFeatureMixin;
import fi.dy.masa.servux.network.ClientPacketHandler;
import fi.dy.masa.servux.network.PacketSplitter;
import fi.dy.masa.servux.network.packet.StructureDataPacketHandler;
import fi.dy.masa.servux.util.PositionUtils;
import fi.dy.masa.servux.util.data.PlayerDimensionPosition;

public class StructureDataProvider extends BaseDataProvider
{
    public static final StructureDataProvider INSTANCE = new StructureDataProvider();

    protected final Map<UUID, PlayerDimensionPosition> registeredPlayers = new HashMap<>();
    //protected final ArrayListMultimap<UUID, StructureStart> syncedStructures = ArrayListMultimap.create();
    protected final Set<UUID> movedPlayers = new HashSet<>();
    protected final CompoundTag metadata = new CompoundTag();
    protected int retainDistanceChunks;

    protected StructureDataProvider()
    {
        super("structure_bounding_boxes",
              StructureDataPacketHandler.PROTOCOL_VERSION,
              "Structure Bounding Boxes data for structures such as Witch Huts, Ocean Monuments, Nether Fortresses etc.");

        this.metadata.putString("id", StructureDataPacketHandler.CHANNEL.toString());
        this.metadata.putBoolean("no_timeout", true);
        this.metadata.putInt("version", StructureDataPacketHandler.PROTOCOL_VERSION);

        this.shouldTick = true;
        this.setTickRate(100);
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
            this.retainDistanceChunks = server.getPlayerManager().getViewDistance() + 2;
            Iterator<UUID> uuidIter = this.registeredPlayers.keySet().iterator();

            while (uuidIter.hasNext())
            {
                UUID uuid = uuidIter.next();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);

                if (player != null)
                {
                    if (this.checkForDimensionChange(player) == false &&
                        this.movedPlayers.contains(uuid))
                    {
                        this.syncStructuresToPlayerWithinRange(player, player.server.getPlayerManager().getViewDistance());
                    }
                }
                else
                {
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
            this.movedPlayers.add(uuid);
        }
    }

    public boolean register(ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        boolean registered = false;

        if (this.registeredPlayers.containsKey(uuid) == false)
        {
            PacketSplitter.sendPacketTypeAndCompound(StructureDataPacketHandler.CHANNEL, StructureDataPacketHandler.PACKET_S2C_METADATA, this.metadata, player);

            this.registeredPlayers.put(uuid, new PlayerDimensionPosition(player));
            this.syncStructuresToPlayerWithinRange(player, player.server.getPlayerManager().getViewDistance());

            registered = true;
            //System.out.printf("register\n");
        }

        return registered;
    }

    public boolean unregister(ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        // System.out.printf("unregister\n");

        if (this.registeredPlayers.remove(uuid) != null)
        {
            this.removeCachedDataForPlayer(uuid);
            return true;
        }

        return false;
    }

    protected void removeCachedDataForPlayer(UUID uuid)
    {
        this.movedPlayers.remove(uuid);
        //this.syncedStructures.removeAll(uuid);
    }

    protected void syncStructuresToPlayerWithinRange(ServerPlayerEntity player, int chunkRadius)
    {
        UUID uuid = player.getUuid();

        this.registeredPlayers.computeIfAbsent(uuid, (u) -> new PlayerDimensionPosition(player)).setPosition(player);

        List<StructureStart> list = new ArrayList<>();
        ChunkGenerator generator = ((ServerChunkCacheMixin) ((ServerWorld) player.getEntityWorld()).getChunkManager()).servux_getChunkGenerator();

        this.getStructuresFromGenerator(list, generator, new BlockPos(player), chunkRadius * 16);
        this.sendStructuresToPlayer(list, player);
        this.movedPlayers.remove(uuid);

        //System.out.printf("syncStructuresToPlayerWithinRange() - size: %d\n", list.size());
    }

    protected boolean checkForDimensionChange(ServerPlayerEntity player)
    {
        UUID uuid = player.getUuid();
        PlayerDimensionPosition playerPos = this.registeredPlayers.get(uuid);

        if (playerPos == null || playerPos.dimensionChanged(player))
        {
            this.removeCachedDataForPlayer(uuid);
            this.registeredPlayers.computeIfAbsent(uuid, (u) -> new PlayerDimensionPosition(player)).setPosition(player);
            this.syncStructuresToPlayerWithinRange(player, player.server.getPlayerManager().getViewDistance()); // TODO
            return true;
        }

        return false;
    }

    protected void getStructuresFromGenerator(List<StructureStart> listOut,
                                              ChunkGenerator chunkGenerator,
                                              BlockPos center,
                                              int maxRange)
    {
        if (chunkGenerator instanceof OverworldChunkGenerator)
        {
            StructureFeature feature;

            feature = ((OverworldChunkGeneratorMixin) chunkGenerator).servux_getOceanMonumentFeature();
            this.addStructuresWithinRange(listOut, feature, center, maxRange);

            feature = ((OverworldChunkGeneratorMixin) chunkGenerator).servux_getTempleFeature();
            this.addTempleStructuresWithinRange(listOut, feature, center, maxRange);

            feature = ((OverworldChunkGeneratorMixin) chunkGenerator).servux_getStrongholdFeature();
            this.addStructuresWithinRange(listOut, feature, center, maxRange);

            feature = ((OverworldChunkGeneratorMixin) chunkGenerator).servux_getVillageFeature();
            this.addStructuresWithinRange(listOut, feature, center, maxRange);

            feature = ((OverworldChunkGeneratorMixin) chunkGenerator).servux_getWoodlandMansionFeature();
            this.addStructuresWithinRange(listOut, feature, center, maxRange);
        }
        else if (chunkGenerator instanceof NetherChunkGenerator)
        {
            StructureFeature feature = ((NetherChunkGeneratorMixin) chunkGenerator).servux_getFortressFeature();
            this.addStructuresWithinRange(listOut, feature, center, maxRange);
        }
        else if (chunkGenerator instanceof EndChunkGenerator)
        {
            StructureFeature feature = ((EndChunkGeneratorMixin) chunkGenerator).servux_getEndCityFeature();
            this.addStructuresWithinRange(listOut, feature, center, maxRange);
        }
        else if (chunkGenerator instanceof FlatChunkGenerator)
        {
            Map<String, StructureFeature> map = ((FlatChunkGeneratorMixin) chunkGenerator).servux_getStructureFeatureMap();

            for (StructureFeature feature : map.values())
            {
                if (feature instanceof OceanMonumentFeature)
                {
                    this.addStructuresWithinRange(listOut, feature, center, maxRange);
                }
                else if (feature instanceof AbstractTempleFeature)
                {
                    this.addTempleStructuresWithinRange(listOut, feature, center, maxRange);
                }
                else if (feature instanceof StrongholdFeature)
                {
                    this.addStructuresWithinRange(listOut, feature, center, maxRange);
                }
                else if (feature instanceof VillageFeature)
                {
                    this.addStructuresWithinRange(listOut, feature, center, maxRange);
                }
            }
        }
    }

    protected void addStructuresWithinRange(List<StructureStart> listOut, StructureFeature feature, BlockPos center, int maxRange)
    {
        Long2ObjectMap<StructureStart> structureMap = ((StructureFeatureMixin) feature).servux_getStructureMap();

        for (StructureStart start : structureMap.values())
        {
            if (PositionUtils.isBoxWithinRange(start.getBoundingBox(), center, maxRange))
            {
                listOut.add(start);
            }
        }
    }

    protected void addTempleStructuresWithinRange(List<StructureStart> listOut, StructureFeature feature, BlockPos center, int maxRange)
    {
        Long2ObjectMap<StructureStart> structureMap = ((StructureFeatureMixin) feature).servux_getStructureMap();

        for (StructureStart start : structureMap.values())
        {
            List<StructurePiece> components = start.getChildren();

            if (components.size() == 1 && PositionUtils.isBoxWithinRange(start.getBoundingBox(), center, maxRange))
            {
                String id = Structures.getName(components.get(0));

                if (id != null)
                {
                    listOut.add(start);
                }
            }
        }
    }

    protected ListTag asListTag(List<StructureStart> list)
    {
        ListTag listOut = new ListTag();

        for (StructureStart structure : list)
        {
            CompoundTag tag = structure.method_27890(structure.method_27902(), structure.method_27903());

            if (structure instanceof AbstractTempleFeature.Start)
            {
                List<StructurePiece> pieces = structure.getChildren();

                if (pieces.size() == 1)
                {
                    tag.putString("id", Structures.getName(structure) + "." + Structures.getName(pieces.get(0)));
                }
            }

            listOut.add(tag);
        }

        return listOut;
    }

    protected void sendStructuresToPlayer(List<StructureStart> list, ServerPlayerEntity player)
    {
        CompoundTag tag = new CompoundTag();
        tag.put("Structures", this.asListTag(list));

        PacketSplitter.sendPacketTypeAndCompound(StructureDataPacketHandler.CHANNEL,
                                                 StructureDataPacketHandler.PACKET_S2C_STRUCTURE_DATA,
                                                 tag, player);
    }
}
