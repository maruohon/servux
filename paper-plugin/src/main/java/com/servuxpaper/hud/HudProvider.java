package com.servuxpaper.hud;

import com.servuxpaper.ServuxPaperPlugin;
import com.servuxpaper.util.NbtCodec;
import com.servuxpaper.util.PacketSender;
import com.servuxpaper.util.VarInts;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HudProvider implements PluginMessageListener, Listener {
    private static final int PROTOCOL_VERSION = 2;
    private static final int PACKET_S2C_METADATA = 1;
    private static final int PACKET_C2S_METADATA_REQUEST = 2;
    private static final int PACKET_S2C_SPAWN_DATA = 3;
    private static final int PACKET_C2S_SPAWN_DATA_REQUEST = 4;
    private static final int PACKET_S2C_WEATHER_TICK = 5;
    private static final int PACKET_C2S_RECIPE_MANAGER_REQUEST = 6;
    private static final int PACKET_S2C_DATA_LOGGER_TICK = 7;
    private static final int PACKET_C2S_DATA_LOGGER_REQUEST = 8;
    private static final int PACKET_S2C_NBT_RESPONSE_DATA = 11;

    private final ServuxPaperPlugin plugin;
    private final String permissionNode = "servux.provider.hud_data";

    private final boolean enabled;
    private final int updateInterval;
    private final int permissionLevel;
    private final boolean shareWeatherStatus;
    private final int weatherPermissionLevel;
    private final boolean shareSeed;
    private final int seedPermissionLevel;
    private final boolean loggersEnabled;
    private final List<String> loggersEnabledList;
    private final int loggerPermissionLevel;
    private final int loggerTickInterval;

    private World spawnWorld;
    private int spawnX;
    private int spawnY;
    private int spawnZ;
    private long worldSeed;

    private boolean refreshSpawnMetadata;
    private boolean refreshWeatherData;

    private int clearWeatherTime = -1;
    private int rainWeatherTime = -1;
    private int thunderWeatherTime = -1;
    private boolean isRaining;
    private boolean isThundering;

    private final Map<UUID, Set<LoggerType>> loggerPlayers = new HashMap<>();

    public HudProvider(ServuxPaperPlugin plugin) {
        this.plugin = plugin;
        var cfg = plugin.getConfig();
        this.enabled = cfg.getBoolean("hud.enabled", true);
        this.updateInterval = cfg.getInt("hud.updateIntervalTicks", 40);
        this.permissionLevel = cfg.getInt("hud.permissionLevel", 0);
        this.shareWeatherStatus = cfg.getBoolean("hud.shareWeatherStatus", false);
        this.weatherPermissionLevel = cfg.getInt("hud.weatherPermissionLevel", 0);
        this.shareSeed = cfg.getBoolean("hud.shareSeed", false);
        this.seedPermissionLevel = cfg.getInt("hud.seedPermissionLevel", 2);
        this.loggersEnabled = cfg.getBoolean("hud.loggersEnabled", true);
        this.loggersEnabledList = cfg.getStringList("hud.loggersEnabledList");
        this.loggerPermissionLevel = cfg.getInt("hud.loggerPermissionLevel", 0);
        this.loggerTickInterval = cfg.getInt("hud.loggerTickIntervalTicks", 15);

        updateSpawnFromServer();
    }

    public void tick(int tickCounter) {
        if (!enabled) {
            return;
        }
        if (tickCounter % updateInterval == 0) {
            updateSpawnFromServer();
            updateWeatherFromServer();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!hasPermission(player)) {
                    continue;
                }
                if (refreshWeatherData && shareWeatherStatus && hasWeatherPermission(player)) {
                    sendWeather(player);
                }
                if (refreshSpawnMetadata) {
                    refreshSpawnMetadata(player);
                }
            }
            refreshWeatherData = false;
            refreshSpawnMetadata = false;
        }

        if (loggersEnabled && tickCounter % loggerTickInterval == 0) {
            CompoundTag tpsTag = buildTpsData();
            CompoundTag mobCapsTag = buildMobCapsData();
            for (Map.Entry<UUID, Set<LoggerType>> entry : new HashMap<>(loggerPlayers).entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null || !player.isOnline()) {
                    loggerPlayers.remove(entry.getKey());
                    continue;
                }
                if (!hasLoggerPermission(player)) {
                    continue;
                }
                CompoundTag nbt = new CompoundTag();
                for (LoggerType type : entry.getValue()) {
                    if (type == LoggerType.TPS && tpsTag != null) {
                        nbt.put(type.key, tpsTag);
                    } else if (type == LoggerType.MOB_CAPS && mobCapsTag != null) {
                        nbt.put(type.key, mobCapsTag);
                    }
                }
                if (!nbt.isEmpty()) {
                    PacketSender.sendNbt(plugin, player, ServuxPaperPlugin.CHANNEL_HUD, PACKET_S2C_DATA_LOGGER_TICK, nbt);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
        if (!enabled) {
            return;
        }
        if (ServuxPaperPlugin.CHANNEL_HUD.equals(event.getChannel())) {
            sendMetadata(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) {
            return;
        }
        Player player = event.getPlayer();
        if (player.getListeningPluginChannels().contains(ServuxPaperPlugin.CHANNEL_HUD)) {
            sendMetadata(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        loggerPlayers.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!enabled || !ServuxPaperPlugin.CHANNEL_HUD.equals(channel)) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            int packetType = VarInts.readVarInt(in);
            switch (packetType) {
                case PACKET_C2S_METADATA_REQUEST -> sendMetadata(player);
                case PACKET_C2S_SPAWN_DATA_REQUEST -> refreshSpawnMetadata(player);
                case PACKET_C2S_RECIPE_MANAGER_REQUEST -> sendRecipeManager(player);
                case PACKET_C2S_DATA_LOGGER_REQUEST -> handleLoggerRequest(player, message, in);
                default -> {
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void handleLoggerRequest(Player player, byte[] message, DataInputStream in) throws IOException {
        if (!loggersEnabled || !hasLoggerPermission(player)) {
            return;
        }
        byte[] rest = in.readAllBytes();
        CompoundTag nbt = NbtCodec.decodeNbt(rest);
        if (nbt == null || nbt.isEmpty()) {
            loggerPlayers.remove(player.getUniqueId());
            return;
        }
        EnumSet<LoggerType> requested = EnumSet.noneOf(LoggerType.class);
        for (String key : nbt.keySet()) {
            if (!nbt.getBooleanOr(key, false)) {
                continue;
            }
            LoggerType type = LoggerType.fromKey(key);
            if (type == null || !isLoggerTypeEnabled(type)) {
                continue;
            }
            requested.add(type);
        }
        if (requested.isEmpty()) {
            loggerPlayers.remove(player.getUniqueId());
        } else {
            loggerPlayers.put(player.getUniqueId(), requested);
        }
    }

    private void sendMetadata(Player player) {
        if (!enabled || !hasPermission(player)) {
            return;
        }
        CompoundTag nbt = new CompoundTag();
        nbt.putString("name", "hud_data");
        nbt.putString("id", ServuxPaperPlugin.CHANNEL_HUD);
        nbt.putInt("version", PROTOCOL_VERSION);
        nbt.putString("servux", plugin.getServuxString());
        if (spawnWorld != null) {
            nbt.putString("spawnDimension", spawnWorld.getKey().toString());
            nbt.putInt("spawnPosX", spawnX);
            nbt.putInt("spawnPosY", spawnY);
            nbt.putInt("spawnPosZ", spawnZ);
        }
        if (shareSeed && hasSeedPermission(player)) {
            nbt.putLong("worldSeed", worldSeed);
        }
        if (loggersEnabled) {
            CompoundTag loggers = new CompoundTag();
            for (LoggerType type : LoggerType.values()) {
                loggers.putBoolean(type.key, isLoggerTypeEnabled(type));
            }
            nbt.put("Loggers", loggers);
        }
        PacketSender.sendNbt(plugin, player, ServuxPaperPlugin.CHANNEL_HUD, PACKET_S2C_METADATA, nbt);
    }

    public void refreshSpawnMetadata(Player player) {
        if (!enabled || !hasPermission(player)) {
            return;
        }
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", ServuxPaperPlugin.CHANNEL_HUD);
        nbt.putString("servux", plugin.getServuxString());
        nbt.putInt("version", PROTOCOL_VERSION);
        if (spawnWorld != null) {
            nbt.putString("spawnDimension", spawnWorld.getKey().toString());
            nbt.putInt("spawnPosX", spawnX);
            nbt.putInt("spawnPosY", spawnY);
            nbt.putInt("spawnPosZ", spawnZ);
        }
        if (shareSeed && hasSeedPermission(player)) {
            nbt.putLong("worldSeed", worldSeed);
        }
        PacketSender.sendNbt(plugin, player, ServuxPaperPlugin.CHANNEL_HUD, PACKET_S2C_SPAWN_DATA, nbt);
    }

    private void sendWeather(Player player) {
        if (!shareWeatherStatus || !hasWeatherPermission(player)) {
            return;
        }
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", ServuxPaperPlugin.CHANNEL_HUD);
        nbt.putString("servux", plugin.getServuxString());
        if (isRaining && rainWeatherTime > -1) {
            nbt.putInt("SetRaining", rainWeatherTime);
            nbt.putBoolean("isRaining", true);
        } else {
            nbt.putBoolean("isRaining", false);
        }
        if (isThundering && thunderWeatherTime > -1) {
            nbt.putInt("SetThundering", thunderWeatherTime);
            nbt.putBoolean("isThundering", true);
        } else {
            nbt.putBoolean("isThundering", false);
        }
        if (clearWeatherTime > -1) {
            nbt.putInt("SetClear", clearWeatherTime);
        }
        PacketSender.sendNbt(plugin, player, ServuxPaperPlugin.CHANNEL_HUD, PACKET_S2C_WEATHER_TICK, nbt);
    }

    private void sendRecipeManager(Player player) {
        if (!hasPermission(player)) {
            return;
        }
        CompoundTag nbt = new CompoundTag();
        ListTag list = new ListTag();
        nbt.put("RecipeManager", list);
        byte[] data = NbtCodec.encodeNbt(nbt);
        PacketSender.sendSplitBuffer(plugin, player, ServuxPaperPlugin.CHANNEL_HUD, PACKET_S2C_NBT_RESPONSE_DATA, data);
    }

    private void updateSpawnFromServer() {
        World overworld = getOverworld();
        if (overworld == null) {
            return;
        }
        int x = overworld.getSpawnLocation().getBlockX();
        int y = overworld.getSpawnLocation().getBlockY();
        int z = overworld.getSpawnLocation().getBlockZ();
        if (spawnWorld == null || !spawnWorld.equals(overworld) || spawnX != x || spawnY != y || spawnZ != z) {
            spawnWorld = overworld;
            spawnX = x;
            spawnY = y;
            spawnZ = z;
            refreshSpawnMetadata = true;
        }
        if (shareSeed) {
            worldSeed = overworld.getSeed();
        } else {
            worldSeed = 0L;
        }
    }

    private void updateWeatherFromServer() {
        if (!shareWeatherStatus) {
            return;
        }
        World overworld = getOverworld();
        if (overworld == null) {
            return;
        }
        int newClear = overworld.getClearWeatherDuration();
        int newRain = overworld.getWeatherDuration();
        int newThunder = overworld.getThunderDuration();
        boolean newRaining = overworld.hasStorm();
        boolean newThundering = overworld.isThundering();
        if (newClear != clearWeatherTime || newRain != rainWeatherTime || newThunder != thunderWeatherTime || newRaining != isRaining || newThundering != isThundering) {
            clearWeatherTime = newClear;
            rainWeatherTime = newRain;
            thunderWeatherTime = newThunder;
            isRaining = newRaining;
            isThundering = newThundering;
            refreshWeatherData = true;
        }
    }

    private CompoundTag buildTpsData() {
        double[] tps = Bukkit.getServer().getTPS();
        double mspt = Bukkit.getServer().getAverageTickTime();
        double tpsValue = tps.length > 0 ? tps[0] : 20.0;
        var tickManager = Bukkit.getServer().getServerTickManager();
        boolean frozen = tickManager.isFrozen();
        boolean sprinting = tickManager.isSprinting();
        boolean stepping = tickManager.isStepping();
        long sprintTicks = 0L;
        CompoundTag tag = new CompoundTag();
        tag.putDouble("mspt", mspt);
        tag.putDouble("tps", tpsValue);
        tag.putLong("sprintTicks", sprintTicks);
        tag.putBoolean("frozen", frozen);
        tag.putBoolean("sprinting", sprinting);
        tag.putBoolean("stepping", stepping);
        return tag;
    }

    private CompoundTag buildMobCapsData() {
        try {
            MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
            CompoundTag root = new CompoundTag();
            for (ServerLevel level : server.getAllLevels()) {
                NaturalSpawner.SpawnState state = level.getChunkSource().getLastSpawnState();
                if (state == null) {
                    continue;
                }
                int spawnableChunks = state.getSpawnableChunkCount();
                if (spawnableChunks <= 0) {
                    continue;
                }
                Object2IntMap<MobCategory> counts = state.getMobCategoryCounts();
                ListTag caps = new ListTag();
                for (MobCategory category : LoggerType.MOB_CAPS.categories()) {
                    int current = counts.getInt(category);
                    int cap = category.getMaxInstancesPerChunk() * spawnableChunks / 289;
                    CompoundTag capTag = new CompoundTag();
                    capTag.putInt("current", current);
                    capTag.putInt("cap", cap);
                    caps.add(capTag);
                }
                CompoundTag entry = new CompoundTag();
                entry.putInt("cap_count", LoggerType.MOB_CAPS.categoryCount());
                entry.put("cap_data", caps);
                entry.putLong("WorldTick", level.getGameTime());
                root.put(level.dimension().identifier().toString(), entry);
            }
            return root;
        } catch (Throwable t) {
            return null;
        }
    }

    private boolean isLoggerTypeEnabled(LoggerType type) {
        return loggersEnabledList.contains(type.key);
    }

    private boolean hasPermission(Player player) {
        if (permissionLevel <= 0) {
            return true;
        }
        return player.hasPermission(permissionNode) || player.isOp();
    }

    private boolean hasWeatherPermission(Player player) {
        if (weatherPermissionLevel <= 0) {
            return true;
        }
        return player.hasPermission(permissionNode + ".weather") || player.isOp();
    }

    private boolean hasSeedPermission(Player player) {
        if (seedPermissionLevel <= 0) {
            return true;
        }
        return player.hasPermission(permissionNode + ".seed") || player.isOp();
    }

    private boolean hasLoggerPermission(Player player) {
        if (loggerPermissionLevel <= 0) {
            return true;
        }
        return player.hasPermission(permissionNode + ".logger") || player.isOp();
    }

    private World getOverworld() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL) {
                return world;
            }
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    private enum LoggerType {
        TPS("tps", new MobCategory[0]),
        MOB_CAPS("mob_caps", new MobCategory[] {
                MobCategory.MONSTER,
                MobCategory.CREATURE,
                MobCategory.AMBIENT,
                MobCategory.AXOLOTLS,
                MobCategory.UNDERGROUND_WATER_CREATURE,
                MobCategory.WATER_CREATURE,
                MobCategory.WATER_AMBIENT,
                MobCategory.MISC
        });

        private final String key;
        private final MobCategory[] categories;

        LoggerType(String key, MobCategory[] categories) {
            this.key = key;
            this.categories = categories;
        }

        static LoggerType fromKey(String key) {
            for (LoggerType type : values()) {
                if (type.key.equalsIgnoreCase(key)) {
                    return type;
                }
            }
            return null;
        }

        static int categoryCount() {
            return MOB_CAPS.categories.length;
        }

        MobCategory[] categories() {
            return categories;
        }
    }
}
