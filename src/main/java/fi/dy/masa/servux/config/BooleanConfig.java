package fi.dy.masa.servux.config;

public class BooleanConfig extends BaseConfig
{
    protected boolean booleanValue;

    public BooleanConfig(String name, boolean defaultValue)
    {
        super(name);

        this.booleanValue = defaultValue;
    }

    public boolean getBooleanValue()
    {
        return this.booleanValue;
    }

    public boolean setBooleanValue(boolean newValue)
    {
        boolean changed = this.booleanValue != newValue;
        this.booleanValue = newValue;
        return changed;
    }

    public void toggleBooleanValue()
    {
        this.setBooleanValue(! this.booleanValue);
    }
}
