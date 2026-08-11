package fi.dy.masa.servux.util;

import java.util.List;

import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

public class NbtUtils
{
    public static NBTTagList asListTag(List<String> strings)
    {
        NBTTagList list = new NBTTagList();

        for (String str : strings)
        {
            list.appendTag(new NBTTagString(str));
        }

        return list;
    }
}
