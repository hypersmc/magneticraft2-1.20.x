package com.magneticraft2.common.systems.Multiblocking.core;

import net.minecraft.core.Direction;

/**
 * Baked Steel multiblock palette, selected once when the machine forms.
 * Offsets follow the controller-relative SOUTH JSON blueprint. Kept separate
 * from rendering so no geometry is constructed in BER/tick paths.
 */
public final class SteelProcessModelParts {
    private SteelProcessModelParts() {}

    public static String key(String machine, Direction facing, int dx, int dy,
                             int dz, boolean originalAir, boolean foundation) {
        if (!facing.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("Steel model facing must be horizontal");
        }
        if (foundation) return "steel_process/foundation";

        // Transform world offsets to the canonical SOUTH layout.
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
        boolean coke = machine.equals("steel_coke_oven");
        int half = coke ? 1 : 2;
        int x = cx + half;
        int z = cz + 2 * half;
        String front = facing.getName();

        if (originalAir) {
            if (dy != 1 || x != half || z != 2 * half || coke) {
                throw new IllegalStateException("Unexpected Steel air model: "
                        + machine + " " + dx + "," + dy + "," + dz);
            }
            return "steel_process/" + (machine.equals("steel_blast_furnace")
                    ? "blast_hearth_" : "refine_hearth_") + front;
        }
        String part;
        if (coke) {
            if (dy == 3) {
                if (z == 2 && x == 1) part = "coke_cap_" + front;
                else if (z == 2) part = "coke_roof_corner_" + front;
                else part = topRing(facing, x, z, 1);
            } else if (z == 2) {
                part = x == 1 ? (dy == 1 ? "coke_lower_" : "coke_upper_") + front
                        : "front_corner_" + front;
            } else if (x == 0 || x == 2) {
                part = "side_panel_" + outward(facing, x == 0).getName();
            } else {
                part = "refractory";
            }
        } else if (machine.equals("steel_blast_furnace")) {
            if (dy == 1) {
                if (z == 4) {
                    part = x == 1 ? "blast_jamb_left_" + front
                            : x == 3 ? "blast_jamb_right_" + front
                            : "front_corner_" + front;
                } else if (x == 0 || x == 4) {
                    part = "side_panel_" + outward(facing, x == 0).getName();
                } else part = "refractory";
            } else if (dy == 2) {
                part = z == 4 ? (x == 2 ? "blast_lintel_" : "blast_ledge_") + front
                        : "roof";
            } else if (dy == 3) {
                if (z == 3) part = (x == 2 ? "blast_shaft_vent_" : "shaft_column_") + front;
                else if (x == 1 || x == 3)
                    part = "side_panel_" + outward(facing, x == 1).getName();
                else part = "shaft_wall";
            } else if (dy == 4) {
                part = z == 3 && x == 2 ? "blast_shaft_flue_" + front
                        : topRing(facing, x, z, 2);
            } else part = "refractory";
        } else if (machine.equals("steel_refining_furnace")) {
            if (dy == 1) {
                if (z == 4) {
                    part = x == 1 ? "refine_wing_left_" + front
                            : x == 3 ? "refine_wing_right_" + front
                            : "front_corner_" + front;
                } else if (x == 0 || x == 4) {
                    part = "side_panel_" + outward(facing, x == 0).getName();
                } else part = "refractory";
            } else if (dy == 2) {
                if (z == 4) part = (x >= 1 && x <= 3
                        ? "refine_vent_" : "front_corner_") + front;
                else if (x == 0 || x == 4)
                    part = "side_panel_" + outward(facing, x == 0).getName();
                else part = "refractory";
            } else if (dy == 3) {
                part = z == 4 ? "refine_roof_cornice_" + front : "roof";
            } else if (dy == 4) {
                part = z == 3 && x == 2 ? "refine_hood_vent_" + front
                        : topRing(facing, x, z, 2);
            } else part = "refractory";
        } else {
            throw new IllegalArgumentException("Unknown Steel process: " + machine);
        }
        return "steel_process/" + part;
    }

    private static Direction outward(Direction facing, boolean lowerX) {
        return lowerX ? facing.getClockWise() : facing.getCounterClockWise();
    }

    /** The center is real air; the four neighbors line the open flue. */
    private static String topRing(Direction facing, int x, int z, int mid) {
        Direction inner;
        if (x == mid && z == mid - 1) inner = facing;
        else if (x == mid && z == mid + 1) inner = facing.getOpposite();
        else if (x == mid - 1 && z == mid) inner = facing.getCounterClockWise();
        else if (x == mid + 1 && z == mid) inner = facing.getClockWise();
        else return "stack_corner";
        return "stack_inner_" + inner.getName();
    }
}
