package com.magneticraft2.common.systems.Multiblocking.core;

import com.magneticraft2.common.blockentity.general.Multiblockfiller_tile;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * V2 static render/physics proxies: each controller/filler owns its local model
 * and voxel snapshots. A voxel profile is optional: legacy machines preserve
 * the previous controller-defined physical outline.
 */
public final class MultiblockRenderParts {
    private MultiblockRenderParts() {}

    public static void assign(Level world, BlockPos master,
                              MultiblockController controller, String machineId,
                              Direction facing) {
        assign(world, master, controller, machineId, facing, List.of(), "");
    }

    public static void assign(Level world, BlockPos master,
                              MultiblockController controller, String machineId,
                              Direction facing, List<BlockPos> renderAirAnchors) {
        assign(world, master, controller, machineId, facing, renderAirAnchors, "");
    }

    public static void assign(Level world, BlockPos master,
                              MultiblockController controller, String machineId,
                              Direction facing, List<BlockPos> renderAirAnchors,
                              String voxelProfileId) {
        if (world.isClientSide || controller == null || !facing.getAxis().isHorizontal()) return;

        Map<BlockPos, BlockState> originalBlocks = controller.getBlockMap();
        // Required-air anchors must belong to the original JSON blueprint.
        // Check all of them BEFORE replacing any block.
        Set<BlockPos> newAnchors = new HashSet<>();
        for (BlockPos offset : renderAirAnchors) {
            BlockPos pos = master.offset(offset);
            if (pos.equals(master) || !newAnchors.add(pos)
                    || !originalBlocks.containsKey(pos)
                    || !originalBlocks.get(pos).isAir()
                    || !world.getBlockState(pos).isAir()) {
                throw new IllegalStateException("Invalid V2 air anchor at " + pos
                        + " for " + machineId);
            }
        }

        BlockState controllerState = world.getBlockState(master);
        VoxelShape formedCollision;
        VoxelShape formedSelection;
        if (!voxelProfileId.isBlank()) {
            // Profiles are loaded and cached by the server's datapack reload
            // listener. Silent fallback is unsafe for opted-in machines.
            formedCollision = MultiblockVoxelProfiles.collision(voxelProfileId, facing);
            formedSelection = MultiblockVoxelProfiles.selection(voxelProfileId, facing);
            if (formedCollision == null || formedSelection == null
                    || !(world.getBlockEntity(master) instanceof MultiblockVoxelHost)) {
                throw new IllegalStateException("Missing/unusable Voxel V2 profile "
                        + voxelProfileId + " on controller " + master);
            }
        } else {
            formedCollision = controllerState.getBlock().getCollisionShape(
                    controllerState, world, master, CollisionContext.empty());
            formedSelection = formedCollision;
        }

        // Place only required-air proxies, leaving other hollow cells as air.
        for (BlockPos pos : newAnchors) {
            if (!world.setBlock(pos,
                    BlockRegistry.multiblockfiller.get().defaultBlockState(),
                    Block.UPDATE_ALL)) {
                throw new IllegalStateException("Unable to place V2 proxy at " + pos);
            }
        }

        // The controller itself must own a *block-local* outline, not one
        // huge shape overlapping adjacent chunks and their filler collision.
        if (!voxelProfileId.isBlank()
                && world.getBlockEntity(master) instanceof MultiblockVoxelHost voxelHost) {
            voxelHost.setMultiblockVoxelShapes(
                    clip(formedCollision, BlockPos.ZERO, Shapes.block()),
                    clip(formedSelection, BlockPos.ZERO, Shapes.block()));
        }

        for (BlockPos partPos : originalBlocks.keySet()) {
            if (partPos.equals(master)) continue;
            if (!(world.getBlockEntity(partPos) instanceof Multiblockfiller_tile filler)) {
                // Some legacy structure cells can be genuine modules, not fillers.
                continue;
            }

            int dx = partPos.getX() - master.getX();
            int dy = partPos.getY() - master.getY();
            int dz = partPos.getZ() - master.getZ();
            String modelKey = machineId + "_" + facing.getName()
                    + "/part_" + dx + "_" + dy + "_" + dz;

            // Preserve at most half a block of actual physical protrusion from
            // explicitly specified AIR anchors. Ordinary filler collision and
            // ALL targeting/selection remains contained within each local cell.
            VoxelShape collisionClip = newAnchors.contains(partPos)
                    ? Shapes.box(-0.5D, 0D, -0.5D, 1.5D, 1D, 1.5D)
                    : Shapes.block();
            BlockPos offset = new BlockPos(dx,dy,dz);
            VoxelShape localCollision = clip(formedCollision, offset, collisionClip);
            VoxelShape localSelection = clip(formedSelection, offset, Shapes.block());
            filler.setV2ModelPart(master, modelKey, localCollision, localSelection);
        }
    }

    private static VoxelShape clip(VoxelShape original, BlockPos offset,
                                    VoxelShape localBounds) {
        return Shapes.join(original.move(-offset.getX(), -offset.getY(), -offset.getZ()),
                localBounds, BooleanOp.AND).optimize();
    }
}
