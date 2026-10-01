package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static com.magneticraft2.common.block.stage.copper.LargeGearWithHandleBlock_wood.ACTIVE;
import static com.magneticraft2.common.block.stage.copper.LargeGearWithHandleBlock_wood.VERTICAL_FACING_down;
import static com.magneticraft2.common.block.stage.copper.LargeGearWithHandleBlock_wood.VERTICAL_FACING_up;
import static net.minecraft.world.level.block.DirectionalBlock.FACING;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * @version 1.0.0
 */
public class LargeGearWithHandleBlockEntity_wood extends GearBlockEntity {
    private boolean isMoving = false;
    private int activeTicks = 0;

    public LargeGearWithHandleBlockEntity_wood(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GEAR_LARGE_WITH_HANDLE_BE_WOOD.get(), pos, state);
    }

    public void handleRightClick() {
        if (level == null || level.isClientSide) {
            return;
        }

        isMoving = true;
        activeTicks = 100;
        setSource(true, 130.0F, getDefaultSourceTorque());
        updateActiveState(true);
    }

    public static <E extends BlockEntity> void serverTick(Level level, BlockPos pos, BlockState estate, E e) {
        if (level.isClientSide) {
            return;
        }

        if (level.getBlockEntity(pos) instanceof LargeGearWithHandleBlockEntity_wood entity) {
            entity.tickHandleSource();
            entity.serverTickGear();
            entity.updateActiveState(entity.shouldRenderGearWithBlockEntity());
        }
    }

    private void tickHandleSource() {
        if (!isMoving) {
            return;
        }

        activeTicks--;
        if (activeTicks <= 0) {
            activeTicks = 0;
            isMoving = false;
            setSource(false, 0.0F, 0.0F);
        }
    }

    private void updateActiveState(boolean active) {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState currentState = level.getBlockState(worldPosition);
        if (currentState.hasProperty(ACTIVE) && currentState.getValue(ACTIVE) != active) {
            level.setBlock(worldPosition, currentState.setValue(ACTIVE, active), 2);
        }
    }

    @Override
    public int getGearTeeth() {
        return 16;
    }

    @Override
    public float getGearMaxTorque() {
        return 16.0F;
    }

    @Override
    public float getDefaultSourceTorque() {
        return 8.0F;
    }

    @Override
    public Direction.Axis getGearAxis() {
        BlockState state = getBlockState();
        if (state.hasProperty(VERTICAL_FACING_up) && state.hasProperty(VERTICAL_FACING_down)
                && (state.getValue(VERTICAL_FACING_up) || state.getValue(VERTICAL_FACING_down))) {
            return Direction.Axis.Y;
        }
        return state.hasProperty(FACING) ? state.getValue(FACING).getAxis() : Direction.Axis.Y;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        isMoving = tag.getBoolean("IsMoving");
        activeTicks = tag.getInt("ActiveTicks");
        if (!isMoving || activeTicks <= 0) {
            getOrCreateGearNode().setSource(false);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("IsMoving", isMoving);
        tag.putInt("ActiveTicks", activeTicks);
    }
}
