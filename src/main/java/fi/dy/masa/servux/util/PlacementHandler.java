package fi.dy.masa.servux.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CakeBlock;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.config.Configs;

public class PlacementHandler
{
    public static final ImmutableSet<Property<?>> WHITELISTED_PROPERTIES = ImmutableSet.of(
            // BooleanProperty:
            // INVERTED - DaylightDetector
            // OPEN - Barrel, Door, FenceGate, Trapdoor
            // PERSISTENT - Leaves
            DoorBlock.OPEN,
            FenceGateBlock.OPEN,
            TrapdoorBlock.OPEN,
            LeavesBlock.DECAYABLE,
            LeavesBlock.CHECK_DECAY,
            // EnumProperty:
            // AXIS - Pillar
            // BLOCK_HALF - Stairs, Trapdoor
            // CHEST_TYPE - Chest
            // COMPARATOR_MODE - Comparator
            // DOOR_HINGE - Door
            // SLAB_TYPE - Slab - PARTIAL ONLY: TOP and BOTTOM, not DOUBLE
            // STAIR_SHAPE - Stairs (needed to get the correct state, otherwise the player facing would be a factor)
            // WALL_MOUNT_LOCATION - Button, Grindstone, Lever
            PillarBlock.AXIS,
            StairsBlock.HALF,
            TrapdoorBlock.field_24932,
            StairsBlock.SHAPE,
            ComparatorBlock.MODE,
            DoorBlock.HINGE,
            SlabBlock.HALF,
            LeverBlock.FACING,
            // IntProperty:
            // BITES - Cake
            // DELAY - Repeater
            // NOTE - NoteBlock
            // ROTATION - Banner, Sign, Skull
            CakeBlock.BITES,
            RepeaterBlock.DELAY,
            BannerBlock.FACING,
            BannerBlock.ROTATION,
            SignBlock.ROTATION,
            WallSignBlock.FACING,
            SkullBlock.FACING
    );

    @Nullable
    public static BlockState applyPlacementProtocolToPlacementState(BlockState state, UseContext context)
    {
        if (Configs.Generic.PLACEMENT_PROTOCOL_V3.getBooleanValue())
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
        int protocolValue = (int) context.getHitVec().x - 2;

        //System.out.printf("V2 raw protocolValue: 0x%08X, bx: %d, hx: %.3f\n", protocolValue, context.getPos().getX(), context.getHitVec().x);
        if (protocolValue < 0)
        {
            return state;
        }

        Optional<DirectionProperty> propertyOpt = BlockUtils.getFirstDirectionProperty(state);

        if (propertyOpt.isPresent())
        {
            state = applyDirectionProperty(state, context, propertyOpt.get(), protocolValue);

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

                if (RepeaterBlock.DELAY.getValues().contains(delay))
                {
                    state = state.with(RepeaterBlock.DELAY, delay);
                }
            }
            else if (block instanceof ComparatorBlock)
            {
                state = state.with(ComparatorBlock.MODE, ComparatorBlock.ComparatorMode.SUBTRACT);
            }
            else if (block instanceof TrapdoorBlock)
            {
                state = state.with(TrapdoorBlock.field_24932, TrapdoorBlock.Side.TOP);
            }
            else if (block instanceof StairsBlock && state.get(StairsBlock.HALF) == StairsBlock.BlockHalf.BOTTOM)
            {
                state = state.with(StairsBlock.HALF, StairsBlock.BlockHalf.TOP);
            }
        }

