package fi.dy.masa.servux.util.data;

public class LimitedValue
{
    protected int value;
    protected int limit;

    public int getValue()
    {
        return this.value;
    }

    public int getLimit()
    {
        return this.limit;
    }

    public void setValue(int value)
    {
        this.value = value;
    }

    public void setLimit(int limit)
    {
        this.limit = limit;
    }

    public void setBoth(int value, int limit)
    {
        this.value = value;
        this.limit = limit;
    }
}
