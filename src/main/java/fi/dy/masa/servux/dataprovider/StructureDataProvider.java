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
import fi.dy.masa.servux.mixin.structure.ChunkGeneratorEndMixin;
import fi.dy.masa.servux.mixin.structure.ChunkGeneratorFlatMixin;
import fi.dy.masa.servux.mixin.structure.ChunkGeneratorHellMixin;
import fi.dy.masa.servux.mixin.structure.ChunkGeneratorOverworldMixin;
import fi.dy.masa.servux.mixin.structure.MapGenStructureMixin;
import fi.dy.masa.servux.mixin.structure.ChunkProviderServerMixin;
import fi.dy.masa.servux.network.ClientPacketHandler;
import fi.dy.masa.servux.network.PacketSplitter;
import fi.dy.masa.servux.network.packet.StructureDataPacketHandler;
import fi.dy.masa.servux.util.PositionUtils;
import fi.dy.masa.servux.util.data.PlayerDimensionPosition;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkGeneratorEnd;
import net.minecraft.world.gen.ChunkGeneratorFlat;
import net.minecraft.world.gen.ChunkGeneratorHell;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureOceanMonument;
import net.minecraft.world.gen.structure.StructureStart;

public class StructureDataProvider extends BaseDataProvider
{
    public static final StructureDataProvider INSTANCE = new StructureDataProvider();

    protected final Map<UUID, PlayerDimensionPosition> registeredPlayers = new HashMap<>();
    //protected final ArrayListMultimap<UUID, StructureStart> syncedStructures = ArrayListMultimap.create();
    protected final Set<UUID> movedPlayers = new HashSet<>();
    protected final NBTTagCompound metadata = new NBTTagCompound();
    protected int retainDistanceChunks;

