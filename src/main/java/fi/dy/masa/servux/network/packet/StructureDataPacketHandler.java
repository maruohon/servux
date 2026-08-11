package fi.dy.masa.servux.network.packet;

import fi.dy.masa.servux.dataprovider.StructureDataProvider;
import fi.dy.masa.servux.network.BaseClientPacketHandler;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.ResourceLocation;

public class StructureDataPacketHandler extends BaseClientPacketHandler
{
    public static final ResourceLocation CHANNEL = new ResourceLocation("servux:structure");
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
    public ResourceLocation getChannel()
    {
        return CHANNEL;
    }

    @Override
    public void subscribe(NetHandlerPlayServer netHandler)
    {
        StructureDataProvider.INSTANCE.register(netHandler.player);
    }

    @Override
    public void unsubscribe(NetHandlerPlayServer netHandler)
    {
        StructureDataProvider.INSTANCE.unregister(netHandler.player);
    }
}
