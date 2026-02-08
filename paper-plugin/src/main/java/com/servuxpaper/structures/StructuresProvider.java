package com.servuxpaper.structures;

import com.servuxpaper.ServuxPaperPlugin;
import com.servuxpaper.util.NbtCodec;
import com.servuxpaper.util.PacketSender;
import com.servuxpaper.util.PlayerDimensionPosition;
import com.servuxpaper.util.Timeout;
import com.servuxpaper.util.VarInts;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StructuresProvider implements PluginMessageListener, Listener {
    private static final int PROTOCOL_VERSION = 2;
    private static final int PACKET_S2C_METADATA = 1;
    private static final int PACKET_S2C_STRUCTURE_DATA = 2;
    private static final int PACKET_C2S_STRUCTURES_REGISTER = 3;
    private static final int PACKET_C2S_STRUCTURES_UNREGISTER = 4;
    private static final int PACKET_C2S_REQUEST_SPAWN_METADATA = 11;

    private final ServuxPaperPlugin plugin;
    private final Map<UUID, PlayerState> players = new HashMap<>();
    private final String permissionNode = "servux.provider.structure_bounding_boxes";

    private final boolean enabled;
    private final int updateInterval;
    private final int timeoutTicks;
    private final int permissionLevel;
    private final boolean whitelistEnabled;
    private final boolean blacklistEnabled;
    private final Set<String> whitelist;
    private final Set<String> blacklist;

    public StructuresProvider(ServuxPaperPlugin plugin) {
        this.plugin = plugin;
        var cfg = plugin.getConfig();
        this.enabled = cfg.getBoolean("structures.enabled", true);
        this.updateInterval = cfg.getInt("structures.updateIntervalTicks", 40);
        this.timeoutTicks = cfg.getInt("structures.timeoutTicks", 600);
        this.permissionLevel = cfg.getInt("structures.permissionLevel", 0);
        this.whitelistEnabled = cfg.getBoolean("structures.whitelistEnabled", false);
        this.blacklistEnabled = cfg.getBoolean("structures.blacklistEnabled", false);
        this.whitelist = new HashSet<>(cfg.getStringList("structures.whitelist"));
        this.blacklist = new HashSet<>(cfg.getStringList("structures.blacklist"));
    }

    public void tick(int tickCounter) {
        if (!enabled) {
            return;
        }
        if (tickCounter % updateInterval != 0) {
            return;
        }
        int viewDistance = Bukkit.getViewDistance() + 2;
        for (Map.Entry<UUID, PlayerState> entry : new HashSet<>(players.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                players.remove(entry.getKey());
                continue;
            }
            if (!hasPermission(player)) {
                players.remove(entry.getKey());
                continue;
            }
            PlayerState state = entry.getValue();
            boolean needsUpdate = state.pos.dimensionChanged(player) || state.pos.chunkChanged(player) || state.timeout.needsUpdate(tickCounter, timeoutTicks);
            if (needsUpdate) {
                sendStructures(player, viewDistance, tickCounter);
                state.pos.setPosition(player);
                state.timeout.setLastSync(tickCounter);
            }
        }
    }

    @EventHandler
    public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
        if (!enabled) {
            return;
        }
        if (ServuxPaperPlugin.CHANNEL_STRUCTURES.equals(event.getChannel())) {
            register(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) {
            return;
        }
        Player player = event.getPlayer();
        if (player.getListeningPluginChannels().contains(ServuxPaperPlugin.CHANNEL_STRUCTURES)) {
            register(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!enabled || !ServuxPaperPlugin.CHANNEL_STRUCTURES.equals(channel)) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            int packetType = VarInts.readVarInt(in);
            switch (packetType) {
                case PACKET_C2S_STRUCTURES_REGISTER -> register(player);
                case PACKET_C2S_STRUCTURES_UNREGISTER -> unregister(player);
                case PACKET_C2S_REQUEST_SPAWN_METADATA -> plugin.getHudProvider().refreshSpawnMetadata(player);
                default -> {
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void register(Player player) {
        if (!enabled || !hasPermission(player)) {
            return;
        }
        if (players.containsKey(player.getUniqueId())) {
            return;
        }
        players.put(player.getUniqueId(), new PlayerState(player));
        sendMetadata(player);
        sendStructures(player, Bukkit.getViewDistance() + 2, 0);
    }

    private void unregister(Player player) {
        players.remove(player.getUniqueId());
    }

    private void sendMetadata(Player player) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("name", "structure_bounding_boxes");
        nbt.putString("id", ServuxPaperPlugin.CHANNEL_STRUCTURES);
        nbt.putInt("version", PROTOCOL_VERSION);
        nbt.putString("servux", plugin.getServuxString());
        nbt.putInt("timeout", timeoutTicks);
        PacketSender.sendNbt(plugin, player, ServuxPaperPlugin.CHANNEL_STRUCTURES, PACKET_S2C_METADATA, nbt);
    }

    private void sendStructures(Player player, int chunkRadius, int tickCounter) {
        ServerLevel world = ((CraftWorld) player.getWorld()).getHandle();
        ChunkPos center = new ChunkPos(player.getLocation().getBlockX() >> 4, player.getLocation().getBlockZ() >> 4);
        Map<Structure, LongSet> references = getStructureReferencesWithinRange(world, center, chunkRadius);
        Map<ChunkPos, StructureStart> starts = getStructureStartsFromReferences(world, references);
        if (starts.isEmpty()) {
            return;
        }
        ListTag list = getStructureList(starts, world);
        if (list.isEmpty()) {
            return;
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Structures", list);
        byte[] data = NbtCodec.encodeNbt(nbt);
        PacketSender.sendSplitBuffer(plugin, player, ServuxPaperPlugin.CHANNEL_STRUCTURES, PACKET_S2C_STRUCTURE_DATA, data);
        PlayerState state = players.get(player.getUniqueId());
        if (state != null) {
            state.timeout.setLastSync(tickCounter);
        }
    }

    private Map<Structure, LongSet> getStructureReferencesWithinRange(ServerLevel world, ChunkPos center, int chunkRadius) {
        Map<Structure, LongSet> references = new HashMap<>();
        for (int cx = center.x - chunkRadius; cx <= center.x + chunkRadius; ++cx) {
            for (int cz = center.z - chunkRadius; cz <= center.z + chunkRadius; ++cz) {
                getStructureReferencesFromChunk(world, cx, cz, references);
            }
        }
        return references;
    }

    private void getStructureReferencesFromChunk(ServerLevel world, int chunkX, int chunkZ, Map<Structure, LongSet> references) {
        if (!world.hasChunk(chunkX, chunkZ)) {
            return;
        }
        var chunkAccess = world.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_REFERENCES, false);
        if (!(chunkAccess instanceof LevelChunk chunk)) {
            return;
        }
        for (Map.Entry<Structure, LongSet> entry : chunk.getAllReferences().entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            references.merge(entry.getKey(), entry.getValue(), (oldSet, entrySet) -> {
                LongOpenHashSet newSet = new LongOpenHashSet(oldSet);
                newSet.addAll(entrySet);
                return newSet;
            });
        }
    }

    private Map<ChunkPos, StructureStart> getStructureStartsFromReferences(ServerLevel world, Map<Structure, LongSet> references) {
        Map<ChunkPos, StructureStart> starts = new HashMap<>();
        for (Map.Entry<Structure, LongSet> entry : references.entrySet()) {
            Structure structure = entry.getKey();
            LongSet startChunks = entry.getValue();
            LongIterator iter = startChunks.iterator();
            while (iter.hasNext()) {
                ChunkPos pos = new ChunkPos(iter.nextLong());
                if (!world.hasChunk(pos.x, pos.z)) {
                    continue;
                }
                var chunkAccess = world.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_REFERENCES, false);
                if (!(chunkAccess instanceof LevelChunk chunk)) {
                    continue;
                }
                StructureStart start = chunk.getStartForStructure(structure);
                if (start != null) {
                    starts.put(pos, start);
                }
            }
        }
        return starts;
    }

    private ListTag getStructureList(Map<ChunkPos, StructureStart> structures, ServerLevel world) {
        ListTag list = new ListTag();
        StructurePieceSerializationContext ctx = StructurePieceSerializationContext.fromLevel(world);
        for (Map.Entry<ChunkPos, StructureStart> entry : structures.entrySet()) {
            StructureStart start = entry.getValue();
            Structure structure = start.getStructure();
            if (structure == null) {
                continue;
            }
            Identifier structureType = BuiltInRegistries.STRUCTURE_TYPE.getKey(structure.type());
            if (structureType == null || !shouldSendStructure(structureType)) {
                continue;
            }
            boolean expandBox = structure.terrainAdaptation() != TerrainAdjustment.NONE;
            CompoundTag nbt = start.createTag(ctx, entry.getKey());
            nbt.putBoolean("ExpandBox", expandBox);
            list.add(nbt);
        }
        return list;
    }

    private boolean shouldSendStructure(Identifier id) {
        String key = id.toString();
        if (whitelistEnabled) {
            return whitelist.contains(key);
        }
        if (blacklistEnabled) {
            return !blacklist.contains(key);
        }
        return true;
    }

    private boolean hasPermission(Player player) {
        if (permissionLevel <= 0) {
            return true;
        }
        return player.hasPermission(permissionNode) || player.isOp();
    }

    private static final class PlayerState {
        private final PlayerDimensionPosition pos;
        private final Timeout timeout;

        private PlayerState(Player player) {
            this.pos = new PlayerDimensionPosition(player);
            this.timeout = new Timeout(0);
        }
    }
}
