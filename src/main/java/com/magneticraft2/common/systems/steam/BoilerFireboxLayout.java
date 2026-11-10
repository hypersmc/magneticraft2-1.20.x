package com.magneticraft2.common.systems.steam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.List;

public final class BoilerFireboxLayout {
    private BoilerFireboxLayout() { }

    public static int turns(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0;
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> throw new IllegalArgumentException("Horizontal boiler facing required");
        };
    }

    public static BlockPos offset(BoilerFireboxPlan.Cell cell, Direction facing) {
        BoilerFireboxPlan.Cell rotated = cell.rotate(turns(facing));
        return new BlockPos(rotated.x(), rotated.y(), rotated.z());
    }
    public static BlockPos front(BlockPos vessel, Direction facing) {
        return vessel.offset(offset(BoilerFireboxPlan.FRONT, facing));
    }
    public static BlockPos vessel(BlockPos front, Direction facing) {
        return front.subtract(offset(BoilerFireboxPlan.FRONT, facing));
    }
    public static BlockPos stack(BlockPos vessel, Direction facing, int section) {
        return vessel.offset(offset(BoilerFireboxPlan.chimney(section), facing));
    }
    public static List<BlockPos> cells(BlockPos vessel, Direction facing) {
        return BoilerFireboxPlan.fireboxCells().stream()
                .map(cell -> vessel.offset(offset(cell, facing))).toList();
    }
    public static boolean loaded(Level level, Iterable<BlockPos> positions) {
        for (BlockPos pos : positions) if (!level.hasChunkAt(pos)) return false;
        return true;
    }
}
