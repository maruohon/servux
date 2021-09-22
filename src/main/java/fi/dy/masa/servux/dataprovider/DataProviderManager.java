package fi.dy.masa.servux.dataprovider;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.server.MinecraftServer;
import fi.dy.masa.servux.network.ClientPacketHandler;
import fi.dy.masa.servux.network.PacketChannelHandler;
import fi.dy.masa.servux.util.JsonUtils;

public class DataProviderManager
{
    public static final DataProviderManager INSTANCE = new DataProviderManager();

    protected final HashMap<String, DataProvider> providers = new HashMap<>();
    protected final List<DataProvider> providersTicking = new ArrayList<>();
    protected ImmutableList<DataProvider> providersImmutable = ImmutableList.of();

    public ImmutableList<DataProvider> getAllProviders()
    {
        return this.providersImmutable;
    }

    /**
     * Registers the given data provider, if it's not already registered
     * @return true if the provider did not exist yet and was successfully registered
     */
    public boolean registerDataProvider(DataProvider provider)
    {
        String name = provider.getName();

        if (this.providers.containsKey(name) == false)
        {
            this.providers.put(name, provider);
            this.providersImmutable = ImmutableList.copyOf(this.providers.values());
            //System.out.printf("registerDataProvider: %s\n", provider);
            return true;
        }

        return false;
    }

    public boolean setProviderEnabled(String providerName, boolean enabled)
    {
        DataProvider provider = this.providers.get(providerName);
        return provider != null && this.setProviderEnabled(provider, enabled);
    }

    public boolean setProviderEnabled(DataProvider provider, boolean enabled)
    {
        boolean wasEnabled = provider.isEnabled();
        enabled = true; // FIXME TODO remove debug

        if (enabled || wasEnabled != enabled)
        {
            //System.out.printf("setProviderEnabled: %s (%s)\n", enabled, provider);
            provider.setEnabled(enabled);
            this.updatePacketHandlerRegistration(provider);

            if (enabled && provider.shouldTick())
            {
                if (this.providersTicking.contains(provider) == false)
                {
                    this.providersTicking.add(provider);
                }
            }
            else
            {
                this.providersTicking.remove(provider);
            }

            return true;
        }

        return false;
    }

    public void tickProviders(MinecraftServer server, int tickCounter)
    {
        if (this.providersTicking.isEmpty() == false)
        {
            for (DataProvider provider : this.providersTicking)
            {
                if ((tickCounter % provider.getTickRate()) == 0)
                {
                    provider.tick(server, tickCounter);
                }
            }
        }
    }

    protected void registerEnabledPacketHandlers()
    {
        for (DataProvider provider : this.providersImmutable)
        {
            this.updatePacketHandlerRegistration(provider);
        }
    }

    protected void updatePacketHandlerRegistration(DataProvider provider)
    {
        ClientPacketHandler handler = provider.getPacketHandler();

        if (provider.isEnabled())
        {
            PacketChannelHandler.INSTANCE.registerServerChannelHandler(handler);
        }
        else
        {
            PacketChannelHandler.INSTANCE.unregisterServerChannelHandler(handler);
        }
    }

    public void readFromConfig()
    {
        JsonElement el = JsonUtils.parseJsonFile(this.getConfigFile());
        JsonObject obj = null;

        if (el != null && el.isJsonObject())
        {
            JsonObject root = el.getAsJsonObject();

            if (JsonUtils.hasObject(root, "DataProviderToggles"))
            {
                obj = JsonUtils.getNestedObject(root, "DataProviderToggles", false);
            }
        }

        for (DataProvider provider : this.providersImmutable)
        {
            String name = provider.getName();
            boolean enabled = obj != null && JsonUtils.getBooleanOrDefault(obj, name, false);
            this.setProviderEnabled(provider, enabled);
        }
    }

    public void writeToConfig()
    {
        JsonObject root = new JsonObject();
        JsonObject objToggles = new JsonObject();

        for (DataProvider provider : this.providersImmutable)
        {
            String name = provider.getName();
            objToggles.add(name, new JsonPrimitive(provider.isEnabled()));
        }

        root.add("DataProviderToggles", objToggles);

        JsonUtils.writeJsonToFile(root, this.getConfigFile());
    }

    protected File getConfigFile()
    {
        return new File("servux.json");
    }
}
