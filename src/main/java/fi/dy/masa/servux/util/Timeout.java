package fi.dy.masa.servux.util;

public class Timeout
{
    protected int lastSync;

    public Timeout(int currentTick)
    {
        this.lastSync = currentTick;
    }

    public boolean needsUpdate(int currentTick, int timeout)
    {
        return currentTick - this.lastSync >= timeout;
    }

    public void setLastSync(int tickCounter)
    {
        this.lastSync = tickCounter;
    }
}
