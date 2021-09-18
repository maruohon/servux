package fi.dy.masa.servux.network.packet;

import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.ResourceLocation;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;
import fi.dy.masa.servux.network.IPluginChannelHandler;

public class StructureDataPacketHandler implements IPluginChannelHandler
{
    public static final ResourceLocation CHANNEL = new ResourceLocation("servux:structures");
    public static final StructureDataPacketHandler INSTANCE = new StructureDataPacketHandler();

    public static final int PROTOCOL_VERSION = 1;
    public static final int PACKET_S2C_METADATA = 1;
    public static final int PACKET_S2C_STRUCTURE_DATA = 2;

    @Override
    public ResourceLocation getChannel()
    {
        return CHANNEL;
    }

    @Override
    public boolean isSubscribable()
    {
        return true;
    }

    @Override
    public boolean subscribe(ServerPlayNetHandler netHandler)
    {
        return StructureDataProvider.INSTANCE.register(netHandler.player);
    }

    @Override
    public boolean unsubscribe(ServerPlayNetHandler netHandler)
    {
        return StructureDataProvider.INSTANCE.unregister(netHandler.player);
    }
}
