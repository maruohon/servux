package fi.dy.masa.servux.util;

import java.util.List;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

public class NbtUtils
{
    public static ListTag asListTag(List<String> strings)
    {
        ListTag list = new ListTag();

        for (String str : strings)
        {
            list.add(new StringTag(str));
        }

        return list;
    }
}