    protected StructureDataProvider()
    {
        super("structure_bounding_boxes",
              StructureDataPacketHandler.PROTOCOL_VERSION,
              "Structure Bounding Boxes data for structures such as Witch Huts, Ocean Monuments, Nether Fortresses etc.");

        this.metadata.setString("id", StructureDataPacketHandler.CHANNEL.toString());
        this.metadata.setBoolean("no_timeout", true);
        this.metadata.setInteger("version", StructureDataPacketHandler.PROTOCOL_VERSION);

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
            this.retainDistanceChunks = server.getPlayerList().getViewDistance() + 2;
            Iterator<UUID> uuidIter = this.registeredPlayers.keySet().iterator();

            while (uuidIter.hasNext())
            {
                UUID uuid = uuidIter.next();
                EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(uuid);

                if (player != null)
                {
                    if (this.checkForDimensionChange(player) == false &&
                        this.movedPlayers.contains(uuid))
                    {
                        this.syncStructuresToPlayerWithinRange(player, player.server.getPlayerList().getViewDistance());
                    }
                }
                else
                {
                    uuidIter.remove();
                }
            }
        }
    }

    public void onStartedWatchingChunk(EntityPlayerMP player, Chunk chunk)
    {
        UUID uuid = player.getUniqueID();

        if (this.registeredPlayers.containsKey(uuid))
        {
            this.movedPlayers.add(uuid);
        }
    }

    public boolean register(EntityPlayerMP player)
    {
        UUID uuid = player.getUniqueID();
        boolean registered = false;

        if (this.registeredPlayers.containsKey(uuid) == false)
        {
            PacketSplitter.sendPacketTypeAndCompound(StructureDataPacketHandler.CHANNEL, StructureDataPacketHandler.PACKET_S2C_METADATA, this.metadata, player);

            this.registeredPlayers.put(uuid, new PlayerDimensionPosition(player));
            this.syncStructuresToPlayerWithinRange(player, player.server.getPlayerList().getViewDistance());

            registered = true;
            //System.out.printf("register\n");
        }

        return registered;
    }

    public boolean unregister(EntityPlayerMP player)
    {
        UUID uuid = player.getUniqueID();
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

    protected void syncStructuresToPlayerWithinRange(EntityPlayerMP player, int chunkRadius)
    {
        UUID uuid = player.getUniqueID();

        this.registeredPlayers.computeIfAbsent(uuid, (u) -> new PlayerDimensionPosition(player)).setPosition(player);

        List<StructureStart> list = new ArrayList<>();
        IChunkGenerator generator = ((ChunkProviderServerMixin) player.getEntityWorld().getChunkProvider()).servux_getChunkGenerator();

        this.getStructuresFromGenerator(list, generator, new BlockPos(player), chunkRadius * 16);
        this.sendStructuresToPlayer(list, player);
        this.movedPlayers.remove(uuid);

        //System.out.printf("syncStructuresToPlayerWithinRange() - size: %d\n", list.size());
    }

    protected boolean checkForDimensionChange(EntityPlayerMP player)
    {
        UUID uuid = player.getUniqueID();
        PlayerDimensionPosition playerPos = this.registeredPlayers.get(uuid);

        if (playerPos == null || playerPos.dimensionChanged(player))
        {
            this.removeCachedDataForPlayer(uuid);
            this.registeredPlayers.computeIfAbsent(uuid, (u) -> new PlayerDimensionPosition(player)).setPosition(player);
            this.syncStructuresToPlayerWithinRange(player, player.server.getPlayerList().getViewDistance()); // TODO
            return true;
        }

        return false;
    }

    protected void getStructuresFromGenerator(List<StructureStart> listOut,
                                              IChunkGenerator chunkGenerator,
                                              BlockPos center,
                                              int maxRange)
    {
        if (chunkGenerator instanceof ChunkGeneratorOverworld)
        {
            MapGenStructure feature;

            feature = ((ChunkGeneratorOverworldMixin) chunkGenerator).servux_getOceanMonumentFeature();
            this.addStructuresWithinRange(listOut, feature, center, maxRange);

            feature = ((ChunkGeneratorOverworldMixin) chunkGenerator).servux_getTempleFeature();
            this.addTempleStructuresWithinRange(listOut, feature, center, maxRange);

            feature = ((ChunkGeneratorOverworldMixin) chunkGenerator).servux_getStrongholdFeature();
            this.addStructuresWithinRange(listOut, feature, center, maxRange);

            feature = ((ChunkGeneratorOverworldMixin) chunkGenerator).servux_getVillageFeature();
            this.addStructuresWithinRange(listOut, feature, center, maxRange);

            feature = ((ChunkGeneratorOverworldMixin) chunkGenerator).servux_getWoodlandMansionFeature();
            this.addStructuresWithinRange(listOut, feature, center, maxRange);
        }
        else if (chunkGenerator instanceof ChunkGeneratorHell)
        {
            MapGenStructure feature = ((ChunkGeneratorHellMixin) chunkGenerator).servux_getFortressFeature();
            this.addStructuresWithinRange(listOut, feature, center, maxRange);
        }
        else if (chunkGenerator instanceof ChunkGeneratorEnd)
        {
            MapGenStructure feature = ((ChunkGeneratorEndMixin) chunkGenerator).servux_getEndCityFeature();
            this.addStructuresWithinRange(listOut, feature, center, maxRange);
        }
        else if (chunkGenerator instanceof ChunkGeneratorFlat)
        {
            Map<String, MapGenStructure> map = ((ChunkGeneratorFlatMixin) chunkGenerator).servux_getStructureFeatureMap();

            for (MapGenStructure feature : map.values())
            {
                if (feature instanceof StructureOceanMonument)
                {
                    this.addStructuresWithinRange(listOut, feature, center, maxRange);
                }
                else if (feature instanceof MapGenScatteredFeature)
                {
                    this.addTempleStructuresWithinRange(listOut, feature, center, maxRange);
                }
                else if (feature instanceof MapGenStronghold)
                {
                    this.addStructuresWithinRange(listOut, feature, center, maxRange);
                }
                else if (feature instanceof MapGenVillage)
                {
                    this.addStructuresWithinRange(listOut, feature, center, maxRange);
                }
            }
        }
    }

    protected void addStructuresWithinRange(List<StructureStart> listOut, MapGenStructure feature, BlockPos center, int maxRange)
    {
        Long2ObjectMap<StructureStart> structureMap = ((MapGenStructureMixin) feature).servux_getStructureMap();

        for (StructureStart start : structureMap.values())
        {
            if (PositionUtils.isBoxWithinRange(start.getBoundingBox(), center, maxRange))
            {
                listOut.add(start);
            }
        }
    }

    protected void addTempleStructuresWithinRange(List<StructureStart> listOut, MapGenStructure feature, BlockPos center, int maxRange)
    {
        Long2ObjectMap<StructureStart> structureMap = ((MapGenStructureMixin) feature).servux_getStructureMap();

        for (StructureStart start : structureMap.values())
        {
            List<StructureComponent> components = start.getComponents();

            if (components.size() == 1 && PositionUtils.isBoxWithinRange(start.getBoundingBox(), center, maxRange))
            {
                String id = MapGenStructureIO.getStructureComponentName(components.get(0));

                if (id != null)
                {
                    listOut.add(start);
                }
            }
        }
    }

    protected NBTTagList asListTag(List<StructureStart> list)
    {
        NBTTagList listOut = new NBTTagList();

        for (StructureStart structure : list)
        {
            NBTTagCompound tag = structure.writeStructureComponentsToNBT(structure.getChunkPosX(), structure.getChunkPosZ());

            if (structure instanceof MapGenScatteredFeature.Start)
            {
                List<StructureComponent> pieces = structure.getComponents();

                if (pieces.size() == 1)
                {
                    tag.setString("id", MapGenStructureIO.getStructureStartName(structure) + "." + MapGenStructureIO.getStructureComponentName(pieces.get(0)));
                }
            }

            listOut.appendTag(tag);
        }

        return listOut;
    }

    protected void sendStructuresToPlayer(List<StructureStart> list, EntityPlayerMP player)
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Structures", this.asListTag(list));

        PacketSplitter.sendPacketTypeAndCompound(StructureDataPacketHandler.CHANNEL,
                                                 StructureDataPacketHandler.PACKET_S2C_STRUCTURE_DATA,
                                                 tag, player);
    }
}
