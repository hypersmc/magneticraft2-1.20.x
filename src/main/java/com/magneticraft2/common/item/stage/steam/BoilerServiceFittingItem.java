package com.magneticraft2.common.item.stage.steam;

import com.magneticraft2.common.block.stage.steel.SteelBoilerVesselBlock;
import com.magneticraft2.common.block.stage.steam.BoilerServiceFittingBlock;
import com.magneticraft2.common.blockentity.stage.steel.SteelBoilerVesselBlockEntity;
import com.magneticraft2.common.systems.steam.BoilerFireboxInstallation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** One held item installs one real block; V2 chimney sections are installed bottom-up. */
public final class BoilerServiceFittingItem extends BlockItem {
    public BoilerServiceFittingItem(BoilerServiceFittingBlock block, Properties properties) { super(block, properties); }

    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        SteelBoilerVesselBlockEntity vessel = BoilerFireboxInstallation.vesselFromClick(level, context.getClickedPos());
        if (vessel == null) return super.useOn(context); // precise placement remains available
        Player player = context.getPlayer();
        if (!vessel.isFormed()) return fail(level, player, "vessel");
        BoilerServiceFittingBlock fitting = (BoilerServiceFittingBlock) getBlock();
        Direction facing = vessel.getBlockState().getValue(SteelBoilerVesselBlock.FACING);
        BlockPos target;
        if (fitting.role() == BoilerServiceFittingBlock.Role.CHIMNEY && vessel.boilerAssemblyTracked()) {
            BoilerFireboxInstallation.Target result = BoilerFireboxInstallation.nextChimney(level, vessel);
            if (result.position() == null) return fail(level, player, result.failure());
            target = result.position();
        } else {
            target = BoilerServiceFittingBlock.attachmentPosition(vessel.getBlockPos(), fitting.role(), facing);
        }
        if (!level.hasChunkAt(target)) return fail(level, player, "unloaded");
        if (!level.getBlockState(target).isAir()) return fail(level, player, "obstructed");
        if (player == null) return InteractionResult.FAIL;
        // The actual destination must be reachable and permitted, not only the clicked shell.
        double reach = player.getBlockReach();
        double distanceSquared = player.getEyePosition().distanceToSqr(
                net.minecraft.world.phys.Vec3.atCenterOf(target));
        if (distanceSquared > reach * reach) return fail(level, player, "reach");
        if (!player.getAbilities().mayBuild || !level.mayInteract(player, target)
                || !player.mayUseItemAt(target, Direction.UP, context.getItemInHand()))
            return fail(level, player, "protected");
        // Forge's normal use-on wrapper captures the actual changed block and fires placement hooks.
        BlockPlaceContext moved = BlockPlaceContext.at(new BlockPlaceContext(context), target, Direction.UP);
        return place(moved);
    }
    private static InteractionResult fail(Level level, Player player, String reason) {
        if (!level.isClientSide && player != null) player.displayClientMessage(
                Component.translatable("message.magneticraft2.firebox_v2." + reason), true);
        return InteractionResult.FAIL;
    }
}
