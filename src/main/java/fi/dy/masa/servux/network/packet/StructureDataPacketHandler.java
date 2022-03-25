package fi.dy.masa.servux.network.packet;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.Identifier;
import fi.dy.masa.servux.dataprovider.StructureDataProvider;
import fi.dy.masa.servux.network.BaseClientPacketHandler;

public class StructureDataPacketHandler extends BaseClientPacketHandler
{
    public static final Identifier CHANNEL = new Identifier("servux:structure");
    public static final StructureDataPacketHandler INSTANCE = new StructureDataPacketHandler();

    public static final int PROTOCOL_VERSION = 2;
    public static final int PACKET_S2C_METADATA = 1;
    public static final int PACKET_S2C_STRUCTURE_DATA = 2;

    protected StructureDataPacketHandler()
    {
        this.canSubscribe = true;
        this.usePacketSplitter = true;
    }

    @Override
    public Identifier getChannel()
    {
        return CHANNEL;
    }

    @Override
    public void subscribe(ServerPlayNetworkHandler netHandler)
    {
        StructureDataProvider.INSTANCE.register(netHandler.player);
    }

    @Override
    public void unsubscribe(ServerPlayNetworkHandler netHandler)
    {
        StructureDataProvider.INSTANCE.unregister(netHandler.player);
    }
}
