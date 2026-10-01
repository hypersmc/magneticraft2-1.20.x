package com.magneticraft2.common.block.stage.stone;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Canonical WEST-facing physical slot layout for the Primitive Storage Cellar.
 *
 * The same definitions are used by interaction and rendering so a visible item
 * can never disagree with the inventory slot that was clicked.
 */
public final class PrimitiveStorageCellarLayout {
    public static final int SLOT_COUNT = 48;

    public enum Wall {
        NORTH,
        SOUTH,
        WEST
    }

    public enum Shelf {
        LOWER(-0.90D),
        MIDDLE(-0.275D),
        UPPER(0.35D);

        private final double renderY;

        Shelf(double renderY) {
            this.renderY = renderY;
        }

        public double renderY() {
            return renderY;
        }
    }

    public record Slot(int index, Wall wall, Shelf shelf, Vec3 renderPosition) {
    }

    private static final double NORTH_Z = -0.45D;
    private static final double SOUTH_Z = 1.45D;
    private static final double WEST_X = -1.45D;

    private static final double LONG_MIN = -1.80D;
    private static final double LONG_MAX = 0.80D;
    private static final double WEST_MIN = 0.12D;
    private static final double WEST_MAX = 0.88D;

    private static final List<Slot> SLOTS = buildSlots();

    private PrimitiveStorageCellarLayout() {
    }

    public static List<Slot> slots() {
        return SLOTS;
    }

    @Nullable
    public static Slot findSlot(Wall wall, Shelf shelf, Vec3 localHit) {
        int localIndex = switch (wall) {
            case NORTH, SOUTH -> indexForCoordinate(localHit.x, LONG_MIN, LONG_MAX, 6);
            case WEST -> indexForCoordinate(localHit.z, WEST_MIN, WEST_MAX, 4);
        };

        if (localIndex < 0) {
            return null;
        }

        int shelfBase = shelf.ordinal() * 16;
        int wallOffset = switch (wall) {
            case NORTH -> 0;
            case SOUTH -> 6;
            case WEST -> 12;
        };

        return SLOTS.get(shelfBase + wallOffset + localIndex);
    }

    private static List<Slot> buildSlots() {
        List<Slot> slots = new ArrayList<>(SLOT_COUNT);

        for (Shelf shelf : Shelf.values()) {
            for (int i = 0; i < 6; i++) {
                double x = centerForIndex(i, LONG_MIN, LONG_MAX, 6);
                slots.add(new Slot(slots.size(), Wall.NORTH, shelf, new Vec3(x, shelf.renderY(), NORTH_Z)));
            }
            for (int i = 0; i < 6; i++) {
                double x = centerForIndex(i, LONG_MIN, LONG_MAX, 6);
                slots.add(new Slot(slots.size(), Wall.SOUTH, shelf, new Vec3(x, shelf.renderY(), SOUTH_Z)));
            }
            for (int i = 0; i < 4; i++) {
                double z = centerForIndex(i, WEST_MIN, WEST_MAX, 4);
                slots.add(new Slot(slots.size(), Wall.WEST, shelf, new Vec3(WEST_X, shelf.renderY(), z)));
            }
        }

        return Collections.unmodifiableList(slots);
    }

    private static int indexForCoordinate(double value, double min, double max, int divisions) {
        if (value < min || value > max) {
            return -1;
        }

        double normalized = (value - min) / (max - min);
        int index = (int) Math.floor(normalized * divisions);
        return Math.min(index, divisions - 1);
    }

    private static double centerForIndex(int index, double min, double max, int divisions) {
        double width = (max - min) / divisions;
        return min + width * (index + 0.5D);
    }
}
