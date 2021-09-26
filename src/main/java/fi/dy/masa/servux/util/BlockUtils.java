package fi.dy.masa.servux.util;

import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Property;

public class BlockUtils
{
    /**
     * Returns the first PropertyDirection property from the provided state, if any.
     * @return the first PropertyDirection, or empty() if there are no such properties
     */
    public static Optional<DirectionProperty> getFirstDirectionProperty(BlockState state)
    {
        for (Property<?> prop : state.getEntries().keySet())
        {
            if (prop instanceof DirectionProperty)
            {
                return Optional.of((DirectionProperty) prop);
            }
        }

        return Optional.empty();
    }
}
