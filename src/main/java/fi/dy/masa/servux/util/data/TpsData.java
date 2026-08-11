package fi.dy.masa.servux.util.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;

public class TpsData
{
    private float serverMspt;
    private float serverTps;

    public float getServerMspt()
    {
        return this.serverMspt;
    }

    public float getServerTps()
    {
        return this.serverTps;
    }

    public void update(MinecraftServer server)
    {
        double mspt = MathHelper.average(server.tickTimeArray) / 1000000D;
        this.serverMspt = (float) mspt;
        this.serverTps = (float) (mspt <= 50 ? 20D : (1000D / mspt));
    }
}
