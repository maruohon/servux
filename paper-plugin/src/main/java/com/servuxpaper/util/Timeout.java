package com.servuxpaper.util;

public final class Timeout {
    private int lastSync;

    public Timeout(int currentTick) {
        this.lastSync = currentTick;
    }

    public boolean needsUpdate(int currentTick, int timeoutTicks) {
        return currentTick - lastSync >= timeoutTicks;
    }

    public void setLastSync(int tickCounter) {
        this.lastSync = tickCounter;
    }

    public int getLastSync() {
        return lastSync;
    }
}
