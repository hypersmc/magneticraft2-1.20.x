package com.magneticraft2.common.systems.steam;

import java.util.ArrayList;
import java.util.List;

/** Pure coordinate contract shared by the Forge adapter and executable tests. */
public final class BoilerFireboxPlan {
    private BoilerFireboxPlan() { }

    public record Cell(int x, int y, int z) {
        public Cell plus(Cell b) { return new Cell(x + b.x, y + b.y, z + b.z); }
        public Cell minus(Cell b) { return new Cell(x - b.x, y - b.y, z - b.z); }
        /** 0 SOUTH, 1 WEST, 2 NORTH, 3 EAST; rotate about a block's center. */
        public Cell rotate(int quarterTurns) {
            return switch (Math.floorMod(quarterTurns, 4)) {
                case 0 -> this;
                case 1 -> new Cell(-z, y, x);
                case 2 -> new Cell(-x, y, -z);
                default -> new Cell(z, y, -x);
            };
        }
    }

    public static final Cell FRONT = new Cell(0, 1, 2);
    public static final Cell INTERFACE = new Cell(0, 1, 1);
    public static final int STACK_HEIGHT = 3;

    public static Cell chimney(int section) {
        if (section < 0 || section >= STACK_HEIGHT)
            throw new IllegalArgumentException("Chimney section must be 0..2");
        return new Cell(-1, 3 + section, 1);
    }

    /** Installation envelope only; real formation materials come from JSON. */
    public static List<Cell> fireboxCells() {
        List<Cell> result = new ArrayList<>(18);
        for (int y = 0; y < 3; y++)
            for (int z = 1; z <= 2; z++)
                for (int x = -1; x <= 1; x++) result.add(new Cell(x, y, z));
        return List.copyOf(result);
    }

    public static boolean insideOriginalVessel(Cell cell) {
        return cell.x >= -1 && cell.x <= 1 && cell.y >= 0 && cell.y <= 3
                && cell.z >= -4 && cell.z <= 0;
    }
}
