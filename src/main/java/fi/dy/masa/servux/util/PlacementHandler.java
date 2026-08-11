package fi.dy.masa.servux.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableSet;
import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.config.Configs;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBanner;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockCake;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PlacementHandler
{
    public static final ImmutableSet<IProperty<?>> WHITELISTED_PROPERTIES = ImmutableSet.of(
            // BooleanProperty:
            // INVERTED - DaylightDetector
            // OPEN - Barrel, Door, FenceGate, Trapdoor
            // PERSISTENT - Leaves
            BlockDoor.OPEN,
            BlockFenceGate.OPEN,
            BlockTrapDoor.OPEN,
            BlockLeaves.DECAYABLE,
            BlockLeaves.CHECK_DECAY,
            // EnumProperty:
            // AXIS - Pillar
            // BLOCK_HALF - Stairs, Trapdoor
            // CHEST_TYPE - Chest
            // COMPARATOR_MODE - Comparator
            // DOOR_HINGE - Door
            // SLAB_TYPE - Slab - PARTIAL ONLY: TOP and BOTTOM, not DOUBLE
            // STAIR_SHAPE - Stairs (needed to get the correct state, otherwise the player facing would be a factor)
            // WALL_MOUNT_LOCATION - Button, Grindstone, Lever
            BlockRotatedPillar.AXIS,
            BlockStairs.HALF,
            BlockTrapDoor.HALF,
            BlockStairs.SHAPE,
            BlockRedstoneComparator.MODE,
            BlockDoor.HINGE,
            BlockSlab.HALF,
            BlockLever.FACING,
            // IntProperty:
            // BITES - Cake
            // DELAY - Repeater
            // NOTE - NoteBlock
            // ROTATION - Banner, Sign, Skull
            BlockCake.BITES,
            BlockRedstoneRepeater.DELAY,
            BlockBanner.FACING,
            BlockBanner.ROTATION,
            BlockStandingSign.ROTATION,
            BlockWallSign.FACING,
            BlockSkull.FACING
    );

    @Nullable
    public static IBlockState applyPlacementProtocolToPlacementState(IBlockState state, UseContext context)
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

    public static IBlockState applyPlacementProtocolV2(IBlockState state, UseContext context)
    {
        int protocolValue = (int) context.getHitVec().x - 2;

        //System.out.printf("V2 raw protocolValue: 0x%08X, bx: %d, hx: %.3f\n", protocolValue, context.getPos().getX(), context.getHitVec().x);
        if (protocolValue < 0)
        {
            return state;
        }

        Optional<PropertyDirection> propertyOpt = BlockUtils.getFirstDirectionProperty(state);

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

            if (block instanceof BlockRedstoneRepeater)
            {
                Integer delay = (protocolValue / 16) + 1;

                if (BlockRedstoneRepeater.DELAY.getAllowedValues().contains(delay))
                {
                    state = state.withProperty(BlockRedstoneRepeater.DELAY, delay);
                }
            }
            else if (block instanceof BlockRedstoneComparator)
            {
                state = state.withProperty(BlockRedstoneComparator.MODE, BlockRedstoneComparator.Mode.SUBTRACT);
            }
            else if (block instanceof BlockTrapDoor)
            {
                state = state.withProperty(BlockTrapDoor.HALF, BlockTrapDoor.DoorHalf.TOP);
            }
            else if (block instanceof BlockStairs && state.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.BOTTOM)
            {
                state = state.withProperty(BlockStairs.HALF, BlockStairs.EnumHalf.TOP);
            }
        }

        return state;
    }

    public static <T extends Comparable<T>> IBlockState applyPlacementProtocolV3(IBlockState state, UseContext context)
    {
        int protocolValue = (int) context.getHitVec().x - 2;
        //System.out.printf("V3 raw protocolValue: 0x%08X, bx: %d, hx: %.3f\n", protocolValue, context.getPos().getX(), context.getHitVec().x);

        if (protocolValue < 0)
        {
            return state;
        }

        Optional<PropertyDirection> propertyOpt = BlockUtils.getFirstDirectionProperty(state);

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

        List<IProperty<?>> propList = new ArrayList<>(state.getBlock().getBlockState().getProperties());
        propList.sort(Comparator.comparing(IProperty::getName));

        try
        {
            for (IProperty<?> p : propList)
            {
                if ((p instanceof PropertyDirection) == false &&
                    WHITELISTED_PROPERTIES.contains(p))
                {
                    @SuppressWarnings("unchecked")
                    IProperty<T> prop = (IProperty<T>) p;
                    List<T> list = new ArrayList<>(prop.getAllowedValues());
                    list.sort(Comparable::compareTo);

                    int requiredBits = MathHelper.log2(MathHelper.smallestEncompassingPowerOfTwo(list.size()));
                    int bitMask = ~(0xFFFFFFFF << requiredBits);
                    int valueIndex = protocolValue & bitMask;
                    //System.out.printf("trying to apply valInd: %d, bits: %d, prot val: 0x%08X\n", valueIndex, requiredBits, protocolValue);

                    if (valueIndex >= 0 && valueIndex < list.size())
                    {
                        T value = list.get(valueIndex);

                        if (state.getValue(prop).equals(value) == false)
                        {
                            //System.out.printf("applying %s: %s\n", prop.getName(), value);
                            state = state.withProperty(prop, value);
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

    private static IBlockState applyDirectionProperty(IBlockState state, UseContext context,
                                                      PropertyDirection property, int protocolValue)
    {
        EnumFacing facingOrig = state.getValue(property);
        EnumFacing facing = facingOrig;
        int decodedFacingIndex = (protocolValue & 0x7);

        if (decodedFacingIndex == 6) // the opposite of the normal facing requested
        {
            facing = facing.getOpposite();
        }
        else if (decodedFacingIndex >= 0 && decodedFacingIndex <= 5)
        {
            facing = EnumFacing.byIndex(decodedFacingIndex);

            if (property.getAllowedValues().contains(facing) == false)
            {
                facing = context.getEntity().getHorizontalFacing().getOpposite();
            }
        }

        //System.out.printf("plop facing: %s -> %s (prot: %d, dec: %d)\n", facingOrig, facing, protocolValue, decodedFacingIndex);

        if (facing != facingOrig && property.getAllowedValues().contains(facing))
        {
            if (state.getBlock() instanceof BlockBed)
            {
                BlockPos headPos = context.pos.offset(facing);
                World world = context.getWorld();

                if (world.getBlockState(headPos).getBlock().isReplaceable(world, headPos) == false)
                {
                    return null;
                }
            }

            state = state.withProperty(property, facing);
        }

        return state;
    }

    public static class UseContext
    {
        private final World world;
        private final BlockPos pos;
        private final EnumFacing side;
        private final Vec3d hitVec;
        private final EntityLivingBase entity;
        private final EnumHand hand;

        private UseContext(World world, BlockPos pos, EnumFacing side, Vec3d hitVec, EntityLivingBase entity, EnumHand hand)
        {
            this.world = world;
            this.pos = pos;
            this.side = side;
            this.hitVec = hitVec;
            this.entity = entity;
            this.hand = hand;
        }

        public static UseContext of(World world, BlockPos pos, EnumFacing side, Vec3d hitVec, EntityLivingBase entity, EnumHand hand)
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

        public EnumFacing getSide()
        {
            return this.side;
        }

        public Vec3d getHitVec()
        {
            return this.hitVec;
        }

        public EntityLivingBase getEntity()
        {
            return this.entity;
        }

        public EnumHand getHand()
        {
            return this.hand;
        }
    }
}
