package com.magneticraft2.common.block.stage.steel;

import com.magneticraft2.common.registry.registers.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Inert Steel IV-C structural assembly. Segment states describe one of eight
 * ring locations (0..7) or a front/back closure (8..9). The registry ID and
 * item recipes are unchanged; the JSON multiblock still matches BLOCK IDs.
 * Static baked shapes avoid geometry allocations during rendering or ticks.
 */
public final class SteelBoilerStructuralBlock extends HorizontalDirectionalBlock {
    public static final IntegerProperty SEGMENT = IntegerProperty.create("segment", 0, 9);
    private static final Direction[] DIRECTIONS = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
    private static final int[][][] LOCAL_BOXES = {
        {{15,0,0,16,4,14},{15,0,14,16,4,16},{9,4,0,16,8,14},{9,4,14,16,8,16},{6,8,0,16,12,14},{6,8,14,16,12,16},{3,12,0,13,16,14},{3,12,14,13,16,16},{10,0,0,14,4,14},{10,0,14,14,4,16}},
        {{0,0,0,16,4,14},{0,0,14,16,4,16},{0,4,0,16,8,14},{0,4,14,16,8,16},{0,8,0,2,12,14},{0,8,14,2,12,16},{14,8,0,16,12,14},{14,8,14,16,12,16}},
        {{0,0,0,1,4,14},{0,0,14,1,4,16},{0,4,0,7,8,14},{0,4,14,7,8,16},{0,8,0,10,12,14},{0,8,14,10,12,16},{3,12,0,13,16,14},{3,12,14,13,16,16},{2,0,0,6,4,14},{2,0,14,6,4,16}},
        {{1,0,0,10,4,14},{1,0,14,10,4,16},{1,4,0,7,12,14},{1,4,14,7,12,16},{1,12,0,10,16,14},{1,12,14,10,16,16}},
        {{6,0,0,15,4,14},{6,0,14,15,4,16},{9,4,0,15,12,14},{9,4,14,15,12,16},{6,12,0,15,16,14},{6,12,14,15,16,16}},
        {{3,0,0,13,4,14},{3,0,14,13,4,16},{6,4,0,16,8,14},{6,4,14,16,8,16},{9,8,0,16,12,14},{9,8,14,16,12,16},{15,12,0,16,16,14},{15,12,14,16,16,16}},
        {{0,4,0,2,8,14},{0,4,14,2,8,16},{14,4,0,16,8,14},{14,4,14,16,8,16},{0,8,0,16,12,14},{0,8,14,16,12,16},{0,12,0,16,16,14},{0,12,14,16,16,16}},
        {{3,0,0,13,4,14},{3,0,14,13,4,16},{0,4,0,10,8,14},{0,4,14,10,8,16},{0,8,0,7,12,14},{0,8,14,7,12,16},{0,12,0,1,16,14},{0,12,14,1,16,16}},
        {{0,0,12,16,16,14},{4,2,14,12,4,15},{2,4,14,14,12,15},{4,12,14,12,14,15},{5,5,15,11,11,16}},
        {{0,0,2,16,16,4},{4,2,1,12,4,2},{2,4,1,14,12,2},{4,12,1,12,14,2},{5,5,0,11,11,1}}
    };
    private static final VoxelShape[][] COLLISION = buildShapes();
    private final boolean shell;

    private static VoxelShape[][] buildShapes() {
        VoxelShape[][] result = new VoxelShape[10][4];
        for (int segment = 0; segment < LOCAL_BOXES.length; segment++) {
            for (int direction = 0; direction < 4; direction++) {
                VoxelShape shape = Shapes.empty();
                for (int[] box : LOCAL_BOXES[segment]) {
                    int x0 = box[0], y0 = box[1], z0 = box[2];
                    int x1 = box[3], y1 = box[4], z1 = box[5];
                    for (int turn = 0; turn < direction; turn++) {
                        int nextX0 = 16 - z1, nextX1 = 16 - z0;
                        z0 = x0; z1 = x1; x0 = nextX0; x1 = nextX1;
                    }
                    shape = Shapes.joinUnoptimized(shape,
                            Block.box(x0, y0, z0, x1, y1, z1), BooleanOp.OR);
                }
                result[segment][direction] = shape.optimize();
            }
        }
        return result;
    }

    public SteelBoilerStructuralBlock(boolean shell) {
        super(BlockBehaviour.Properties.of().strength(5F, 8F)
                .sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
        this.shell = shell;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.SOUTH).setValue(SEGMENT, shell ? 0 : 8));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SEGMENT);
    }

    /**
     * With controller-first construction, align the segment automatically to
     * its JSON-world location. Players need no wrench or extra variant items.
     * Independent, already placed parts remain placeable and keep old block IDs.
     */
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos at = context.getClickedPos();
        BlockState fallback = defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-4, -3, -4), at.offset(4, 0, 4))) {
            BlockState state = context.getLevel().getBlockState(pos);
            if (!state.is(BlockRegistry.STEEL_BOILER_VESSEL.get())) continue;
            Direction facing = state.getValue(SteelBoilerVesselBlock.FACING);
            int dx = at.getX() - pos.getX(), dy = at.getY() - pos.getY();
            int dz = at.getZ() - pos.getZ();
            int cx = switch (facing) {
                case NORTH -> -dx;
                case EAST -> -dz;
                case WEST -> dz;
                default -> dx;
            };
            int cz = switch (facing) {
                case NORTH -> -dz;
                case EAST -> dx;
                case WEST -> -dx;
                default -> dz;
            };
            int x = cx + 1, z = cz + 4;
            int segment = -1;
            if (shell && z >= 1 && z <= 3) {
                if (dy == 1 && x >= 0 && x <= 2) segment = x;
                else if (dy == 2 && x == 0) segment = 3;
                else if (dy == 2 && x == 2) segment = 4;
                else if (dy == 3 && x >= 0 && x <= 2) segment = x + 5;
            } else if (!shell && dy == 2 && x == 1 && (z == 0 || z == 4)) {
                segment = z == 4 ? 8 : 9;
            }
            if (segment != -1)
                return defaultBlockState().setValue(FACING, facing).setValue(SEGMENT, segment);
        }
        return fallback;
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        int segment = state.getValue(SEGMENT);
        if (shell && segment >= 8) segment = 0;
        if (!shell && segment < 8) segment = 8;
        Direction facing = state.getValue(FACING);
        for (int i = 0; i < DIRECTIONS.length; i++) {
            if (DIRECTIONS[i] == facing) return COLLISION[segment][i];
        }
        return COLLISION[segment][0];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
}
