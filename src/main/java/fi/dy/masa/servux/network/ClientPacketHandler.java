package fi.dy.masa.servux.network;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

public interface ClientPacketHandler
{
    Identifier getChannel();

    default boolean isSubscribable()
    {
        return true;
    }

    default void onPacketReceived(PacketByteBuf buf, ServerPlayNetworkHandler netHandler)
    {
    }

    default void subscribe(ServerPlayNetworkHandler netHandler)
    {
    }

    default void unsubscribe(ServerPlayNetworkHandler netHandler)
    {
    }
}
