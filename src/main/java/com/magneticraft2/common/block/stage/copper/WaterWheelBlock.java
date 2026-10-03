package com.magneticraft2.common.block.stage.copper;

import com.magneticraft2.common.block.general.GearBlock;
import com.magneticraft2.common.blockentity.stage.copper.WaterWheelBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.world.level.block.DirectionalBlock.FACING;

/**
 * Copper Age wooden water wheel source.
 *
 * Small wheels occupy one block. Large wheels use this center/axle controller plus eight
 * generated WaterWheelFillerBlock cells in the plane perpendicular to the axle.
 */
public class WaterWheelBlock extends GearBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape SMALL_X = Block.box(2.0D, 0.0D, 0.0D, 14.0D, 16.0D, 16.0D);
    private static final VoxelShape SMALL_Z = Block.box(0.0D, 0.0D, 2.0D, 16.0D, 16.0D, 14.0D);
    private static final VoxelShape LARGE_CENTER_X = Block.box(4.0D, 0.0D, 0.0D, 12.0D, 16.0D, 16.0D);
    private static final VoxelShape LARGE_CENTER_Z = Block.box(0.0D, 0.0D, 4.0D, 16.0D, 16.0D, 12.0D);

    private final boolean large;

    public WaterWheelBlock(boolean large) {
        super(BlockBehaviour.Properties.of()
                .strength(3.0F)
                .noOcclusion());

        this.large = large;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.EAST)
                .setValue(ACTIVE, false)
                .setValue(WATERLOGGED, false));
    }

    public boolean isLarge() {
        return large;
    }

    @Override
    protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new WaterWheelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction axleDirection = context.getClickedFace();
        if (axleDirection.getAxis() == Direction.Axis.Y) {
            axleDirection = context.getHorizontalDirection().getClockWise();
        }

        BlockPos pos = context.getClickedPos();
        boolean waterlogged = context.getLevel()
                .getFluidState(pos)
                .is(FluidTags.WATER);

        BlockState placement = defaultBlockState()
                .setValue(FACING, axleDirection)
                .setValue(ACTIVE, false)
                .setValue(WATERLOGGED, waterlogged);

        if (large && !canFormLargeWheel(context.getLevel(), pos, axleDirection.getAxis())) {
            return null;
        }

        return validateGearPlacement(context, placement);
    }

    @Override
    public int getPlacementGearTeeth(BlockState state) {
        return large ? 16 : 8;
    }

    @Override
    public boolean isShaftLikeForPlacement(BlockState state) {
        return true;
    }

    @Override
    public Direction.Axis getPlacementGearAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE, WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state,
                                  Direction direction,
                                  BlockState neighborState,
                                  LevelAccessor level,
                                  BlockPos pos,
                                  BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(
                    pos,
                    Fluids.WATER,
                    Fluids.WATER.getTickDelay(level)
            );
        }

        return super.updateShape(
                state,
                direction,
                neighborState,
                level,
                pos,
                neighborPos
        );
    }

    @Override
    public VoxelShape getShape(BlockState state,
                               BlockGetter level,
                               BlockPos pos,
                               CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state,
                                        BlockGetter level,
                                        BlockPos pos,
                                        CollisionContext context) {
        return shapeFor(state);
    }

    private VoxelShape shapeFor(BlockState state) {
        Direction.Axis axis = state.getValue(FACING).getAxis();
        if (large) {
            return axis == Direction.Axis.X
                    ? LARGE_CENTER_X
                    : LARGE_CENTER_Z;
        }

        return axis == Direction.Axis.X
                ? SMALL_X
                : SMALL_Z;
    }

    @Override
    public void onPlace(BlockState state,
                        Level level,
                        BlockPos pos,
                        BlockState oldState,
                        boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide
                && large
                && !oldState.is(this)) {
            formLargeWheel(
                    level,
                    pos,
                    state.getValue(FACING).getAxis()
            );
        }
    }

    @Override
    public void onRemove(BlockState state,
                         Level level,
                         BlockPos pos,
                         BlockState newState,
                         boolean movedByPiston) {
        if (!level.isClientSide
                && large
                && !state.is(newState.getBlock())) {
            removeLargeWheelFillers(
                    level,
                    pos,
                    state.getValue(FACING).getAxis()
            );
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static boolean isLargeWheelIntact(Level level,
                                             BlockPos center,
                                             Direction.Axis axis) {
        for (BlockPos fillerPos : largeWheelFillerPositions(center, axis)) {
            BlockState state = level.getBlockState(fillerPos);
            if (!state.is(BlockRegistry.WATER_WHEEL_FILLER.get())
                    || state.getValue(WaterWheelFillerBlock.AXIS) != axis) {
                return false;
            }
        }
        return true;
    }

    private boolean canFormLargeWheel(Level level,
                                      BlockPos center,
                                      Direction.Axis axis) {
        for (BlockPos fillerPos : largeWheelFillerPositions(center, axis)) {
            BlockState state = level.getBlockState(fillerPos);
            if (!state.isAir() && !state.canBeReplaced()) {
                return false;
            }
        }

        return true;
    }

    private void formLargeWheel(Level level,
                                BlockPos center,
                                Direction.Axis axis) {
        BlockState fillerBase = BlockRegistry.WATER_WHEEL_FILLER.get()
                .defaultBlockState()
                .setValue(WaterWheelFillerBlock.AXIS, axis);

        for (BlockPos fillerPos : largeWheelFillerPositions(center, axis)) {
            boolean waterlogged = level.getFluidState(fillerPos).is(FluidTags.WATER);

            int horizontal = axis == Direction.Axis.X
                    ? fillerPos.getZ() - center.getZ()
                    : fillerPos.getX() - center.getX();
            int vertical = fillerPos.getY() - center.getY();

            level.setBlock(
                    fillerPos,
                    fillerBase
                            .setValue(WaterWheelFillerBlock.PART, partForOffset(horizontal, vertical))
                            .setValue(WaterWheelFillerBlock.ACTIVE, false)
                            .setValue(WaterWheelFillerBlock.WATERLOGGED, waterlogged),
                    Block.UPDATE_ALL
            );
        }
    }

    public static WaterWheelFillerBlock.Part partForOffset(int horizontal, int vertical) {
        if (vertical > 0) {
            if (horizontal < 0) {
                return WaterWheelFillerBlock.Part.TOP_LEFT;
            }
            if (horizontal > 0) {
                return WaterWheelFillerBlock.Part.TOP_RIGHT;
            }
            return WaterWheelFillerBlock.Part.TOP;
        }

        if (vertical < 0) {
            if (horizontal < 0) {
                return WaterWheelFillerBlock.Part.BOTTOM_LEFT;
            }
            if (horizontal > 0) {
                return WaterWheelFillerBlock.Part.BOTTOM_RIGHT;
            }
            return WaterWheelFillerBlock.Part.BOTTOM;
        }

        return horizontal < 0
                ? WaterWheelFillerBlock.Part.LEFT
                : WaterWheelFillerBlock.Part.RIGHT;
    }

    private void removeLargeWheelFillers(Level level,
                                         BlockPos center,
                                         Direction.Axis axis) {
        for (BlockPos fillerPos : largeWheelFillerPositions(center, axis)) {
            BlockState fillerState = level.getBlockState(fillerPos);
            if (!fillerState.is(BlockRegistry.WATER_WHEEL_FILLER.get())) {
                continue;
            }

            if (fillerState.getValue(WaterWheelFillerBlock.WATERLOGGED)) {
                level.setBlock(
                        fillerPos,
                        Blocks.WATER.defaultBlockState(),
                        Block.UPDATE_ALL
                );
            } else {
                level.removeBlock(fillerPos, false);
            }
        }
    }

    public static List<BlockPos> largeWheelFillerPositions(BlockPos center,
                                                            Direction.Axis axis) {
        List<BlockPos> positions = new ArrayList<>(8);

        for (int horizontal = -1; horizontal <= 1; horizontal++) {
            for (int vertical = -1; vertical <= 1; vertical++) {
                if (horizontal == 0 && vertical == 0) {
                    continue;
                }

                BlockPos offset = axis == Direction.Axis.X
                        ? center.offset(0, vertical, horizontal)
                        : center.offset(horizontal, vertical, 0);
                positions.add(offset.immutable());
            }
        }

        return positions;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                                                                  BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                        type,
                        BlockEntityRegistry.WATER_WHEEL_BE.get(),
                        WaterWheelBlockEntity::serverTick
                );
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType,
            BlockEntityType<E> expectedType,
            BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType
                ? (BlockEntityTicker<A>) ticker
                : null;
    }
}
