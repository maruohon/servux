package fi.dy.masa.servux.dataprovider;

import net.minecraft.server.MinecraftServer;
import fi.dy.masa.servux.network.ClientPacketHandler;

public interface DataProvider
{
    /**
     * @return the simple name for this data provider.
     * This should preferably be a lower case alphanumeric string with no
     * other special characters than '-' and '_'.
     * This name will be used in the enable/disable commands as the argument
     * and also as the config file key/identifier.
     */
    String getName();

    /**
     * @return the description of this data provider. Used in the command to list
     *         the available providers and to check the status of a given provider.
     */
    String getDescription();

    /**
     * @return the current protocol version this provider supports
     */
    int getProtocolVersion();

    /**
     * @return whether this data provider is currently enabled.
     */
    boolean isEnabled();

    /**
     * Enables or disables this data provider
     */
    void setEnabled(boolean enabled);

    /**
     * @return whether or not this data provider should get ticked to periodically send some data,
     * or if it's only listening for incoming requests and responds to them directly.
     */
    default boolean shouldTick()
    {
        return false;
    }

    /**
     * @return the interval in game ticks that this data provider should be ticked at
     */
    int getTickRate();

    /**
     * Called at the given tick rate
     * @param tickCounter The current server tick (since last server start, ie. not a world tick)
     */
    default void tick(MinecraftServer server, int tickCounter)
    {
    }

    /**
     * @return the network packet handler used for this data provider.
     */
    ClientPacketHandler getPacketHandler();
}
