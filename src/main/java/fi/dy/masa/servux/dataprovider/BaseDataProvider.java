package fi.dy.masa.servux.dataprovider;

public abstract class BaseDataProvider implements DataProvider
{
    protected final String name;
    protected final String description;
    protected final int protocolVersion;
    protected boolean enabled;
    protected boolean shouldTick;
    private int tickRate = 40;

    protected BaseDataProvider(String name, int protocolVersion, String description)
    {
        this.name = name;
        this.protocolVersion = protocolVersion;
        this.description = description;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    @Override
    public int getProtocolVersion()
    {
        return this.protocolVersion;
    }

    @Override
    public boolean isEnabled()
    {
        return this.enabled;
    }

    @Override
    public boolean shouldTick()
    {
        return this.shouldTick;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    protected void setTickRate(int tickRate)
    {
        this.tickRate = Math.max(tickRate, 1);
    }

    @Override
    public final int getTickRate()
    {
        return this.tickRate;
    }
}
