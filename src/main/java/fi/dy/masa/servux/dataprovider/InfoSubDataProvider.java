package fi.dy.masa.servux.dataprovider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import fi.dy.masa.servux.network.ClientPacketHandler;
import fi.dy.masa.servux.network.packet.InfoSubPacketHandler;
import fi.dy.masa.servux.network.util.PacketUtils;
import fi.dy.masa.servux.util.EntityUtils;
import fi.dy.masa.servux.util.NbtUtils;
import fi.dy.masa.servux.util.WorldUtils;
import fi.dy.masa.servux.util.data.MobCaps;
import fi.dy.masa.servux.util.data.PerDimensionObjectCounts;
import fi.dy.masa.servux.util.data.TpsData;
import fi.dy.masa.servux.util.palette.HashMapPalette;
import fi.dy.masa.servux.util.palette.Palette;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

public class InfoSubDataProvider extends BaseDataProvider
{
    public static final InfoSubDataProvider INSTANCE = new InfoSubDataProvider();

    protected final Int2ObjectOpenHashMap<BiConsumer<PacketBuffer, EntityPlayerMP>> dataWriters = new Int2ObjectOpenHashMap<>();
    protected final HashMap<UUID, IntOpenHashSet> registeredChannels = new HashMap<>();

    protected final Int2ObjectOpenHashMap<PerDimensionObjectCounts> perDimensionCounts = new Int2ObjectOpenHashMap<>();
    protected final Int2ObjectOpenHashMap<MobCaps> mobCapsPerDimension = new Int2ObjectOpenHashMap<>();
    protected final TpsData tpsData = new TpsData();
    protected HashMapPalette<String> channelIdPalette = new HashMapPalette<>(7, this::resizePalette);

    protected final IntOpenHashSet countChannels = new IntOpenHashSet();
    protected final IntOpenHashSet mobCapChannels = new IntOpenHashSet();
    protected final IntOpenHashSet tpsChannels = new IntOpenHashSet();
    protected boolean hasAnySubscribers;
    protected boolean hasCountSubscribers;
    protected boolean hasMobCapSubscribers;
    protected boolean hasTpsSubscribers;
    protected boolean needsSubscriberCheck;

    protected InfoSubDataProvider()
    {
        super("info_subscriptions",
              InfoSubPacketHandler.PROTOCOL_VERSION,
              "Miscellaneous info subscriptions, such as TPS, MSPT, loaded entity/block entity/chunk count");

        this.shouldTick = true;
        this.setTickRate(20);
        this.initializeChannels();
    }

    @Override
    public ClientPacketHandler getPacketHandler()
    {
        return InfoSubPacketHandler.INSTANCE;
    }

    @Override
    public void tick(MinecraftServer server, int tickCounter)
    {
        this.updateSubscriberCaches();

        if (this.hasAnySubscribers)
        {
            this.updateData(server);
            this.sendUpdatePackets(server);
        }
    }

    protected void updateData(MinecraftServer server)
    {
        if (this.hasTpsSubscribers)
        {
            this.tpsData.update(server);
        }

        for (WorldServer world : server.worlds)
        {
            try
            {
                int dim = WorldUtils.getDimensionId(world);

                if (this.hasCountSubscribers)
                {
                    this.perDimensionCounts.get(dim).update(world);
                }

                if (this.hasMobCapSubscribers)
                {
                    this.mobCapsPerDimension.get(dim).update(world);
                }
            }
            catch (Exception ignore) {}
        }
    }

    public void register(String channel, NetHandlerPlayServer netHandler)
    {
        int id = this.channelIdPalette.getIdFor(channel);

        if (id >= 0)
        {
            this.registeredChannels.computeIfAbsent(netHandler.player.getUniqueID(), (u) -> new IntOpenHashSet()).add(id);
            this.setNeedsSubscriberCheck();
        }
    }

    public void unregister(String channel, NetHandlerPlayServer netHandler)
    {
        UUID uuid = netHandler.player.getUniqueID();
        IntOpenHashSet set = this.registeredChannels.get(uuid);
        int id = this.channelIdPalette.getIdFor(channel);

        if (id >= 0 && set != null)
        {
            set.rem(id);

            if (set.isEmpty())
            {
                this.registeredChannels.remove(uuid);
            }

            this.setNeedsSubscriberCheck();
        }
    }

