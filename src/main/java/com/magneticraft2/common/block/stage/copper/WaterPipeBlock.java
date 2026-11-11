package com.magneticraft2.common.block.stage.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.magneticraft2.common.systems.fluid.DiagonalPipeBreakHandler;
import com.magneticraft2.common.block.stage.steam.BoilerServiceFittingBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public class WaterPipeBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    // One optional direct 45-degree connection per existing Copper pipe.
    // Diagonal positions are saved as blockstate, not block entities.
    public static final EnumProperty<DiagonalPort> DIAGONAL =
            EnumProperty.create("diagonal", DiagonalPort.class);

    private static final VoxelShape[] DIAGONAL_ARM_SHAPES = createDiagonalArmShapes();

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
                        .setValue(DIAGONAL, DiagonalPort.NONE)
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
        // Only explicit end fittings of the diagonal line expose a face socket.
        if (block instanceof DiagonalWaterPipeBlock) {
            return DiagonalWaterPipeBlock.hasSocket(state, direction.getOpposite());
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

        // Copper Fluid Tanks intentionally expose one visible pipe socket: TOP.
        // From the pipe's point of view that means the neighbouring tank is DOWN.
        if (block instanceof CopperFluidTankBlock) {
            return direction == Direction.DOWN;
        }

        if (state.is(com.magneticraft2.common.registry.registers.BlockRegistry.BOILER_WATER_INLET.get())) {
            Direction facing = state.getValue(BoilerServiceFittingBlock.FACING);
            return BoilerServiceFittingBlock.waterPipePosition(neighbourPos, facing).equals(pipePos);
        }

        // Generic Forge interop: render a real pipe arm toward any neighbouring
        // block entity that exposes a fluid handler on the touching face. Some
        // mods expose only an unsided handler, so use the same fallback as the
        // runtime fluid-network discovery.
        BlockEntity blockEntity =
                level.getBlockEntity(neighbourPos);

        if (blockEntity != null) {
            if (blockEntity
                    .getCapability(
                            ForgeCapabilities.FLUID_HANDLER,
                            direction.getOpposite()
                    )
                    .isPresent()) {
                return true;
            }

            return blockEntity
                    .getCapability(
                            ForgeCapabilities.FLUID_HANDLER,
                            null
                    )
                    .isPresent();
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
                DOWN,
                DIAGONAL
        );
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        VoxelShape shape = CORE;
        DiagonalPort port = state.getValue(DIAGONAL);
        if (port != DiagonalPort.NONE) {
            shape = Shapes.or(shape, DIAGONAL_ARM_SHAPES[port.ordinal()]);
        }

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

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state,
                                  Player player) {
        if (!level.isClientSide && state.getValue(DIAGONAL) != DiagonalPort.NONE) {
            // Survival refunds all generated sections; creative never does.
            DiagonalPipeBreakHandler.breakRun(level, pos, state,
                    !player.getAbilities().instabuild);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos,
                         BlockState newState, boolean moving) {
        if (oldState.getBlock() != newState.getBlock() && !level.isClientSide
                && !DiagonalPipeBreakHandler.isBreakingRun()) {
            BlockPos step = oldState.getValue(DIAGONAL).step();
            if (step != null && !DiagonalPipeBreakHandler.breakRun(
                    level, pos, oldState, true)) {
                // Retain the prior cleanup for incomplete/legacy links.
                DiagonalWaterPipeBlock.detachOppositeEndpoint(level, pos, step);
            }
        }
        super.onRemove(oldState, level, pos, newState, moving);
    }

    private static VoxelShape[] createDiagonalArmShapes() {
        VoxelShape[] result = new VoxelShape[DiagonalPort.values().length];
        for (DiagonalPort port : DiagonalPort.values()) {
            VoxelShape shape = Shapes.empty();
            BlockPos step = port.step();
            if (step != null) {
                for (int i = 0; i <= 12; i++) {
                    double t = i / 24.0D;
                    double x = 8.0D + step.getX() * 16.0D * t;
                    double y = 8.0D + step.getY() * 16.0D * t;
                    double z = 8.0D + step.getZ() * 16.0D * t;
                    shape = Shapes.or(shape, Block.box(
                            Math.max(0.0D, x - 2.0D),
                            Math.max(0.0D, y - 2.0D),
                            Math.max(0.0D, z - 2.0D),
                            Math.min(16.0D, x + 2.0D),
                            Math.min(16.0D, y + 2.0D),
                            Math.min(16.0D, z + 2.0D)));
                }
            }
            result[port.ordinal()] = shape.optimize();
        }
        return result;
    }

    public enum DiagonalPort implements StringRepresentable {
        NONE("none", null, 0),
        XY_POS_POSITIVE("xy_pos_positive", DiagonalWaterPipeBlock.Axis.XY_POS, 1),
        XY_POS_NEGATIVE("xy_pos_negative", DiagonalWaterPipeBlock.Axis.XY_POS, -1),
        XY_NEG_POSITIVE("xy_neg_positive", DiagonalWaterPipeBlock.Axis.XY_NEG, 1),
        XY_NEG_NEGATIVE("xy_neg_negative", DiagonalWaterPipeBlock.Axis.XY_NEG, -1),
        XZ_POS_POSITIVE("xz_pos_positive", DiagonalWaterPipeBlock.Axis.XZ_POS, 1),
        XZ_POS_NEGATIVE("xz_pos_negative", DiagonalWaterPipeBlock.Axis.XZ_POS, -1),
        XZ_NEG_POSITIVE("xz_neg_positive", DiagonalWaterPipeBlock.Axis.XZ_NEG, 1),
        XZ_NEG_NEGATIVE("xz_neg_negative", DiagonalWaterPipeBlock.Axis.XZ_NEG, -1),
        YZ_POS_POSITIVE("yz_pos_positive", DiagonalWaterPipeBlock.Axis.YZ_POS, 1),
        YZ_POS_NEGATIVE("yz_pos_negative", DiagonalWaterPipeBlock.Axis.YZ_POS, -1),
        YZ_NEG_POSITIVE("yz_neg_positive", DiagonalWaterPipeBlock.Axis.YZ_NEG, 1),
        YZ_NEG_NEGATIVE("yz_neg_negative", DiagonalWaterPipeBlock.Axis.YZ_NEG, -1);

        private final String name;
        @Nullable private final BlockPos step;

        DiagonalPort(String name, @Nullable DiagonalWaterPipeBlock.Axis axis,
                     int sign) {
            this.name = name;
            if (axis == null) {
                this.step = null;
            } else {
                BlockPos a = axis.step();
                this.step = new BlockPos(a.getX() * sign,
                        a.getY() * sign, a.getZ() * sign);
            }
        }

        @Nullable
        public BlockPos step() {
            return step;
        }

        public static DiagonalPort forStep(BlockPos travel) {
            for (DiagonalPort port : values()) {
                if (port.step != null && port.step.equals(travel)) {
                    return port;
                }
            }
            return NONE;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
