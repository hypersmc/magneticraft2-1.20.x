package com.magneticraft2.common.item.stage.steam;

import com.magneticraft2.common.block.stage.steel.SteelBoilerVesselBlock;
import com.magneticraft2.common.block.stage.steam.BoilerServiceFittingBlock;
import com.magneticraft2.common.blockentity.general.Multiblockfiller_tile;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Boiler service hardware installs by using it on ANY formed boiler part.
 * The authoritative controller/facing determines the exact attachment cell.
 * Delegate to vanilla BlockItem.place so protection hooks, collision,
 * item consumption, sound and block placement events remain intact.
 */
public final class BoilerServiceFittingItem extends BlockItem {
    public BoilerServiceFittingItem(BoilerServiceFittingBlock block, Properties properties) {
        super(block, properties);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockEntity selected = level.getBlockEntity(clicked);
        BlockPos controllerPos;
        if (selected instanceof Multiblockfiller_tile filler) {
            controllerPos = filler.getControllerPos();
        } else if (level.getBlockState(clicked).is(BlockRegistry.STEEL_BOILER_VESSEL.get())) {
            controllerPos = clicked;
        } else {
            // Normal precise placement still works at an intended free position.
            return super.useOn(context);
        }

        if (!level.hasChunkAt(controllerPos)) return InteractionResult.FAIL;
        BlockState controllerState = level.getBlockState(controllerPos);
        Player player = context.getPlayer();
        if (!controllerState.is(BlockRegistry.STEEL_BOILER_VESSEL.get())
                || !controllerState.getValue(SteelBoilerVesselBlock.IS_FORMED)) {
            if (player != null && !level.isClientSide)
                player.displayClientMessage(Component.translatable(
                        "message.magneticraft2.boiler_service_not_formed"), true);
            return InteractionResult.FAIL;
        }

        BoilerServiceFittingBlock fitting = (BoilerServiceFittingBlock) getBlock();
        Direction facing = controllerState.getValue(SteelBoilerVesselBlock.FACING);
        BlockPos target = BoilerServiceFittingBlock.attachmentPosition(
                controllerPos, fitting.role(), facing);

        // Only place into a genuinely empty and loaded external cell. In
        // particular, never replace an existing fitting or a boiler filler.
        if (!level.hasChunkAt(target) || !level.getBlockState(target).isAir()) {
            if (player != null && !level.isClientSide)
                player.displayClientMessage(Component.translatable(
                        "message.magneticraft2.boiler_service_obstructed"), true);
            return InteractionResult.FAIL;
        }

        BlockPlaceContext moved = BlockPlaceContext.at(
                new BlockPlaceContext(context), target, Direction.UP);
        // The block itself rechecks exact controller orientation and contact.
        return place(moved);
    }
}