    public void unregisterAllChannels(UUID uuid)
    {
        this.registeredChannels.remove(uuid);
        this.setNeedsSubscriberCheck();
    }

    public void sendMetadata(NetHandlerPlayServer netHandler)
    {
        NBTTagCompound tag = new NBTTagCompound();

        tag.setInteger("version", this.getProtocolVersion());
        tag.setTag("channel_ids", NbtUtils.asListTag(this.channelIdPalette.getMapping()));

        PacketUtils.sendPacketTypeAndCompound(InfoSubPacketHandler.REG_CHANNEL,
                                              InfoSubPacketHandler.PACKET_S2C_METADATA,
                                              tag, netHandler);
    }

    protected void setNeedsSubscriberCheck()
    {
        this.needsSubscriberCheck = true;
    }

    protected void updateSubscriberCaches()
    {
        if (this.needsSubscriberCheck)
        {
            this.hasCountSubscribers = this.containsAny(this.countChannels, this.registeredChannels.values());
            this.hasMobCapSubscribers = this.containsAny(this.mobCapChannels, this.registeredChannels.values());
            this.hasTpsSubscribers = this.containsAny(this.tpsChannels, this.registeredChannels.values());
            this.hasAnySubscribers = this.registeredChannels.isEmpty() == false;
            this.needsSubscriberCheck = false;
        }
    }

    protected boolean containsAny(IntOpenHashSet haystack, Collection<IntOpenHashSet> sets)
    {
        return sets.stream().anyMatch((s) -> s.stream().anyMatch(haystack::contains));
    }

    protected void sendUpdatePackets(MinecraftServer server)
    {
        for (UUID uuid : this.registeredChannels.keySet())
        {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(uuid);

            if (player != null)
            {
                this.sendUpdateToPlayer(player, this.registeredChannels.get(uuid));
            }
            else
            {
                this.unregisterAllChannels(uuid);
            }
        }
    }

    protected void sendUpdateToPlayer(EntityPlayerMP player, IntOpenHashSet channelIds)
    {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeVarInt(channelIds.size());

        for (int channelId : channelIds)
        {
            this.addDataForChannel(channelId, buf, player);
        }

        PacketUtils.send(InfoSubPacketHandler.DATA_CHANNEL, buf, player.connection);
    }

    protected void addDataForChannel(int channelId, PacketBuffer buf, EntityPlayerMP player)
    {
        BiConsumer<PacketBuffer, EntityPlayerMP> dataWriter = this.dataWriters.get(channelId);

        buf.writeVarInt(channelId);

        if (dataWriter != null && channelId >= 0)
        {
            try
            {
                dataWriter.accept(buf, player);
            }
            catch (Exception ignore) {}
        }
        else
        {
            buf.writeVarInt(0);
        }
    }

    protected void initializeChannels()
    {
        // minecraft.status.chunk_loading.dropped_chunks.hash_size[.dimension_name]
        // minecraft.status.count.loaded.block_entities[.dimension_name]
        // minecraft.status.count.loaded.block_entities.ticking[.dimension_name]
        // minecraft.status.count.loaded.chunks[.dimension_name]
        // minecraft.status.count.loaded.entities[.dimension_name]
        // minecraft.status.count.mobcap.<capname>.val[.dimension_name]
        // minecraft.status.count.mobcap.<capname>.max[.dimension_name]
        // minecraft.status.count.scheduled_block_ticks[.dimension_name]
        // minecraft.status.performance.tps
        // minecraft.status.performance.mspt

        // 1.13+/1.14+
        // minecraft.status.count.loaded.pois[.dimension_name]
        // minecraft.status.count.scheduled_fluid_ticks[.dimension_name]

        this.initializeTpsData();

        Integer[] dims = new Integer[] { null, 0, -1, 1 };

        for (Integer dim : dims)
        {
            this.initializeCountsForDimension(dim);
            this.initializeMobCapsForDimension(dim);
        }
    }

