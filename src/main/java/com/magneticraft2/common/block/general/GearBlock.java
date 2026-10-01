package com.magneticraft2.common.block.general;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.GEAR.GearPlacementValidator;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * @version 1.0.0
 */
public abstract class GearBlock extends DirectionalBlock implements EntityBlock {
    public GearBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return createBlockEntity(pos, state);
    }

    protected abstract BlockEntity createBlockEntity(BlockPos pos, BlockState state);

    public int getPlacementGearTeeth(BlockState state) {
        return 8;
    }

    public boolean isShaftLikeForPlacement(BlockState state) {
        return false;
    }

    public Direction.Axis getPlacementGearAxis(BlockState state) {
        if (state != null && state.hasProperty(FACING)) {
            return state.getValue(FACING).getAxis();
        }
        return Direction.Axis.Y;
    }

    protected BlockState validateGearPlacement(BlockPlaceContext context, BlockState placementState) {
        if (placementState == null) {
            return null;
        }

        String invalidReason = GearPlacementValidator.getInvalidPlacementReason(context.getLevel(), context.getClickedPos(), placementState);
        if (invalidReason != null) {
            Player player = context.getPlayer();
            if (player != null && context.getLevel().isClientSide) {
                player.displayClientMessage(Component.literal(invalidReason), true);
            }
            return null;
        }

        return placementState;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GearBlockEntity gearBlockEntity) {
            gearBlockEntity.updateGearNetwork();
        }
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide) {
            GearNetworkManager.getInstance().removeGear(pos, level);
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            GearNetworkManager.getInstance().removeGear(pos, level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
