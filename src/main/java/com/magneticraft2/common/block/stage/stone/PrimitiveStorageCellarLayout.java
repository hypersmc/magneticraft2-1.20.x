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

    private static final double LONG_MIN = -1.94D;
    private static final double LONG_MAX = 0.94D;
    private static final double WEST_MIN = 0.00D;
    private static final double WEST_MAX = 1.00D;

    // Safe X anchors around the two vertical timber posts. The post bands are
    // roughly -1.625..-1.375 and 0.3125..0.5625 in canonical WEST space.
    private static final double[] LONG_SAFE = {
            -1.80D, -1.16D, -0.74D, -0.32D, 0.10D, 0.76D
    };

    private static final List<Slot> SLOTS = buildSlots();

    private PrimitiveStorageCellarLayout() {
    }

    public static List<Slot> slots() {
        return SLOTS;
    }

    @Nullable
    public static Slot findSlot(Wall wall, Shelf shelf, Vec3 localHit) {
        double coordinate = wall == Wall.WEST ? localHit.z : localHit.x;
        double min = wall == Wall.WEST ? WEST_MIN : LONG_MIN;
        double max = wall == Wall.WEST ? WEST_MAX : LONG_MAX;

        if (coordinate < min || coordinate > max) {
            return null;
        }

        Slot closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Slot slot : SLOTS) {
            if (slot.wall() != wall || slot.shelf() != shelf) {
                continue;
            }

            double anchor = wall == Wall.WEST ? slot.renderPosition().z : slot.renderPosition().x;
            double distance = Math.abs(coordinate - anchor);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = slot;
            }
        }

        return closest;
    }

    private static List<Slot> buildSlots() {
        List<Slot> slots = new ArrayList<>(SLOT_COUNT);

        for (Shelf shelf : Shelf.values()) {
            double[] longAnchors = longShelfAnchors(shelf);

            for (double x : longAnchors) {
                slots.add(new Slot(slots.size(), Wall.NORTH, shelf, new Vec3(x, shelf.renderY(), NORTH_Z)));
            }
            for (double x : longAnchors) {
                slots.add(new Slot(slots.size(), Wall.SOUTH, shelf, new Vec3(x, shelf.renderY(), SOUTH_Z)));
            }
            for (int i = 0; i < 4; i++) {
                double z = centerForIndex(i, WEST_MIN, WEST_MAX, 4);
                slots.add(new Slot(slots.size(), Wall.WEST, shelf, new Vec3(WEST_X, shelf.renderY(), z)));
            }
        }

        return Collections.unmodifiableList(slots);
    }

    private static double[] longShelfAnchors(Shelf shelf) {
        return switch (shelf) {
            // LOWER stays completely unchanged.
            case LOWER -> new double[]{
                    LONG_SAFE[0], LONG_SAFE[1], LONG_SAFE[2],
                    LONG_SAFE[3], LONG_SAFE[4], LONG_SAFE[5]
            };

            // Keep the working left-to-right positions. Visibility around
            // the left post is handled by the renderer, not by reordering slots.
            case MIDDLE -> new double[]{
                    LONG_SAFE[0], LONG_SAFE[1], LONG_SAFE[2],
                    LONG_SAFE[3], LONG_SAFE[5], LONG_SAFE[4]
            };

            case UPPER -> new double[]{
                    LONG_SAFE[0], LONG_SAFE[1], LONG_SAFE[2],
                    LONG_SAFE[3], LONG_SAFE[4], LONG_SAFE[5]
            };
        };
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
