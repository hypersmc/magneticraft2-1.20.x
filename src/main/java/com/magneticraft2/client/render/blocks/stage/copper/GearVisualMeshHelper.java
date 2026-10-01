package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.systems.GEAR.GearPlacementValidator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Client-side visual-only helpers for gear rendering.
 *
 * The mechanical system is grid based, but some valid meshes use a larger block-center
 * distance than the models visually suggest. Do not scale the gear models to hide this,
 * because scaling makes the teeth larger and can create worse tooth clipping. Instead this
 * helper applies a tiny render-only center nudge toward valid long-distance mesh neighbors.
 *
 * This does not change placement, collision, networking, or mechanical calculations.
 */
public final class GearVisualMeshHelper {
    private static final int SCAN_RADIUS = 3;

    /**
     * Render-only offset for medium gears meshing at distance 2, usually medium-large.
     */
    private static final double MEDIUM_LONG_MESH_NUDGE = 0.24D;

    /**
     * Render-only offset for large gears meshing at distance 2.
     */
    private static final double LARGE_LONG_MESH_NUDGE = 0.14D;

    private static final double MIN_OFFSET = 0.0001D;

    private GearVisualMeshHelper() {
    }

    /**
     * Applies a small render-only center offset toward valid longer-distance gear meshes.
     *
     * Call this after translating to the gear center and before applying spin rotation. This
     * keeps the gear rotating around its visually nudged center without changing the block
     * position or network math.
     */
    public static void applyConnectedGearVisualOffset(GearBlockEntity blockEntity, PoseStack stack) {
        double[] offset = getConnectedGearVisualOffset(blockEntity);
        if (Math.abs(offset[0]) < MIN_OFFSET && Math.abs(offset[1]) < MIN_OFFSET && Math.abs(offset[2]) < MIN_OFFSET) {
            return;
        }

        stack.translate(offset[0], offset[1], offset[2]);
    }

    private static double[] getConnectedGearVisualOffset(GearBlockEntity blockEntity) {
        double[] offset = new double[]{0.0D, 0.0D, 0.0D};
        if (blockEntity == null || blockEntity.isShaftLike()) {
            return offset;
        }

        Level level = blockEntity.getLevel();
        if (level == null) {
            return offset;
        }

        BlockPos pos = blockEntity.getBlockPos();
        Direction.Axis axis = blockEntity.getGearAxis();
        int teeth = blockEntity.getGearTeeth();
        double nudge = getLongMeshNudgeFor(teeth);
        int matches = 0;

        for (BlockPos scanPosMutable : BlockPos.betweenClosed(
                pos.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS),
                pos.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS))) {

            BlockPos scanPos = scanPosMutable.immutable();
            if (scanPos.equals(pos)) {
                continue;
            }

            BlockEntity neighborEntity = level.getBlockEntity(scanPos);
            if (!(neighborEntity instanceof GearBlockEntity neighbor) || neighbor.isShaftLike()) {
                continue;
            }

            if (neighbor.getGearAxis() != axis) {
                continue;
            }

            if (!GearPlacementValidator.isOffsetInGearPlane(pos, scanPos, axis)) {
                continue;
            }

            if (!GearPlacementValidator.isStraightPlanarOffset(pos, scanPos, axis)) {
                continue;
            }

            int requiredDistance = GearPlacementValidator.getRequiredMeshDistance(teeth, neighbor.getGearTeeth());
            int planarDistance = GearPlacementValidator.getPlanarChebyshevDistance(pos, scanPos, axis);
            if (planarDistance != requiredDistance || requiredDistance <= 1) {
                continue;
            }

            int dx = Integer.compare(scanPos.getX(), pos.getX());
            int dy = Integer.compare(scanPos.getY(), pos.getY());
            int dz = Integer.compare(scanPos.getZ(), pos.getZ());

            offset[0] += dx * nudge;
            offset[1] += dy * nudge;
            offset[2] += dz * nudge;
            matches++;
        }

        if (matches == 1) {
            return offset;
        }

        // Do not nudge a gear that has multiple long-distance mesh neighbors.
        // A single render center cannot move toward several neighbors at once without
        // making the gear look disconnected from some sides or clipping into others.
        // Keep multi-connected gears centered; the single-neighbor gears around them
        // can still nudge inward individually.
        return new double[]{0.0D, 0.0D, 0.0D};
    }

    private static double getLongMeshNudgeFor(int teeth) {
        if (teeth <= 8) {
            return MEDIUM_LONG_MESH_NUDGE;
        }
        return LARGE_LONG_MESH_NUDGE;
    }
}
