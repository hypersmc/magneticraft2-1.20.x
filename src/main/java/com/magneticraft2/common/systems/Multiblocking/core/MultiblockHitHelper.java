package com.magneticraft2.common.systems.Multiblocking.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Helpers for physical interactions with formed multiblock models.
 *
 * Coordinates are kept relative to the controller block and can span multiple
 * blocks. Rotation is around the controller block center, so this also works
 * for hit positions outside the normal 0..1 block bounds.
 */
public final class MultiblockHitHelper {
    private MultiblockHitHelper() {
    }

    public static Vec3 relativeToController(BlockHitResult hit, BlockPos controllerPos) {
        return new Vec3(
                hit.getLocation().x - controllerPos.getX(),
                hit.getLocation().y - controllerPos.getY(),
                hit.getLocation().z - controllerPos.getZ()
        );
    }

    /**
     * Converts a horizontally rotated hit back into the model's canonical
     * orientation. The current Storage Cellar model is authored facing WEST.
     */
    /**
     * Converts canonical WEST-authored coordinates into the currently formed
     * horizontal orientation. This is the inverse of {@link #toCanonicalWest}.
     */
    public static Vec3 fromCanonicalWest(Vec3 canonical, Direction formedFacing) {
        double x = canonical.x;
        double y = canonical.y;
        double z = canonical.z;

        return switch (formedFacing) {
            case WEST -> new Vec3(x, y, z);
            case EAST -> new Vec3(1.0D - x, y, 1.0D - z);
            case NORTH -> new Vec3(1.0D - z, y, x);
            case SOUTH -> new Vec3(z, y, 1.0D - x);
            default -> new Vec3(x, y, z);
        };
    }

    public static Vec3 toCanonicalWest(Vec3 relativeHit, Direction formedFacing) {
        double x = relativeHit.x;
        double y = relativeHit.y;
        double z = relativeHit.z;

        return switch (formedFacing) {
            case WEST -> new Vec3(x, y, z);
            case EAST -> new Vec3(1.0D - x, y, 1.0D - z);
            case NORTH -> new Vec3(z, y, 1.0D - x);
            case SOUTH -> new Vec3(1.0D - z, y, x);
            default -> new Vec3(x, y, z);
        };
    }
}
