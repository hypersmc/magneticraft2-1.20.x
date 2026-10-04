package com.magneticraft2.common.block.stage.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WaterPipeBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final VoxelShape CORE =
            Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape ARM_NORTH =
            Block.box(5, 5, 0, 11, 11, 5);
    private static final VoxelShape ARM_SOUTH =
            Block.box(5, 5, 11, 11, 11, 16);
    private static final VoxelShape ARM_WEST =
            Block.box(0, 5, 5, 5, 11, 11);
    private static final VoxelShape ARM_EAST =
            Block.box(11, 5, 5, 16, 11, 11);
    private static final VoxelShape ARM_DOWN =
            Block.box(5, 0, 5, 11, 5, 11);
    private static final VoxelShape ARM_UP =
            Block.box(5, 11, 5, 11, 16, 11);

    public WaterPipeBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(2.5F)
                .noOcclusion());

        registerDefaultState(
                stateDefinition.any()
                        .setValue(NORTH, false)
                        .setValue(EAST, false)
                        .setValue(SOUTH, false)
                        .setValue(WEST, false)
                        .setValue(UP, false)
                        .setValue(DOWN, false)
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        LevelAccessor level = context.getLevel();
        BlockState state = defaultBlockState();

        for (Direction direction : Direction.values()) {
            state = state.setValue(
                    property(direction),
                    canConnect(
                            level,
                            pos,
                            direction
                    )
            );
        }

        return state;
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighbour,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighbourPos) {
        return state.setValue(
                property(direction),
                canConnect(
                        level,
                        pos,
                        direction
                )
        );
    }

    private static boolean canConnect(
            LevelAccessor level,
            BlockPos pipePos,
            Direction direction) {
        BlockPos neighbourPos =
                pipePos.relative(direction);
        BlockState state =
                level.getBlockState(neighbourPos);
        Block block =
                state.getBlock();

        if (block instanceof WaterPipeBlock) {
            return true;
        }

        // The pump only exposes fluid on its TOP. A pipe touching any mechanical
        // side must not pretend it is a valid fluid connection.
        if (block instanceof MechanicalWaterPumpBlock) {
            return direction == Direction.DOWN;
        }

        if (block instanceof MultiblockFluidInputBlock
                && state.hasProperty(
                        MultiblockFluidInputBlock.FACING
                )) {
            return state.getValue(
                    MultiblockFluidInputBlock.FACING
            ) == direction.getOpposite();
        }

        if (block instanceof MultiblockFluidOutputBlock
                && state.hasProperty(
                        MultiblockFluidOutputBlock.FACING
                )) {
            return state.getValue(
                    MultiblockFluidOutputBlock.FACING
            ) == direction.getOpposite();
        }

        return false;
    }

    public static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(
                NORTH,
                EAST,
                SOUTH,
                WEST,
                UP,
                DOWN
        );
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        VoxelShape shape = CORE;

        if (state.getValue(NORTH)) {
            shape = Shapes.or(shape, ARM_NORTH);
        }
        if (state.getValue(SOUTH)) {
            shape = Shapes.or(shape, ARM_SOUTH);
        }
        if (state.getValue(WEST)) {
            shape = Shapes.or(shape, ARM_WEST);
        }
        if (state.getValue(EAST)) {
            shape = Shapes.or(shape, ARM_EAST);
        }
        if (state.getValue(DOWN)) {
            shape = Shapes.or(shape, ARM_DOWN);
        }
        if (state.getValue(UP)) {
            shape = Shapes.or(shape, ARM_UP);
        }

        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return getShape(state, level, pos, context);
    }
}
