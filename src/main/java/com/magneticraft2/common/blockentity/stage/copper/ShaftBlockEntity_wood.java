package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static com.magneticraft2.common.block.stage.copper.ShaftBlock_wood.ROTATING;

/**
 * Simple shaft/axle block entity for Gear V2.
 */
public class ShaftBlockEntity_wood extends GearBlockEntity {
    public ShaftBlockEntity_wood(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.SHAFT_BE_WOOD.get(), pos, state);
    }

    public static <E extends BlockEntity> void serverTick(Level level, BlockPos pos, BlockState state, E blockEntity) {
        if (level.isClientSide) {
            return;
        }

        if (level.getBlockEntity(pos) instanceof ShaftBlockEntity_wood entity) {
            entity.serverTickGear();
            entity.updateRotatingState();
        }
    }

    private void updateRotatingState() {
        if (level == null || level.isClientSide) {
            return;
        }

        markHasEverRotatedIfMoving(getServerSpeed());
        boolean renderDynamically = shouldRenderGearWithBlockEntity();
        BlockState currentState = level.getBlockState(worldPosition);
        if (currentState.hasProperty(ROTATING) && currentState.getValue(ROTATING) != renderDynamically) {
            level.setBlock(worldPosition, currentState.setValue(ROTATING, renderDynamically), 2);
        }
    }

    @Override
    public int getGearTeeth() {
        // Shafts do not mesh by teeth; this value is only a safe placeholder for debug/readout.
        return 1;
    }

    @Override
    public float getGearMaxTorque() {
        return 16.0F;
    }

    @Override
    public boolean isShaftLike() {
        return true;
    }

    @Override
    public Direction.Axis getGearAxis() {
        BlockState state = getBlockState();
        return state.hasProperty(DirectionalBlock.FACING) ? state.getValue(DirectionalBlock.FACING).getAxis() : Direction.Axis.Y;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
