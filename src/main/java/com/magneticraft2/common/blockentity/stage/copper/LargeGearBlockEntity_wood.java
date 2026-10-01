package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static com.magneticraft2.common.block.stage.copper.LargeGearBlock_wood.POWERED;
import static com.magneticraft2.common.block.stage.copper.LargeGearBlock_wood.VERTICAL_FACING_down;
import static com.magneticraft2.common.block.stage.copper.LargeGearBlock_wood.VERTICAL_FACING_up;
import static net.minecraft.world.level.block.DirectionalBlock.FACING;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * @version 1.0.0
 */
public class LargeGearBlockEntity_wood extends GearBlockEntity {
    public LargeGearBlockEntity_wood(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GEAR_LARGE_BE_WOOD.get(), pos, state);
    }

    public static <E extends BlockEntity> void serverTick(Level level, BlockPos pos, BlockState estate, E e) {
        if (level.isClientSide) {
            return;
        }

        if (level.getBlockEntity(pos) instanceof LargeGearBlockEntity_wood entity) {
            entity.serverTickGear();
            entity.updatePoweredState();
        }
    }

    private void updatePoweredState() {
        if (level == null || level.isClientSide) {
            return;
        }

        markHasEverRotatedIfMoving(getServerSpeed());
        boolean renderWithBlockEntity = shouldRenderGearWithBlockEntity();
        BlockState currentState = level.getBlockState(worldPosition);
        if (currentState.hasProperty(POWERED) && currentState.getValue(POWERED) != renderWithBlockEntity) {
            level.setBlock(worldPosition, currentState.setValue(POWERED, renderWithBlockEntity), 2);
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
    public Direction.Axis getGearAxis() {
        BlockState state = getBlockState();
        if (state.hasProperty(VERTICAL_FACING_up) && state.hasProperty(VERTICAL_FACING_down)
                && (state.getValue(VERTICAL_FACING_up) || state.getValue(VERTICAL_FACING_down))) {
            return Direction.Axis.Y;
        }
        return state.hasProperty(FACING) ? state.getValue(FACING).getAxis() : Direction.Axis.Y;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public float getSpeed() {
        return getClientSpeed();
    }

    public float getTorque() {
        return getClientTorque();
    }

    @Override
    public void setPowered(boolean powered) {
        super.setPowered(powered);
    }
}
