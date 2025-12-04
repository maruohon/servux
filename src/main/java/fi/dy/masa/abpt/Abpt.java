package fi.dy.masa.abpt;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;

@Mod(Reference.MOD_ID)
public class Abpt
{
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    public static boolean accuratePlacementProtocolV3 = false;

    public Abpt()
    {
        accuratePlacementProtocolV3 = ! Files.exists(Path.of("config/abpt.json"));
        LOGGER.info("Abpt v{} loaded, using protocol {}", Reference.MOD_VERSION, accuratePlacementProtocolV3 ? "v3": "v2");
    }
}
