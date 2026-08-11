package fi.dy.masa.servux.util;

import java.util.Optional;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;

public class BlockUtils
{
    /**
     * Returns the first PropertyDirection property from the provided state, if any.
     * @return the first PropertyDirection, or empty() if there are no such properties
     */
    public static Optional<PropertyDirection> getFirstDirectionProperty(IBlockState state)
    {
        for (IProperty<?> prop : state.getProperties().keySet())
        {
            if (prop instanceof PropertyDirection)
            {
                return Optional.of((PropertyDirection) prop);
            }
        }

        return Optional.empty();
    }
}
