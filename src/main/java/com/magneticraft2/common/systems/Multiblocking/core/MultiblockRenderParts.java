package com.magneticraft2.common.systems.Multiblocking.core;

import com.magneticraft2.common.blockentity.general.Multiblockfiller_tile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Generic V2 static-part assignment. Block-model names are deterministic:
 * multiblock/<id>_<facing>/part_<dx>_<dy>_<dz>
 *
 * The generator uses the same controller-relative offset contract. The
 * original JSON multiblock remains authoritative for formation and drops.
 */
public final class MultiblockRenderParts {
    private MultiblockRenderParts() {}

    public static void assign(Level world, BlockPos master,
                              MultiblockController controller, String machineId,
                              Direction facing) {
        if (world.isClientSide || controller == null || !facing.getAxis().isHorizontal()) return;

        BlockState controllerState = world.getBlockState(master);
        VoxelShape formedCollision = controllerState.getBlock().getCollisionShape(
                controllerState, world, master, CollisionContext.empty());

        for (BlockPos partPos : controller.getBlockMap().keySet()) {
            if (partPos.equals(master)) continue;
            if (!(world.getBlockEntity(partPos) instanceof Multiblockfiller_tile filler)) continue;

            int dx = partPos.getX() - master.getX();
            int dy = partPos.getY() - master.getY();
            int dz = partPos.getZ() - master.getZ();
            String modelKey = machineId + "_" + facing.getName()
                    + "/part_" + dx + "_" + dy + "_" + dz;

            // One-time local collision snapshot. Querying collision after a
            // chunk reload does not depend on the controller being loaded.
            VoxelShape local = Shapes.join(
                    formedCollision.move(-dx, -dy, -dz),
                    Shapes.block(),
                    BooleanOp.AND).optimize();
            filler.setV2ModelPart(modelKey, local);
        }
    }
}