    protected void initializeTpsData()
    {
        int id = this.channelIdPalette.getOrAddIdFor("minecraft.status.performance.tps");
        this.dataWriters.put(id, (buf, p) -> buf.writeFloat(this.tpsData.getServerTps()));
        this.tpsChannels.add(id);

        id = this.channelIdPalette.getOrAddIdFor("minecraft.status.performance.mspt");
        this.dataWriters.put(id, (buf, p) -> buf.writeFloat(this.tpsData.getServerMspt()));
        this.tpsChannels.add(id);
    }

    protected void initializeMobCapsForDimension(@Nullable Integer dimension)
    {
        if (dimension != null)
        {
            this.mobCapsPerDimension.put(dimension.intValue(), new MobCaps());
        }

        for (EnumCreatureType group : MobCaps.ENTITY_CATEGORIES)
        {
            String groupName = group.name().toLowerCase(Locale.ROOT);

            String channel = buildChannelName(String.format("minecraft.status.count.mobcap.%s.val", groupName), dimension);
            int id = this.channelIdPalette.getOrAddIdFor(channel);
            this.dataWriters.put(id, (buf, p) -> buf.writeVarInt(this.mobCapsPerDimension.get(getActiveDimension(p, dimension)).getMobCap(group).getValue()));
            this.mobCapChannels.add(id);

            channel = buildChannelName(String.format("minecraft.status.count.mobcap.%s.max", groupName), dimension);
            id = this.channelIdPalette.getOrAddIdFor(channel);
            this.dataWriters.put(id, (buf, p) -> buf.writeVarInt(this.mobCapsPerDimension.get(getActiveDimension(p, dimension)).getMobCap(group).getLimit()));
            this.mobCapChannels.add(id);
        }
    }

    protected void initializeCountsForDimension(@Nullable Integer dimension)
    {
        if (dimension != null)
        {
            this.perDimensionCounts.put(dimension.intValue(), new PerDimensionObjectCounts());
        }

        this.initializeCountForDimension("minecraft.status.chunk_loading.dropped_chunks.hash_size", dimension, PerDimensionObjectCounts::getDroppedChunksHashSize);
        this.initializeCountForDimension("minecraft.status.count.loaded.block_entities",            dimension, PerDimensionObjectCounts::getLoadedBlockEntities);
        this.initializeCountForDimension("minecraft.status.count.loaded.block_entities.ticking",    dimension, PerDimensionObjectCounts::getLoadedBlockEntitiesTicking);
        this.initializeCountForDimension("minecraft.status.count.loaded.chunks",                    dimension, PerDimensionObjectCounts::getLoadedChunks);
        this.initializeCountForDimension("minecraft.status.count.loaded.entities",                  dimension, PerDimensionObjectCounts::getLoadedEntities);
        this.initializeCountForDimension("minecraft.status.count.scheduled_block_ticks",            dimension, PerDimensionObjectCounts::getScheduledTicks);
    }

    protected void initializeCountForDimension(String channel, @Nullable Integer dimension, Function<PerDimensionObjectCounts, Integer> func)
    {
        channel = buildChannelName(channel, dimension);
        int id = this.channelIdPalette.getOrAddIdFor(channel);
        this.dataWriters.put(id, (buf, p) -> buf.writeVarInt(func.apply(this.perDimensionCounts.get(getActiveDimension(p, dimension)))));
        this.countChannels.add(id);
    }

    public static String buildChannelName(String base, @Nullable Integer dimension)
    {
        return dimension != null ? base + "." + dimension : base;
    }

    public static int getActiveDimension(Entity entity, @Nullable Integer dimension)
    {
        return dimension != null ? dimension : EntityUtils.getDimension(entity);
    }

    protected MobCaps getMobCapInDimension(Entity entity)
    {
        return this.mobCapsPerDimension.get(EntityUtils.getDimension(entity));
    }

    protected int resizePalette(int bitsIn, String valueBeingAdded, Palette<String> oldPalette)
    {
        HashMapPalette<String> newPalette = new HashMapPalette<>(bitsIn, this::resizePalette);
        newPalette.setMapping(oldPalette.getMapping());
        this.channelIdPalette = newPalette;
        return newPalette.getOrAddIdFor(valueBeingAdded);
    }
}