        return state;
    }

    public static <T extends Comparable<T>> BlockState applyPlacementProtocolV3(BlockState state, UseContext context)
    {
        int protocolValue = (int) context.getHitVec().x - 2;
        //System.out.printf("V3 raw protocolValue: 0x%08X, bx: %d, hx: %.3f\n", protocolValue, context.getPos().getX(), context.getHitVec().x);

        if (protocolValue < 0)
        {
            return state;
        }

        Optional<DirectionProperty> propertyOpt = BlockUtils.getFirstDirectionProperty(state);

        // DirectionProperty - allow all except: VERTICAL_DIRECTION (PointedDripstone)
        if (propertyOpt.isPresent())// && property != Properties.VERTICAL_DIRECTION)
        {
            //System.out.printf("applying: 0x%08X\n", protocolValue);
            state = applyDirectionProperty(state, context, propertyOpt.get(), protocolValue);

            if (state == null)
            {
                return null;
            }

            // Consume the bits used for the facing
            protocolValue >>>= 3;
        }

        // Consume the lowest unused bit
        protocolValue >>>= 1;

        List<Property<?>> propList = new ArrayList<>(state.getBlock().getStateManager().getProperties());
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
                    List<T> list = new ArrayList<>(prop.getValues());
                    list.sort(Comparable::compareTo);

                    int requiredBits = MathHelper.log2(MathHelper.smallestEncompassingPowerOfTwo(list.size()));
                    int bitMask = ~(0xFFFFFFFF << requiredBits);
                    int valueIndex = protocolValue & bitMask;
                    //System.out.printf("trying to apply valInd: %d, bits: %d, prot val: 0x%08X\n", valueIndex, requiredBits, protocolValue);

                    if (valueIndex >= 0 && valueIndex < list.size())
                    {
                        T value = list.get(valueIndex);

                        if (state.get(prop).equals(value) == false)
                        {
                            //System.out.printf("applying %s: %s\n", prop.getName(), value);
                            state = state.with(prop, value);
                        }

                        protocolValue >>>= requiredBits;
                    }
                }
            }
        }
        catch (Exception e)
        {
            Servux.LOGGER.warn("Exception trying to apply placement protocol value", e);
        }

        return state;
    }

    private static BlockState applyDirectionProperty(BlockState state, UseContext context,
                                                     DirectionProperty property, int protocolValue)
    {
        Direction facingOrig = state.get(property);
        Direction facing = facingOrig;
        int decodedFacingIndex = (protocolValue & 0x7);

        if (decodedFacingIndex == 6) // the opposite of the normal facing requested
        {
            facing = facing.getOpposite();
        }
        else if (decodedFacingIndex >= 0 && decodedFacingIndex <= 5)
        {
            facing = Direction.byId(decodedFacingIndex);

            if (property.getValues().contains(facing) == false)
            {
                facing = context.getEntity().getHorizontalFacing().getOpposite();
            }
        }

        //System.out.printf("plop facing: %s -> %s (prot: %d, dec: %d)\n", facingOrig, facing, protocolValue, decodedFacingIndex);

        if (facing != facingOrig && property.getValues().contains(facing))
        {
            if (state.getBlock() instanceof BedBlock)
            {
                BlockPos headPos = context.pos.offset(facing);
                World world = context.getWorld();

                if (world.getBlockState(headPos).getBlock().canReplace(world, headPos) == false)
                {
                    return null;
                }
            }

            state = state.with(property, facing);
        }

        return state;
    }

    public static class UseContext
    {
        private final World world;
        private final BlockPos pos;
        private final Direction side;
        private final Vec3d hitVec;
        private final LivingEntity entity;
        private final Hand hand;

        private UseContext(World world, BlockPos pos, Direction side, Vec3d hitVec, LivingEntity entity, Hand hand)
        {
            this.world = world;
            this.pos = pos;
            this.side = side;
            this.hitVec = hitVec;
            this.entity = entity;
            this.hand = hand;
        }

        public static UseContext of(World world, BlockPos pos, Direction side, Vec3d hitVec, LivingEntity entity, Hand hand)
        {
            return new UseContext(world, pos, side, hitVec, entity, hand);
        }

        /*
        public static UseContext of(ItemPlacementContext ctx, Hand hand)
        {
            Vec3d pos = ctx.getHitPos();
            return new UseContext(ctx.getWorld(), ctx.getBlockPos(), ctx.getSide(), new Vec3d(pos.x, pos.y, pos.z),
                                  ctx.getPlayer(), hand, ctx);
        }
        */

        public World getWorld()
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

        public Vec3d getHitVec()
        {
            return this.hitVec;
        }

        public LivingEntity getEntity()
        {
            return this.entity;
        }

        public Hand getHand()
        {
            return this.hand;
        }
    }
}
