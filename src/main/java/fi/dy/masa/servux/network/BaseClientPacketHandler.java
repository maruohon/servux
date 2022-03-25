package fi.dy.masa.servux.network;

public abstract class BaseClientPacketHandler implements ClientPacketHandler
{
    protected boolean canSubscribe;
    protected boolean usePacketSplitter;

    @Override
    public boolean canSubscribe()
    {
        return this.canSubscribe;
    }

    @Override
    public boolean usePacketSplitter()
    {
        return this.usePacketSplitter;
    }
}
