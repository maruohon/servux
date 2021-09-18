package fi.dy.masa.servux;

import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import fi.dy.masa.servux.dataproviders.DataProviderManager;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;

@Mod(Reference.MOD_ID)
public class Servux
{
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    public Servux()
    {
        // Make sure the mod being absent on the other network side does not cause
        // the client to display the server as incompatible
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                                                       () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY,
                                                                     (incoming, isNetwork) -> true));

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event)
    {
        DataProviderManager.INSTANCE.registerDataProvider(StructureDataProvider.INSTANCE);
        DataProviderManager.INSTANCE.readFromConfig();
    }

    public static String getModVersionString(String modId)
    {
        Optional<? extends ModContainer> modContainer = ModList.get().getModContainerById(modId);
        return modContainer.isPresent() ? modContainer.get().getModInfo().getVersion().toString() : "?";
    }
}
