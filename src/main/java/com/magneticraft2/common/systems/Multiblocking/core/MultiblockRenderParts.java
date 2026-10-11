package com.magneticraft2.common.systems.Multiblocking.core;

import com.magneticraft2.common.blockentity.general.Multiblockfiller_tile;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
 * Chunk-baked, block-local V2 render and collision proxies.
 *
 * A multiblock may require empty cells to form, while its assembled machine
 * occupies those cells (crucible, spouts, moving parts). Only positions explicitly
 * listed in the JSON "render_air_anchors" setting become proxy blocks after formation.
 * All proxies save their own model ID, controller and collision snapshot, without
 * requiring the controller's chunk to be loaded for local physics.
 */
public final class MultiblockRenderParts {
    private MultiblockRenderParts() {}

    public static void assign(Level world, BlockPos master,
                              MultiblockController controller, String machineId,
                              Direction facing) {
        assign(world, master, controller, machineId, facing, List.of());
    }

    public static void assign(Level world, BlockPos master,
                              MultiblockController controller, String machineId,
                              Direction facing, List<BlockPos> renderAirAnchors) {
        if (world.isClientSide || controller == null || !facing.getAxis().isHorizontal()) return;

        Map<BlockPos, BlockState> originalBlocks = controller.getBlockMap();
        // Validate everything before placing the first new proxy. Requiring
        // blueprint AIR prevents replacing player-built blocks or other machines.
        Set<BlockPos> newAnchors = new HashSet<>();
        for (BlockPos offset : renderAirAnchors) {
            BlockPos pos = master.offset(offset);
            if (pos.equals(master)
                    || !newAnchors.add(pos)
                    || !originalBlocks.containsKey(pos)
                    || !originalBlocks.get(pos).isAir()
                    || !world.getBlockState(pos).isAir()) {
                throw new IllegalStateException("Invalid V2 air anchor at " + pos + " for " + machineId);
            }
        }

        BlockState controllerState = world.getBlockState(master);
        VoxelShape formedCollision = controllerState.getBlock().getCollisionShape(
                controllerState, world, master, CollisionContext.empty());

        // The structure is already formed. Replace *only* configured empty
        // locations with the same existing non-occluding V2 filler block.
        for (BlockPos pos : newAnchors) {
            if (!world.setBlock(pos, BlockRegistry.multiblockfiller.get().defaultBlockState(), Block.UPDATE_ALL)) {
                throw new IllegalStateException("Unable to place V2 proxy at " + pos);
            }
        }

        for (BlockPos partPos : originalBlocks.keySet()) {
            if (partPos.equals(master)) continue;
            if (!(world.getBlockEntity(partPos) instanceof Multiblockfiller_tile filler)) continue;

            int dx = partPos.getX() - master.getX();
            int dy = partPos.getY() - master.getY();
            int dz = partPos.getZ() - master.getZ();
            String modelKey = machineId + "_" + facing.getName()
                    + "/part_" + dx + "_" + dy + "_" + dz;

            // Clip the controller's physical outline to the local block.
            // Air anchor proxies therefore collide only where actual machinery
            // exists, not across the crucible's deliberately hollow center.
            VoxelShape local = Shapes.join(
                    formedCollision.move(-dx, -dy, -dz),
                    Shapes.block(),
                    BooleanOp.AND).optimize();
            filler.setV2ModelPart(master, modelKey, local);
        }
    }
}
