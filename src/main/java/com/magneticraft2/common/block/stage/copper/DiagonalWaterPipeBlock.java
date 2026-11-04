package com.magneticraft2.common.block.stage.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Physical, non-ticking diagonal Copper pipe segment. Every port is explicit
 * in its saved blockstate; no block entity or separate fluid simulation exists.
 */
public final class DiagonalWaterPipeBlock extends Block {
    public static final EnumProperty<Axis> AXIS = EnumProperty.create("axis", Axis.class);
    public static final EnumProperty<Section> SECTION = EnumProperty.create("section", Section.class);
    public static final EnumProperty<Socket> SOCKET = EnumProperty.create("socket", Socket.class);
    private static final VoxelShape[][][] SHAPES = createShapes();

    public DiagonalWaterPipeBlock() {
        super(BlockBehaviour.Properties.of().strength(2.5F).noOcclusion());
        registerDefaultState(stateDefinition.any()
                .setValue(AXIS, Axis.XY_POS)
                .setValue(SECTION, Section.BOTH)
                .setValue(SOCKET, Socket.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, SECTION, SOCKET);
    }

    public static boolean hasSocket(BlockState state, Direction direction) {
        return state.getBlock() instanceof DiagonalWaterPipeBlock
                && state.getValue(SOCKET).direction() == direction;
    }

    /** Both diagonal ports must agree and the physical corner must be open. */
    public static boolean isConnectedDiagonal(Level level, BlockPos pos,
                                               BlockState state, int sign) {
        if (!(state.getBlock() instanceof DiagonalWaterPipeBlock)
                || !state.getValue(SECTION).opens(sign)) {
            return false;
        }
        BlockPos step = state.getValue(AXIS).step();
        BlockPos next = pos.offset(step.getX() * sign, step.getY() * sign, step.getZ() * sign);
        if (!level.hasChunkAt(next)) {
            return false;
        }
        BlockState other = level.getBlockState(next);
        if (!isCornerClear(level, pos, next)) {
            return false;
        }
        if (other.getBlock() instanceof DiagonalWaterPipeBlock) {
            return other.getValue(AXIS) == state.getValue(AXIS)
                    && other.getValue(SECTION).opens(-sign);
        }
        if (other.getBlock() instanceof WaterPipeBlock) {
            BlockPos otherPort = other.getValue(WaterPipeBlock.DIAGONAL).step();
            return otherPort != null
                    && otherPort.getX() == -step.getX() * sign
                    && otherPort.getY() == -step.getY() * sign
                    && otherPort.getZ() == -step.getZ() * sign;
        }
        return false;
    }

    /**
     * When a physical section or existing endpoint is broken, clear the distant
     * ordinary endpoint's diagonal port. The pieces remain in the world for
     * ordinary breaking/collection; no hidden logical connection survives.
     */
    public static void detachOppositeEndpoint(Level level, BlockPos origin, BlockPos direction) {
        for (int i = 1; i <= 8; i++) {
            BlockPos pos = origin.offset(direction.getX() * i,
                    direction.getY() * i, direction.getZ() * i);
            if (!level.hasChunkAt(pos)) {
                return; // Never force-load chunks while removing pipes.
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof DiagonalWaterPipeBlock) {
                BlockPos axis = state.getValue(AXIS).step();
                if (axis.getX() * direction.getY() != axis.getY() * direction.getX()
                        || axis.getX() * direction.getZ() != axis.getZ() * direction.getX()
                        || axis.getY() * direction.getZ() != axis.getZ() * direction.getY()) {
                    return;
                }
                continue;
            }
            if (state.getBlock() instanceof WaterPipeBlock) {
                BlockPos opposite = state.getValue(WaterPipeBlock.DIAGONAL).step();
                if (opposite != null
                        && opposite.getX() == -direction.getX()
                        && opposite.getY() == -direction.getY()
                        && opposite.getZ() == -direction.getZ()) {
                    level.setBlock(pos, state.setValue(
                            WaterPipeBlock.DIAGONAL, WaterPipeBlock.DiagonalPort.NONE),
                            Block.UPDATE_ALL);
                }
            }
            return;
        }
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos,
                         BlockState newState, boolean moving) {
        if (oldState.getBlock() != newState.getBlock() && !level.isClientSide) {
            BlockPos step = oldState.getValue(AXIS).step();
            detachOppositeEndpoint(level, pos, step);
            detachOppositeEndpoint(level, pos,
                    new BlockPos(-step.getX(), -step.getY(), -step.getZ()));
        }
        super.onRemove(oldState, level, pos, newState, moving);
    }

    /** The solid tube cannot cut between two opaque side blocks at a corner. */
    public static boolean isCornerClear(Level level, BlockPos from, BlockPos to) {
        int dx = Integer.signum(to.getX() - from.getX());
        int dy = Integer.signum(to.getY() - from.getY());
        int dz = Integer.signum(to.getZ() - from.getZ());
        BlockPos a, b;
        if (dx != 0 && dy != 0 && dz == 0) {
            a = from.offset(dx, 0, 0);
            b = from.offset(0, dy, 0);
        } else if (dx != 0 && dz != 0 && dy == 0) {
            a = from.offset(dx, 0, 0);
            b = from.offset(0, 0, dz);
        } else if (dy != 0 && dz != 0 && dx == 0) {
            a = from.offset(0, dy, 0);
            b = from.offset(0, 0, dz);
        } else {
            return false;
        }
        return level.hasChunkAt(a) && level.hasChunkAt(b)
                && level.getBlockState(a).isAir() && level.getBlockState(b).isAir();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world,
                               BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AXIS).ordinal()]
                [state.getValue(SECTION).ordinal()][state.getValue(SOCKET).ordinal()];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world,
                                        BlockPos pos, CollisionContext context) {
        return getShape(state, world, pos, context);
    }

    /** Cached voxel approximations; neither block ticks nor render frames build shapes. */
    private static VoxelShape[][][] createShapes() {
        VoxelShape[][][] result =
                new VoxelShape[Axis.values().length][Section.values().length][Socket.values().length];
        for (Axis axis : Axis.values()) {
            for (Section section : Section.values()) {
                for (Socket socket : Socket.values()) {
                    VoxelShape shape = Shapes.empty();
                    for (int i = 0; i <= 12; i++) {
                        double t = switch (section) {
                            case BOTH -> -0.5D + i / 12.0D;
                            case POSITIVE -> i / 24.0D;
                            case NEGATIVE -> -0.5D + i / 24.0D;
                        };
                        double x = 8.0D + 16.0D * t * axis.step().getX();
                        double y = 8.0D + 16.0D * t * axis.step().getY();
                        double z = 8.0D + 16.0D * t * axis.step().getZ();
                        shape = Shapes.or(shape, Block.box(
                                Math.max(0.0D, x - 2.0D),
                                Math.max(0.0D, y - 2.0D),
                                Math.max(0.0D, z - 2.0D),
                                Math.min(16.0D, x + 2.0D),
                                Math.min(16.0D, y + 2.0D),
                                Math.min(16.0D, z + 2.0D)));
                    }
                    if (socket.direction() != null) {
                        shape = Shapes.or(shape, socketArm(socket.direction()));
                    }
                    result[axis.ordinal()][section.ordinal()][socket.ordinal()] =
                            shape.optimize();
                }
            }
        }
        return result;
    }

    private static VoxelShape socketArm(Direction direction) {
        return switch (direction) {
            case NORTH -> Block.box(5, 5, 0, 11, 11, 8);
            case SOUTH -> Block.box(5, 5, 8, 11, 11, 16);
            case EAST -> Block.box(8, 5, 5, 16, 11, 11);
            case WEST -> Block.box(0, 5, 5, 8, 11, 11);
            case UP -> Block.box(5, 8, 5, 11, 16, 11);
            case DOWN -> Block.box(5, 0, 5, 11, 8, 11);
        };
    }

    public enum Axis implements StringRepresentable {
        XY_POS("xy_pos", 1, 1, 0), XY_NEG("xy_neg", 1, -1, 0),
        XZ_POS("xz_pos", 1, 0, 1), XZ_NEG("xz_neg", 1, 0, -1),
        YZ_POS("yz_pos", 0, 1, 1), YZ_NEG("yz_neg", 0, 1, -1);

        private final String name;
        private final BlockPos step;

        Axis(String name, int x, int y, int z) {
            this.name = name;
            this.step = new BlockPos(x, y, z);
        }

        public BlockPos step() {
            return step;
        }

        @Nullable
        public static Axis fromTravelStep(BlockPos travel) {
            for (Axis axis : values()) {
                BlockPos p = axis.step;
                if (travel.equals(p) || (travel.getX() == -p.getX()
                        && travel.getY() == -p.getY()
                        && travel.getZ() == -p.getZ())) {
                    return axis;
                }
            }
            return null;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public enum Section implements StringRepresentable {
        BOTH("both"), POSITIVE("positive"), NEGATIVE("negative");
        private final String name;

        Section(String name) {
            this.name = name;
        }

        public boolean opens(int sign) {
            return this == BOTH || (sign > 0 ? this == POSITIVE : this == NEGATIVE);
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public enum Socket implements StringRepresentable {
        NONE("none", null), NORTH("north", Direction.NORTH),
        SOUTH("south", Direction.SOUTH), EAST("east", Direction.EAST),
        WEST("west", Direction.WEST), UP("up", Direction.UP),
        DOWN("down", Direction.DOWN);

        private final String name;
        @Nullable private final Direction direction;

        Socket(String name, @Nullable Direction direction) {
            this.name = name;
            this.direction = direction;
        }

        @Nullable
        public Direction direction() {
            return direction;
        }

        public static Socket of(Direction direction) {
            for (Socket port : values()) {
                if (port.direction == direction) {
                    return port;
                }
            }
            throw new IllegalArgumentException("Unsupported pipe socket: " + direction);
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
