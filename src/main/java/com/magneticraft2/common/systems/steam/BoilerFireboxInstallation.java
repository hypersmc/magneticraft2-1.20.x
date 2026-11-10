package com.magneticraft2.common.systems.steam;

import com.magneticraft2.common.block.stage.steam.BoilerServiceFittingBlock;
import com.magneticraft2.common.block.stage.steel.SteelBoilerVesselBlock;
import com.magneticraft2.common.blockentity.general.Multiblockfiller_tile;
import com.magneticraft2.common.blockentity.stage.steam.BoilerFireboxBlockEntity;
import com.magneticraft2.common.blockentity.stage.steel.SteelBoilerVesselBlockEntity;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Bounded lookups; never scans a network and never loads a neighbor chunk. */
public final class BoilerFireboxInstallation {
    private BoilerFireboxInstallation() { }
    private static final Direction[] HORIZONTAL = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
    public record Target(@Nullable BlockPos position, @Nullable String failure) { }

    @Nullable public static SteelBoilerVesselBlockEntity vesselFromClick(Level world, BlockPos clicked) {
        if (!world.hasChunkAt(clicked)) return null;
        BlockEntity entity = world.getBlockEntity(clicked);
        if (entity instanceof Multiblockfiller_tile filler) {
            BlockPos master = filler.getControllerPos();
            if (!world.hasChunkAt(master)) return null;
            entity = world.getBlockEntity(master);
        }
        if (entity instanceof SteelBoilerVesselBlockEntity vessel) return vessel;
        if (entity instanceof BoilerFireboxBlockEntity firebox && firebox.isFormed()
                && firebox.vesselPosition() != null && world.hasChunkAt(firebox.vesselPosition())
                && world.getBlockEntity(firebox.vesselPosition()) instanceof SteelBoilerVesselBlockEntity vessel
                && vessel.boilerAssemblyId().equals(firebox.vesselIdentity())) return vessel;
        return null;
    }

    @Nullable public static BoilerFireboxBlockEntity linkedFirebox(Level world, SteelBoilerVesselBlockEntity vessel) {
        BoilerAssemblyHost.Link link = vessel.boilerChild();
        if (link == null || !world.hasChunkAt(link.position())) return null;
        if (world.getBlockEntity(link.position()) instanceof BoilerFireboxBlockEntity firebox
                && firebox.isFormed() && firebox.boilerAssemblyId().equals(link.identity())
                && vessel.getBlockPos().equals(firebox.vesselPosition())
                && vessel.boilerAssemblyId().equals(firebox.vesselIdentity())) return firebox;
        return null;
    }

    /** V2 stack location, not the old one-cell post above the interface fitting. */
    public static Target nextChimney(Level world, SteelBoilerVesselBlockEntity vessel) {
        if (vessel.boilerChild() == null) return new Target(null, "firebox_first");
        BoilerFireboxBlockEntity front = linkedFirebox(world, vessel);
        if (front == null) return new Target(null, "unloaded");
        Direction facing = front.assemblyFacing();
        for (int section = 0; section < BoilerFireboxPlan.STACK_HEIGHT; section++) {
            BlockPos pos = BoilerFireboxLayout.stack(vessel.getBlockPos(), facing, section);
            if (!world.hasChunkAt(pos)) return new Target(null, "unloaded");
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) {
                for (int upper = section + 1; upper < BoilerFireboxPlan.STACK_HEIGHT; upper++) {
                    BlockPos above = BoilerFireboxLayout.stack(vessel.getBlockPos(), facing, upper);
                    if (!world.hasChunkAt(above)) return new Target(null, "unloaded");
                    if (!world.getBlockState(above).isAir()) return new Target(null, "stack_gap");
                }
                return new Target(pos, null);
            }
            if (!isStackSection(state, facing)) return new Target(null, "obstructed");
        }
        return new Target(null, "stack_full");
    }
    private static boolean isStackSection(BlockState state, Direction facing) {
        return state.is(BlockRegistry.BOILER_FLUE_CHIMNEY.get())
                && state.getValue(BoilerServiceFittingBlock.FACING) == facing;
    }

    @Nullable public static BoilerFireboxBlockEntity stackOwner(Level world, BlockPos target) {
        for (Direction facing : HORIZONTAL) for (int section = 0; section < BoilerFireboxPlan.STACK_HEIGHT; section++) {
            BlockPos vesselPos = target.subtract(BoilerFireboxLayout.offset(BoilerFireboxPlan.chimney(section), facing));
            BlockPos frontPos = BoilerFireboxLayout.front(vesselPos, facing);
            if (!world.hasChunkAt(frontPos)) continue;
            if (!(world.getBlockEntity(frontPos) instanceof BoilerFireboxBlockEntity front)
                    || !front.isFormed() || front.assemblyFacing() != facing
                    || !vesselPos.equals(front.vesselPosition())) continue;
            return front;
        }
        return null;
    }
    @Nullable public static BlockState stackPlacement(BlockPlaceContext context, BoilerServiceFittingBlock block) {
        Level world = context.getLevel(); BlockPos target = context.getClickedPos();
        BoilerFireboxBlockEntity front = stackOwner(world, target);
        if (front == null || front.vesselPosition() == null) return null;
        BlockPos vesselPos = front.vesselPosition();
        if (!world.hasChunkAt(vesselPos)
                || !(world.getBlockEntity(vesselPos) instanceof SteelBoilerVesselBlockEntity vessel)
                || !vessel.isFormed() || !vessel.boilerAssemblyId().equals(front.vesselIdentity())) return null;
        Direction facing = front.assemblyFacing();
        for (int section = 0; section < BoilerFireboxPlan.STACK_HEIGHT; section++) {
            BlockPos pos = BoilerFireboxLayout.stack(vesselPos, facing, section);
            if (!world.hasChunkAt(pos)) return null;
            if (pos.equals(target)) {
                BlockPos below = target.below();
                if (!world.hasChunkAt(below)) return null;
                if (section == 0) {
                    if (!(world.getBlockEntity(below) instanceof Multiblockfiller_tile filler)
                            || !front.getBlockPos().equals(filler.getControllerPos())
                            || !front.boilerAssemblyId().equals(filler.getBoilerAssemblyIdentity())) return null;
                } else if (!isStackSection(world.getBlockState(below), facing)) return null;
                return block.defaultBlockState().setValue(BoilerServiceFittingBlock.FACING, facing);
            }
            if (!isStackSection(world.getBlockState(pos), facing)) return null;
        }
        return null;
    }

    public static void placed(Level world, BlockPos pos, BlockState state, BoilerServiceFittingBlock.Role role) {
        if (!(world instanceof ServerLevel server)) return;
        if (role == BoilerServiceFittingBlock.Role.CHIMNEY) {
            BoilerFireboxBlockEntity firebox = stackOwner(world, pos);
            if (firebox != null) BoilerAssemblyRecovery.trackAttachment(server, firebox, pos, state);
            return;
        }
        // A newly installed interface is owned by the root until formation
        // replaces it with a child filler; onRemove releases its old stamp.
        Direction facing = state.getValue(BoilerServiceFittingBlock.FACING);
        BlockPos offset = BoilerServiceFittingBlock.attachmentPosition(BlockPos.ZERO, role, facing);
        BlockPos vesselPos = pos.subtract(offset);
        if (world.hasChunkAt(vesselPos)
                && world.getBlockEntity(vesselPos) instanceof SteelBoilerVesselBlockEntity vessel
                && vessel.isFormed() && vessel.boilerAssemblyTracked())
            BoilerAssemblyRecovery.trackAttachment(server, vessel, pos, state);
    }
}
