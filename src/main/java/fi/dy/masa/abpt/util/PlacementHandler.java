package fi.dy.masa.abpt.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableSet;
import org.joml.Vector3d;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.abpt.Abpt;

public class PlacementHandler
{
    public static final ImmutableSet<Property<?>> WHITELISTED_PROPERTIES = ImmutableSet.of(
            // BooleanProperty:
            // INVERTED - DaylightDetector
            // OPEN - Barrel, Door, FenceGate, Trapdoor
            // PERSISTENT - Leaves
            BlockStateProperties.INVERTED,
            BlockStateProperties.OPEN,
            BlockStateProperties.PERSISTENT,
            // EnumProperty:
            // AXIS - Pillar
            // BLOCK_HALF - Stairs, Trapdoor
            // CHEST_TYPE - Chest
            // COMPARATOR_MODE - Comparator
            // DOOR_HINGE - Door
            // SLAB_TYPE - Slab - PARTIAL ONLY: TOP and BOTTOM, not DOUBLE
            // STAIR_SHAPE - Stairs (needed to get the correct state, otherwise the player facing would be a factor)
            // FACE - Button, Grindstone, Lever
            BlockStateProperties.AXIS,
            BlockStateProperties.HALF,
            BlockStateProperties.CHEST_TYPE,
            BlockStateProperties.MODE_COMPARATOR,
            BlockStateProperties.DOOR_HINGE,
            BlockStateProperties.SLAB_TYPE,
            BlockStateProperties.STAIRS_SHAPE,
            BlockStateProperties.ATTACH_FACE,
            // IntProperty:
            // BITES - Cake
            // DELAY - Repeater
            // NOTE - NoteBlock
            // ROTATION - Banner, Sign, Skull
            BlockStateProperties.BITES,
            BlockStateProperties.DELAY,
            BlockStateProperties.NOTE,
            BlockStateProperties.ROTATION_16
    );

    @Nullable
    public static BlockState applyPlacementProtocolToPlacementState(BlockState state, UseContext context)
    {
        if (Abpt.accuratePlacementProtocolV3)
        {
            return applyPlacementProtocolV3(state, context);
        }
        else
        {
            return applyPlacementProtocolV2(state, context);
        }
    }

    public static BlockState applyPlacementProtocolV2(BlockState state, UseContext context)
    {
        int protocolValue = (int) (context.getHitVec().x - (double) context.getPos().getX()) - 2;

        if (protocolValue < 0)
        {
            return state;
        }

        @Nullable DirectionProperty property = getFirstDirectionProperty(state);

        if (property != null)
        {
            state = applyDirectionProperty(state, context, property, protocolValue);

            if (state == null)
            {
                return null;
            }
        }

        protocolValue &= 0xFFFFFFF0;

        if (protocolValue >= 16)
        {
            Block block = state.getBlock();

            if (block instanceof RepeaterBlock)
            {
                Integer delay = (protocolValue / 16) + 1;

                if (RepeaterBlock.DELAY.getPossibleValues().contains(delay))
                {
                    state = state.setValue(RepeaterBlock.DELAY, delay);
                }
            }
            else if (block instanceof ComparatorBlock)
            {
                state = state.setValue(ComparatorBlock.MODE, ComparatorMode.SUBTRACT);
            }
            else if (block instanceof TrapDoorBlock)
            {
                state = state.setValue(TrapDoorBlock.HALF, Half.TOP);
            }
            else if (block instanceof StairBlock && state.getValue(StairBlock.HALF) == Half.BOTTOM)
            {
                state = state.setValue(StairBlock.HALF, Half.TOP);
            }
        }

        return state;
    }

