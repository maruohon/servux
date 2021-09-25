package fi.dy.masa.servux.dataprovider;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.PacketByteBuf;
import fi.dy.masa.servux.network.ClientPacketHandler;
import fi.dy.masa.servux.network.PacketSplitter;
import fi.dy.masa.servux.network.packet.InfoSubPacketHandler;
import fi.dy.masa.servux.util.EntityUtils;
import fi.dy.masa.servux.util.NbtUtils;
import fi.dy.masa.servux.util.WorldUtils;
import fi.dy.masa.servux.util.data.MobCaps;
import fi.dy.masa.servux.util.data.PerDimensionObjectCounts;
import fi.dy.masa.servux.util.data.TpsData;
import fi.dy.masa.servux.util.palette.HashMapPalette;
import fi.dy.masa.servux.util.palette.Palette;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class InfoSubDataProvider extends BaseDataProvider
{
    public static final InfoSubDataProvider INSTANCE = new InfoSubDataProvider();

    protected final ListMultimap<UUID, String> registeredChannels = ArrayListMultimap.create();
    protected final Map<String, BiConsumer<PacketByteBuf, ServerPlayerEntity>> dataWriters = new HashMap<>();

    protected final Int2ObjectOpenHashMap<MobCaps> mobCapsPerDimension = new Int2ObjectOpenHashMap<>();
    protected final Int2ObjectOpenHashMap<PerDimensionObjectCounts> perDimensionCounts = new Int2ObjectOpenHashMap<>();
    protected final TpsData tpsData = new TpsData();

    protected HashMapPalette<String> channelIdPalette = new HashMapPalette<>(7, this::resizePalette);

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
        this.tpsData.update(server);

        for (ServerWorld world : server.worlds)
        {
            int dim = WorldUtils.getDimensionId(world);

            try
            {
                this.perDimensionCounts.get(dim).update(world);
                this.mobCapsPerDimension.get(dim).update(world);
            }
            catch (Exception ignore) {}
        }

        this.sendUpdatePackets(server);
    }

    public void register(String channel, ServerPlayNetworkHandler netHandler)
    {
        this.registeredChannels.put(netHandler.player.getUuid(), channel);
    }

    public void unregister(String channel, ServerPlayNetworkHandler netHandler)
    {
        this.registeredChannels.remove(netHandler.player.getUuid(), channel);
    }

    public void unregisterAllChannels(ServerPlayNetworkHandler netHandler)
    {
        this.registeredChannels.removeAll(netHandler.player.getUuid());
    }

    public void sendMetadata(ServerPlayNetworkHandler netHandler)
    {
        CompoundTag tag = new CompoundTag();

        tag.putInt("version", this.getProtocolVersion());
        tag.put("channel_ids", NbtUtils.asListTag(this.channelIdPalette.getMapping()));

        PacketSplitter.sendPacketTypeAndCompound(InfoSubPacketHandler.REG_CHANNEL,
                                                 InfoSubPacketHandler.PACKET_S2C_METADATA,
                                                 tag, netHandler);
    }

    protected void sendUpdatePackets(MinecraftServer server)
    {
        for (UUID uuid : this.registeredChannels.keySet())
        {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);

            if (player != null)
            {
                this.sendUpdateToPlayer(player, this.registeredChannels.get(uuid));
            }
            else
            {
                this.registeredChannels.removeAll(uuid);
            }
        }
    }

    protected void sendUpdateToPlayer(ServerPlayerEntity player, List<String> channels)
    {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(channels.size());

        for (String channel : channels)
        {
            this.addDataForChannel(channel, buf, player);
        }

        PacketSplitter.send(buf, InfoSubPacketHandler.DATA_CHANNEL, player.networkHandler);
    }

    protected void addDataForChannel(String channel, PacketByteBuf buf, ServerPlayerEntity player)
    {
        BiConsumer<PacketByteBuf, ServerPlayerEntity> dataWriter = this.dataWriters.get(channel);
        int id = this.channelIdPalette.idFor(channel);

        buf.writeVarInt(id);

        if (dataWriter != null && id >= 0)
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
        String channel = "minecraft.status.performance.tps";
        this.channelIdPalette.idFor(channel);
        this.dataWriters.put(channel, (buf, p) -> buf.writeFloat(this.tpsData.getServerTps()));

        channel = "minecraft.status.performance.mspt";
        this.channelIdPalette.idFor(channel);
        this.dataWriters.put(channel, (buf, p) -> buf.writeFloat(this.tpsData.getServerMspt()));
    }

    protected void initializeMobCapsForDimension(@Nullable Integer dimension)
    {
        if (dimension != null)
        {
            this.mobCapsPerDimension.put(dimension.intValue(), new MobCaps());
        }

        for (SpawnGroup group : MobCaps.ENTITY_CATEGORIES)
        {
            String groupName = group.name().toLowerCase(Locale.ROOT);

            String channel = buildChannelName(String.format("minecraft.status.count.mobcap.%s.val", groupName), dimension);
            this.channelIdPalette.idFor(channel);
            this.dataWriters.put(channel, (buf, p) -> buf.writeVarInt(this.mobCapsPerDimension.get(getActiveDimension(p, dimension)).getMobCap(group).getValue()));

            channel = buildChannelName(String.format("minecraft.status.count.mobcap.%s.max", groupName), dimension);
            this.channelIdPalette.idFor(channel);
            this.dataWriters.put(channel, (buf, p) -> buf.writeVarInt(this.mobCapsPerDimension.get(getActiveDimension(p, dimension)).getMobCap(group).getLimit()));
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
        this.channelIdPalette.idFor(channel);
        this.dataWriters.put(channel, (buf, p) -> buf.writeVarInt(func.apply(this.perDimensionCounts.get(getActiveDimension(p, dimension)))));
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
        return newPalette.idFor(valueBeingAdded);
    }
}
