package fi.dy.masa.servux.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.Identifier;

public interface IPluginChannelHandler
{
    Identifier getChannel();

    default void onPacketReceived(PacketByteBuf buf, ServerPlayNetworkHandler netHandler)
    {
    }

    default boolean isSubscribable()
    {
        return false;
    }

    default boolean subscribe(ServerPlayNetworkHandler netHandler)
    {
        return false;
    }

    default boolean unsubscribe(ServerPlayNetworkHandler netHandler)
    {
        return false;
    }
}
