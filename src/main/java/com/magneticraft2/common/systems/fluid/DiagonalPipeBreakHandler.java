package com.magneticraft2.common.systems.fluid;

import com.magneticraft2.common.block.stage.copper.DiagonalWaterPipeBlock;
import com.magneticraft2.common.block.stage.copper.WaterPipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Break one complete direct 45-degree pipe run as a single physical assembly.
 *
 * The two ordinary Copper pipe endpoints can belong to other networks, so
 * neither surviving ordinary pipe is destroyed. All remaining generated
 * diagonal segments are destroyed once, and both diagonal endpoint ports
 * are cleared. Works only with a fully loaded, reciprocal direct run; never
 * destroys unrelated pipes or force-loads chunks to guess where a run ends.
 *
 * Legacy elbow-style diagonal links use different section/socket states and
 * are intentionally excluded from this direct-link teardown.
 */
public final class DiagonalPipeBreakHandler {
    private static final int MAX_SPAN = 8;
    private static final ThreadLocal<Boolean> BREAKING_RUN =
            ThreadLocal.withInitial(() -> false);

    private DiagonalPipeBreakHandler() {
    }

    public static boolean isBreakingRun() {
        return BREAKING_RUN.get();
    }

    /**
     * May run in playerWillDestroy (where creative drops are known) or
     * onRemove (for explosions and other non-player removals).
     *
     * The originally broken block is left to Minecraft to remove/drop.
     */
    public static boolean breakRun(Level level, BlockPos origin,
                                   BlockState removedState, boolean dropPieces) {
        if (level.isClientSide || isBreakingRun()) {
            return false;
        }
        Run run = findRun(level, origin, removedState);
        if (run == null) {
            return false;
        }

        BREAKING_RUN.set(true);
        try {
            // Disconnect all surviving ordinary endpoints before destroying
            // pieces. This prevents the graph from seeing a temporary path.
            for (BlockPos endpoint : run.endpoints()) {
                if (endpoint.equals(origin)) {
                    continue;
                }
                BlockState state = level.getBlockState(endpoint);
                if (state.getBlock() instanceof WaterPipeBlock
                        && state.getValue(WaterPipeBlock.DIAGONAL)
                                != WaterPipeBlock.DiagonalPort.NONE) {
                    level.setBlock(endpoint, state.setValue(
                            WaterPipeBlock.DIAGONAL, WaterPipeBlock.DiagonalPort.NONE),
                            Block.UPDATE_ALL);
                }
            }

            for (BlockPos part : run.segments()) {
                if (part.equals(origin)) {
                    continue;
                }
                // destroyBlock uses the normal Copper pipe loot table. The
                // guard prevents nested onRemove from producing extra drops.
                if (level.getBlockState(part).getBlock()
                        instanceof DiagonalWaterPipeBlock) {
                    level.destroyBlock(part, dropPieces);
                }
            }
        } finally {
            BREAKING_RUN.remove();
        }
        return true;
    }

    private static Run findRun(Level level, BlockPos origin, BlockState state) {
        if (state.getBlock() instanceof WaterPipeBlock) {
            BlockPos direction = state.getValue(WaterPipeBlock.DIAGONAL).step();
            if (direction == null) {
                return null;
            }
            DiagonalWaterPipeBlock.Axis axis =
                    DiagonalWaterPipeBlock.Axis.fromTravelStep(direction);
            Trace other = axis == null ? null : trace(level, origin, direction, axis);
            // A directly connected endpoint requires at least one real section.
            if (other == null || other.segments().isEmpty()) {
                return null;
            }
            return new Run(List.of(origin.immutable(), other.endpoint()),
                    other.segments());
        }

        if (!(state.getBlock() instanceof DiagonalWaterPipeBlock)
                || state.getValue(DiagonalWaterPipeBlock.SECTION)
                        != DiagonalWaterPipeBlock.Section.BOTH
                || state.getValue(DiagonalWaterPipeBlock.SOCKET)
                        != DiagonalWaterPipeBlock.Socket.NONE) {
            return null;
        }

        DiagonalWaterPipeBlock.Axis axis = state.getValue(DiagonalWaterPipeBlock.AXIS);
        BlockPos step = axis.step();
        Trace forward = trace(level, origin, step, axis);
        Trace backward = trace(level, origin,
                new BlockPos(-step.getX(), -step.getY(), -step.getZ()), axis);
        if (forward == null || backward == null
                || forward.segments().size() + backward.segments().size() + 1
                        > MAX_SPAN - 1) {
            return null;
        }

        List<BlockPos> parts = new ArrayList<>(1
                + forward.segments().size() + backward.segments().size());
        parts.add(origin.immutable());
        parts.addAll(forward.segments());
        parts.addAll(backward.segments());
        return new Run(List.of(forward.endpoint(), backward.endpoint()),
                List.copyOf(parts));
    }

    /**
     * Follows at most eight diagonal grid steps. A valid run terminates at
     * an ordinary WaterPipeBlock with a reciprocal diagonal port.
     * Encountering missing sections, another axis, a legacy elbow fitting,
     * or an unloaded chunk makes the trace fail closed.
     */
    private static Trace trace(Level level, BlockPos origin, BlockPos travel,
                               DiagonalWaterPipeBlock.Axis axis) {
        List<BlockPos> parts = new ArrayList<>(MAX_SPAN - 1);
        WaterPipeBlock.DiagonalPort reciprocal =
                WaterPipeBlock.DiagonalPort.forStep(
                        new BlockPos(-travel.getX(), -travel.getY(), -travel.getZ()));

        for (int i = 1; i <= MAX_SPAN; i++) {
            BlockPos pos = origin.offset(travel.getX() * i,
                    travel.getY() * i, travel.getZ() * i);
            if (!level.hasChunkAt(pos)) {
                return null;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof WaterPipeBlock) {
                return state.getValue(WaterPipeBlock.DIAGONAL) == reciprocal
                        ? new Trace(pos.immutable(), List.copyOf(parts))
                        : null;
            }
            if (!(state.getBlock() instanceof DiagonalWaterPipeBlock)
                    || state.getValue(DiagonalWaterPipeBlock.AXIS) != axis
                    || state.getValue(DiagonalWaterPipeBlock.SECTION)
                            != DiagonalWaterPipeBlock.Section.BOTH
                    || state.getValue(DiagonalWaterPipeBlock.SOCKET)
                            != DiagonalWaterPipeBlock.Socket.NONE
                    || parts.size() >= MAX_SPAN - 1) {
                return null;
            }
            parts.add(pos.immutable());
        }
        return null;
    }

    private record Trace(BlockPos endpoint, List<BlockPos> segments) {
    }

    private record Run(List<BlockPos> endpoints, List<BlockPos> segments) {
    }
}
