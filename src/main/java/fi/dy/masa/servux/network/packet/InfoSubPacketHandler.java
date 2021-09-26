package fi.dy.masa.servux.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;
import fi.dy.masa.servux.dataprovider.InfoSubDataProvider;
import fi.dy.masa.servux.network.ClientPacketHandler;
import fi.dy.masa.servux.util.data.Constants;

public class InfoSubPacketHandler implements ClientPacketHandler
{
    public static final Identifier REG_CHANNEL = new Identifier("servux:info_reg");
    public static final Identifier DATA_CHANNEL = new Identifier("servux:info_data");

    public static final InfoSubPacketHandler INSTANCE = new InfoSubPacketHandler();

    public static final int PROTOCOL_VERSION = 1;
    public static final int PACKET_S2C_METADATA = 1;

    @Override
    public Identifier getChannel()
    {
        return REG_CHANNEL;
    }

    @Override
    public void onPacketReceived(PacketByteBuf buf, ServerPlayNetworkHandler netHandler)
    {
        CompoundTag tag = buf.readCompoundTag();
        String action = tag.getString("action");

        if (action.equals("unregister_all"))
        {
            InfoSubDataProvider.INSTANCE.unregisterAllChannels(netHandler.player.getUuid());
        }
        else
        {
            boolean register = action.equals("register");
            boolean unregister = action.equals("unregister");

            if (register || unregister)
            {
                ListTag list = tag.getList("channels", Constants.NBT.TAG_COMPOUND);
                final int size = list.size();

                for (int i = 0; i < size; ++i)
                {
                    CompoundTag channelTag = list.getCompound(i);
                    String channel = channelTag.getString("channel");

                    if (register)
                    {
                        //CompoundTag data = channelTag.getCompound("data");
                        InfoSubDataProvider.INSTANCE.register(channel, netHandler);
                    }
                    else
                    {
                        InfoSubDataProvider.INSTANCE.unregister(channel, netHandler);
                    }
                }

                if (register)
                {
                    InfoSubDataProvider.INSTANCE.sendMetadata(netHandler);
                }
            }
        }
    }
}
