package com.magneticraft2.common.block.stage.bronze;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.magneticraft2.common.systems.PRESSURE.CapabilityPressure;
import com.magneticraft2.common.blockentity.stage.stone.BellowsMultiblockModuleEntity;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeIndustrialAirBlowerBlockEntity;

/**
 * Bronze air duct: passive collision/model arms and directional pressure links.
 * Shares the proven Water Pipe shape, but NEVER shares its fluid connectivity.
 * No block entity or tick: the active blower traverses the bounded duct graph.
 */
public class BronzeAirDuctBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final VoxelShape CORE =
            Block.box(5, 5, 5, 11, 11, 11);
    // Match the rendered bronze flanges and recessed duct arms rather than
    // colliding against an invisible full-width water-pipe segment.
    private static final VoxelShape ARM_NORTH = Shapes.or(
            Block.box(5, 5, 2, 11, 11, 5), Block.box(4, 4, 0, 12, 12, 2));
    private static final VoxelShape ARM_SOUTH = Shapes.or(
            Block.box(5, 5, 11, 11, 11, 14), Block.box(4, 4, 14, 12, 12, 16));
    private static final VoxelShape ARM_WEST = Shapes.or(
            Block.box(2, 5, 5, 5, 11, 11), Block.box(0, 4, 4, 2, 12, 12));
    private static final VoxelShape ARM_EAST = Shapes.or(
            Block.box(11, 5, 5, 14, 11, 11), Block.box(14, 4, 4, 16, 12, 12));
    private static final VoxelShape ARM_DOWN = Shapes.or(
            Block.box(5, 2, 5, 11, 5, 11), Block.box(4, 0, 4, 12, 2, 12));
    private static final VoxelShape ARM_UP = Shapes.or(
            Block.box(5, 11, 5, 11, 14, 11), Block.box(4, 14, 4, 12, 16, 12));

    // Only 64 possible connected shapes. Bake them once instead of repeated
    // Shapes.or allocations for every collision/selection query.
    private static final VoxelShape[] CONNECTION_SHAPES = buildConnectionShapes();

    private static VoxelShape[] buildConnectionShapes() {
        VoxelShape[] shapes = new VoxelShape[64];
        VoxelShape[] arms = {ARM_NORTH, ARM_EAST, ARM_SOUTH, ARM_WEST, ARM_UP, ARM_DOWN};
        for (int mask = 0; mask < shapes.length; mask++) {
            VoxelShape shape = CORE;
            for (int i = 0; i < arms.length; i++)
                if ((mask & (1 << i)) != 0) shape = Shapes.or(shape, arms[i]);
            shapes[mask] = shape.optimize();
        }
        return shapes;
    }

    public BronzeAirDuctBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(3.0F)
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

        if (block instanceof BronzeAirDuctBlock) {
            return true;
        }

        // Real pressure interface only, never water/fluid pipes. Side-facing
        // capability query enforces blower FRONT and kiln FRONT port restrictions.
        BlockEntity neighbourEntity = level.getBlockEntity(neighbourPos);
        if (neighbourEntity == null) return false;
        if (neighbourEntity instanceof BellowsMultiblockModuleEntity bellows
                && !bellows.isFormedModule()) return false;
        return neighbourEntity.getCapability(
                CapabilityPressure.PRESSURE, direction.getOpposite())
                .map(handler -> handler.canReceive()
                        || (neighbourEntity instanceof BronzeIndustrialAirBlowerBlockEntity
                            && handler.canSend()))
                .orElse(false);
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
        int mask = 0;
        if (state.getValue(NORTH)) mask |= 1;
        if (state.getValue(EAST)) mask |= 2;
        if (state.getValue(SOUTH)) mask |= 4;
        if (state.getValue(WEST)) mask |= 8;
        if (state.getValue(UP)) mask |= 16;
        if (state.getValue(DOWN)) mask |= 32;
        return CONNECTION_SHAPES[mask];
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
