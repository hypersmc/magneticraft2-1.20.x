package com.magneticraft2.common.systems.PRESSURE;

import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.blockentity.stage.stone.BellowsMultiblockModuleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bounded, passive air-duct traversal. Mirrors FluidPipeNetwork: the blower
 * performs transfers server-side; pipe blocks have no BE/ticker/save data.
 *
 * K1 transports integer PRESSURE units, not a gas simulation. A branch has no
 * magical throughput multiplier: the driving blower still owns its rate limit.
 * No searching into unloaded chunks, no water/Forge fluid interoperation, and
 * no unsided pressure capability fallback (directional sockets matter).
 */
public final class AirDuctNetwork {
    public static final int MAX_DUCTS = 128;
    private static final Direction[] DIRECTIONS = Direction.values();
    private AirDuctNetwork() {}

    public record Receiver(BlockPos pos, Direction side, IPressureStorage handler) {}

    public static List<Receiver> findReceivers(
            Level level, BlockPos firstDuct, @Nullable BlockPos source) {
        if (level == null || !level.hasChunkAt(firstDuct)
                || !level.getBlockState(firstDuct).is(BlockRegistry.BRONZE_AIR_DUCT.get()))
            return List.of();

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> receiverPositions = new HashSet<>();
        List<Receiver> result = new ArrayList<>();
        queue.add(firstDuct.immutable());

        while (!queue.isEmpty() && visited.size() < MAX_DUCTS) {
            BlockPos p = queue.removeFirst();
            if (!visited.add(p)) continue;
            for (Direction d : DIRECTIONS) {
                BlockPos neighbour = p.relative(d);
                if ((source != null && neighbour.equals(source))
                        || !level.hasChunkAt(neighbour)) continue;

                if (level.getBlockState(neighbour).is(BlockRegistry.BRONZE_AIR_DUCT.get())) {
                    if (!visited.contains(neighbour)) queue.addLast(neighbour.immutable());
                    continue;
                }

                // One receiver per block, even if the duct loops around it.
                if (receiverPositions.contains(neighbour)) continue;
                BlockEntity blockEntity = level.getBlockEntity(neighbour);
                if (blockEntity == null) continue;
                if (blockEntity instanceof BellowsMultiblockModuleEntity bellows
                        && !bellows.isFormedModule()) continue;
                Direction inputFace = d.getOpposite();
                IPressureStorage input = blockEntity
                        .getCapability(CapabilityPressure.PRESSURE, inputFace)
                        .orElse(null);
                if (input != null && input.canReceive()) {
                    receiverPositions.add(neighbour.immutable());
                    result.add(new Receiver(neighbour.immutable(), inputFace, input));
                }
            }
        }
        return result;
    }
}
