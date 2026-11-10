package com.magneticraft2.common.systems.fluid;

import com.magneticraft2.common.block.stage.copper.DiagonalWaterPipeBlock;
import com.magneticraft2.common.block.stage.copper.WaterPipeBlock;
import com.magneticraft2.common.block.stage.steam.BoilerServiceFittingBlock;
import com.magneticraft2.common.systems.steam.BoilerWarmup;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared traversal for Magneticraft's passive Copper Fluid Pipes.
 *
 * Pipes deliberately have no block entities and never tick. Active endpoints
 * (pumps, tanks and future machines) traverse the connected pipe graph only
 * when they need to move fluid.
 */
public final class FluidPipeNetwork {
    public static final int DEFAULT_MAX_PIPE_BLOCKS = 256;

    private FluidPipeNetwork() {
    }

    public record Endpoint(
            BlockPos pos,
            Direction side,
            IFluidHandler handler) {
    }

    private record EndpointKey(
            BlockPos pos,
            Direction side) {
    }

    public static List<Endpoint> findEndpoints(
            Level level,
            BlockPos firstPipe,
            @Nullable BlockPos ignoredPos) {
        return findEndpoints(
                level,
                firstPipe,
                ignoredPos,
                DEFAULT_MAX_PIPE_BLOCKS
        );
    }

    public static List<Endpoint> findEndpoints(
            Level level,
            BlockPos firstPipe,
            @Nullable BlockPos ignoredPos,
            int maxPipeBlocks) {
        if (level == null || !isPipe(level.getBlockState(firstPipe))) {
            return List.of();
        }

        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();
        Set<BlockPos> visited =
                new HashSet<>();
        Set<EndpointKey> endpointKeys =
                new HashSet<>();
        List<Endpoint> endpoints =
                new ArrayList<>();

        queue.add(firstPipe.immutable());

        while (!queue.isEmpty()
                && visited.size()
                < Math.max(1, maxPipeBlocks)) {
            BlockPos pipePos =
                    queue.removeFirst();

            if (!visited.add(pipePos)) {
                continue;
            }

            BlockState pipeState = level.getBlockState(pipePos);
            if (!isPipe(pipeState)) {
                continue;
            }

            if (pipeState.getBlock() instanceof DiagonalWaterPipeBlock) {
                BlockPos step = pipeState.getValue(DiagonalWaterPipeBlock.AXIS).step();
                for (int sign = -1; sign <= 1; sign += 2) {
                    if (DiagonalWaterPipeBlock.isConnectedDiagonal(
                            level, pipePos, pipeState, sign)) {
                        BlockPos diagonal = pipePos.offset(
                                step.getX() * sign, step.getY() * sign,
                                step.getZ() * sign);
                        if (!visited.contains(diagonal)) {
                            queue.addLast(diagonal.immutable());
                        }
                    }
                }
            }

            if (pipeState.getBlock() instanceof WaterPipeBlock) {
                BlockPos outgoing = pipeState.getValue(
                        WaterPipeBlock.DIAGONAL).step();
                if (outgoing != null) {
                    BlockPos neighbour = pipePos.offset(
                            outgoing.getX(), outgoing.getY(), outgoing.getZ());
                    if ((ignoredPos == null || !ignoredPos.equals(neighbour))
                            && level.hasChunkAt(neighbour)) {
                        BlockState otherState = level.getBlockState(neighbour);
                        if (otherState.getBlock() instanceof DiagonalWaterPipeBlock) {
                            BlockPos axis = otherState.getValue(DiagonalWaterPipeBlock.AXIS).step();
                            int sign = axis.equals(outgoing) ? -1
                                    : axis.getX() == -outgoing.getX()
                                            && axis.getY() == -outgoing.getY()
                                            && axis.getZ() == -outgoing.getZ() ? 1 : 0;
                            if (sign != 0 && DiagonalWaterPipeBlock.isConnectedDiagonal(
                                    level, neighbour, otherState, sign)
                                    && !visited.contains(neighbour)) {
                                queue.addLast(neighbour.immutable());
                            }
                        }
                    }
                }
            }

            for (Direction direction :
                    Direction.values()) {
                if (pipeState.getBlock() instanceof DiagonalWaterPipeBlock
                        && !DiagonalWaterPipeBlock.hasSocket(pipeState, direction)) {
                    continue;
                }
                BlockPos neighbour = pipePos.relative(direction);

                if (ignoredPos != null
                        && neighbour.equals(ignoredPos)) {
                    continue;
                }

                BlockState neighbourState =
                        level.getBlockState(neighbour);

                if (isPipe(neighbourState)) {
                    // Diagonal pieces have no ordinary side connections unless
                    // the blockstate explicitly includes an endpoint fitting.
                    if (neighbourState.getBlock() instanceof DiagonalWaterPipeBlock
                            && !DiagonalWaterPipeBlock.hasSocket(
                                    neighbourState, direction.getOpposite())) {
                        continue;
                    }
                    if (!visited.contains(neighbour)) {
                        queue.addLast(neighbour.immutable());
                    }
                    continue;
                }

                // Diagonal fittings terminate into ordinary Copper pipes,
                // never directly into tanks or machine capabilities.
                if (pipeState.getBlock() instanceof DiagonalWaterPipeBlock) {
                    continue;
                }

                Direction endpointSide =
                        direction.getOpposite();
                EndpointKey key =
                        new EndpointKey(
                                neighbour.immutable(),
                                endpointSide
                        );

                if (!endpointKeys.add(key)) {
                    continue;
                }

                if (neighbourState.is(BlockRegistry.BOILER_WATER_INLET.get())) {
                    IFluidHandler boiler = BoilerWarmup.waterHandlerForInlet(
                            level, neighbour, neighbourState, pipePos);
                    if (boiler != null) {
                        endpoints.add(new Endpoint(neighbour.immutable(), endpointSide, boiler));
                        continue;
                    }
                }

                BlockEntity blockEntity =
                        level.getBlockEntity(neighbour);

                if (blockEntity == null) {
                    continue;
                }

                IFluidHandler handler =
                        blockEntity
                                .getCapability(
                                        ForgeCapabilities.FLUID_HANDLER,
                                        endpointSide
                                )
                                .orElse(null);

                // A number of mods expose a valid fluid capability only when
                // queried unsided. Respect the connected face first, then fall
                // back to the generic handler just like our item automation.
                if (handler == null) {
                    handler =
                            blockEntity
                                    .getCapability(
                                            ForgeCapabilities.FLUID_HANDLER,
                                            null
                                    )
                                    .orElse(null);
                }

                if (handler != null) {
                    endpoints.add(
                            new Endpoint(
                                    neighbour.immutable(),
                                    endpointSide,
                                    handler
                            )
                    );
                }
            }
        }

        return endpoints;
    }

    private static boolean isPipe(BlockState state) {
        return state.is(BlockRegistry.WATER_PIPE.get())
                || state.is(BlockRegistry.DIAGONAL_WATER_PIPE.get());
    }
}
