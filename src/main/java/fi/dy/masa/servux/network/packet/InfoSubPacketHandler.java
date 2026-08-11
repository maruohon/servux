package fi.dy.masa.servux.network.packet;

import fi.dy.masa.servux.dataprovider.InfoSubDataProvider;
import fi.dy.masa.servux.network.BaseClientPacketHandler;
import fi.dy.masa.servux.util.data.Constants;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class InfoSubPacketHandler extends BaseClientPacketHandler
{
    public static final ResourceLocation REG_CHANNEL = new ResourceLocation("servux:info_reg");
    public static final ResourceLocation DATA_CHANNEL = new ResourceLocation("servux:info_data");

    public static final InfoSubPacketHandler INSTANCE = new InfoSubPacketHandler();

    public static final int PROTOCOL_VERSION = 1;
    public static final int PACKET_S2C_METADATA = 1;

    protected InfoSubPacketHandler()
    {
    }

    @Override
    public ResourceLocation getChannel()
    {
        return REG_CHANNEL;
    }

    @Override
    public void onPacketReceived(PacketBuffer buf, NetHandlerPlayServer netHandler)
    {
        NBTTagCompound tag = buf.readCompoundTag();
        String action = tag.getString("action");

        if (action.equals("unregister_all"))
        {
            InfoSubDataProvider.INSTANCE.unregisterAllChannels(netHandler.player.getUniqueID());
        }
        else
        {
            boolean register = action.equals("register");
            boolean unregister = action.equals("unregister");

            if (register || unregister)
            {
                NBTTagList list = tag.getTagList("channels", Constants.NBT.TAG_COMPOUND);
                final int size = list.tagCount();

                for (int i = 0; i < size; ++i)
                {
                    NBTTagCompound channelTag = list.getCompoundTagAt(i);
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
