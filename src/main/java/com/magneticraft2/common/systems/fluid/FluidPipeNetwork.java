package com.magneticraft2.common.systems.fluid;

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
        if (level == null
                || !level.getBlockState(firstPipe).is(
                        BlockRegistry.WATER_PIPE.get()
                )) {
            return List.of();
        }

        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();
        Set<BlockPos> visited =
                new HashSet<>();
        Set<BlockPos> endpointPositions =
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

            for (Direction direction :
                    Direction.values()) {
                BlockPos neighbour =
                        pipePos.relative(direction);

                if (ignoredPos != null
                        && neighbour.equals(ignoredPos)) {
                    continue;
                }

                BlockState neighbourState =
                        level.getBlockState(neighbour);

                if (neighbourState.is(
                        BlockRegistry.WATER_PIPE.get()
                )) {
                    if (!visited.contains(neighbour)) {
                        queue.addLast(
                                neighbour.immutable()
                        );
                    }
                    continue;
                }

                if (!endpointPositions.add(
                        neighbour.immutable()
                )) {
                    continue;
                }

                BlockEntity blockEntity =
                        level.getBlockEntity(neighbour);

                if (blockEntity == null) {
                    continue;
                }

                Direction endpointSide =
                        direction.getOpposite();

                blockEntity
                        .getCapability(
                                ForgeCapabilities.FLUID_HANDLER,
                                endpointSide
                        )
                        .resolve()
                        .ifPresent(
                                handler -> endpoints.add(
                                        new Endpoint(
                                                neighbour.immutable(),
                                                endpointSide,
                                                handler
                                        )
                                )
                        );
            }
        }

        return endpoints;
    }
}
