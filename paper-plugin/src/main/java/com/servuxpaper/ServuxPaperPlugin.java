package com.servuxpaper;

import com.servuxpaper.hud.HudProvider;
import com.servuxpaper.structures.StructuresProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ServuxPaperPlugin extends JavaPlugin {
    public static final String CHANNEL_STRUCTURES = "servux:structures";
    public static final String CHANNEL_HUD = "servux:hud_metadata";

    private StructuresProvider structuresProvider;
    private HudProvider hudProvider;
    private int tickCounter;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.structuresProvider = new StructuresProvider(this);
        this.hudProvider = new HudProvider(this);

        var messenger = getServer().getMessenger();
        messenger.registerIncomingPluginChannel(this, CHANNEL_STRUCTURES, structuresProvider);
        messenger.registerOutgoingPluginChannel(this, CHANNEL_STRUCTURES);
        messenger.registerIncomingPluginChannel(this, CHANNEL_HUD, hudProvider);
        messenger.registerOutgoingPluginChannel(this, CHANNEL_HUD);

        var pm = getServer().getPluginManager();
        pm.registerEvents(structuresProvider, this);
        pm.registerEvents(hudProvider, this);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            tickCounter++;
            structuresProvider.tick(tickCounter);
            hudProvider.tick(tickCounter);
        }, 1L, 1L);
    }

    @Override
    public void onDisable() {
        var messenger = getServer().getMessenger();
        messenger.unregisterIncomingPluginChannel(this, CHANNEL_STRUCTURES, structuresProvider);
        messenger.unregisterOutgoingPluginChannel(this, CHANNEL_STRUCTURES);
        messenger.unregisterIncomingPluginChannel(this, CHANNEL_HUD, hudProvider);
        messenger.unregisterOutgoingPluginChannel(this, CHANNEL_HUD);
    }

    public StructuresProvider getStructuresProvider() {
        return structuresProvider;
    }

    public HudProvider getHudProvider() {
        return hudProvider;
    }

    public String getServuxString() {
        String version = getDescription().getVersion();
        String mc = Bukkit.getMinecraftVersion();
        return "servux-paper-" + mc + "-" + version;
    }
}