    public static <T extends Comparable<T>> BlockState applyPlacementProtocolV3(BlockState state, UseContext context)
    {
        int protocolValue = (int) (context.getHitVec().x - (double) context.getPos().getX()) - 2;
        //System.out.printf("raw protocol value in: 0x%08X\n", protocolValue);

        if (protocolValue < 0)
        {
            return state;
        }

        @Nullable DirectionProperty property = getFirstDirectionProperty(state);

        // DirectionProperty - allow all except: VERTICAL_DIRECTION (PointedDripstone)
        if (property != null)
        {
            //System.out.printf("applying: 0x%08X\n", protocolValue);
            state = applyDirectionProperty(state, context, property, protocolValue);

            if (state == null)
            {
                return null;
            }

            // Consume the bits used for the facing
            protocolValue >>>= 3;
        }

        // Consume the lowest unused bit
        protocolValue >>>= 1;

        List<Property<?>> propList = new ArrayList<>(state.getBlock().getStateDefinition().getProperties());
        propList.sort(Comparator.comparing(Property::getName));

        try
        {
            for (Property<?> p : propList)
            {
                if ((p instanceof DirectionProperty) == false &&
                    WHITELISTED_PROPERTIES.contains(p))
                {
                    @SuppressWarnings("unchecked")
                    Property<T> prop = (Property<T>) p;
                    List<T> list = new ArrayList<>(prop.getPossibleValues());
                    list.sort(Comparable::compareTo);

                    int requiredBits = Mth.log2(Mth.smallestEncompassingPowerOfTwo(list.size()));
                    int bitMask = ~(0xFFFFFFFF << requiredBits);
                    int valueIndex = protocolValue & bitMask;
                    //System.out.printf("trying to apply valInd: %d, bits: %d, prot val: 0x%08X\n", valueIndex, requiredBits, protocolValue);

                    if (valueIndex >= 0 && valueIndex < list.size())
                    {
                        T value = list.get(valueIndex);

                        if (state.getValue(prop).equals(value) == false &&
                            value != SlabType.DOUBLE) // don't allow duping slabs by forcing a double slab via the protocol
                        {
                            //System.out.printf("applying %s: %s\n", prop.getName(), value);
                            state = state.setValue(prop, value);
                        }

                        protocolValue >>>= requiredBits;
                    }
                }
            }
        }
        catch (Exception e)
        {
            Abpt.LOGGER.warn("Exception trying to apply placement protocol value", e);
        }

        return state;
    }

    private static BlockState applyDirectionProperty(BlockState state, UseContext context,
                                                     DirectionProperty property, int protocolValue)
    {
        Direction facingOrig = state.getValue(property);
        Direction facing = facingOrig;
        int decodedFacingIndex = (protocolValue & 0xF) >> 1;

        if (decodedFacingIndex == 6) // the opposite of the normal facing requested
        {
            facing = facing.getOpposite();
        }
        else if (decodedFacingIndex >= 0 && decodedFacingIndex <= 5)
        {
            facing = Direction.from3DDataValue(decodedFacingIndex);

            if (property.getPossibleValues().contains(facing) == false)
            {
                facing = context.getEntity().getDirection().getOpposite();
            }
        }

        //System.out.printf("plop facing: %s -> %s (raw: %d, dec: %d)\n", facingOrig, facing, rawFacingIndex, decodedFacingIndex);

        if (facing != facingOrig && property.getPossibleValues().contains(facing))
        {
            if (state.getBlock() instanceof BedBlock)
            {
                BlockPos headPos = context.pos.relative(facing);
                BlockPlaceContext ctx = context.getItemPlacementContext();

                if (context.getWorld().getBlockState(headPos).canBeReplaced(ctx) == false)
                {
                    return null;
                }
            }

            state = state.setValue(property, facing);
        }

        return state;
    }

    /**
     * Returns the first PropertyDirection property from the provided state, if any.
     * @param state
     * @return the first PropertyDirection, or null if there are no such properties
     */
    @Nullable
    public static DirectionProperty getFirstDirectionProperty(BlockState state)
    {
        for (Property<?> prop : state.getProperties())
        {
            if (prop instanceof DirectionProperty)
            {
                return (DirectionProperty) prop;
            }
        }

        return null;
    }

    public static class UseContext
    {
        private final Level world;
        private final BlockPos pos;
        private final Direction side;
        private final Vector3d hitVec;
        private final LivingEntity entity;
        private final InteractionHand hand;
        @Nullable private final BlockPlaceContext itemPlacementContext;

        private UseContext(Level world, BlockPos pos, Direction side, Vector3d hitVec,
                           LivingEntity entity, InteractionHand hand, @Nullable BlockPlaceContext itemPlacementContext)
        {
            this.world = world;
            this.pos = pos;
            this.side = side;
            this.hitVec = hitVec;
            this.entity = entity;
            this.hand = hand;
            this.itemPlacementContext = itemPlacementContext;
        }

        /*
        public static UseContext of(World world, BlockPos pos, Direction side, Vec3d hitVec, LivingEntity entity, Hand hand)
        {
            return new UseContext(world, pos, side, hitVec, entity, hand, null);
        }
        */

        public static UseContext from(BlockPlaceContext ctx, InteractionHand hand)
        {
            Vec3 pos = ctx.getClickLocation();
            return new UseContext(ctx.getLevel(), ctx.getClickedPos(), ctx.getClickedFace(), new Vector3d(pos.x, pos.y, pos.z),
                                  ctx.getPlayer(), hand, ctx);
        }

        public Level getWorld()
        {
            return this.world;
        }

        public BlockPos getPos()
        {
            return this.pos;
        }

        public Direction getSide()
        {
            return this.side;
        }

        public Vector3d getHitVec()
        {
            return this.hitVec;
        }

        public LivingEntity getEntity()
        {
            return this.entity;
        }

        public InteractionHand getHand()
        {
            return this.hand;
        }

        @Nullable
        public BlockPlaceContext getItemPlacementContext()
        {
            return this.itemPlacementContext;
        }
    }
}
