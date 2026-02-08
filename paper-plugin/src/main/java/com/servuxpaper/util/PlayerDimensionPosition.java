package com.servuxpaper.util;

import org.bukkit.World;
import org.bukkit.entity.Player;

public final class PlayerDimensionPosition {
    private World world;
    private int chunkX;
    private int chunkZ;

    public PlayerDimensionPosition(Player player) {
        setPosition(player);
    }

    public boolean dimensionChanged(Player player) {
        return world == null || !world.equals(player.getWorld());
    }

    public boolean chunkChanged(Player player) {
        int cx = player.getLocation().getBlockX() >> 4;
        int cz = player.getLocation().getBlockZ() >> 4;
        return cx != chunkX || cz != chunkZ;
    }

    public void setPosition(Player player) {
        this.world = player.getWorld();
        this.chunkX = player.getLocation().getBlockX() >> 4;
        this.chunkZ = player.getLocation().getBlockZ() >> 4;
    }
}
