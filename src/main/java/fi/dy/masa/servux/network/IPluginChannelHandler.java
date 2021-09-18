package fi.dy.masa.servux.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.ResourceLocation;

public interface IPluginChannelHandler
{
    ResourceLocation getChannel();

    default void onPacketReceived(PacketBuffer buf, ServerPlayNetHandler netHandler)
    {
    }

    default boolean isSubscribable()
    {
        return false;
    }

    default boolean subscribe(ServerPlayNetHandler netHandler)
    {
        return false;
    }

    default boolean unsubscribe(ServerPlayNetHandler netHandler)
    {
        return false;
    }
}
