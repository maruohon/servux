package fi.dy.masa.servux.network;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public interface ClientPacketHandler
{
    ResourceLocation getChannel();

    boolean canSubscribe();

    boolean usePacketSplitter();

    default void onPacketReceived(PacketBuffer buf, NetHandlerPlayServer netHandler)
    {
    }

    default void subscribe(NetHandlerPlayServer netHandler)
    {
    }

    default void unsubscribe(NetHandlerPlayServer netHandler)
    {
    }
}
