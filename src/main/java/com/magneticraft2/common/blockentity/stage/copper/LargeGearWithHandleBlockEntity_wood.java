package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.LargeGearWithHandleBlock_wood;
import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.GEAR.GearNode;
import com.magneticraft2.common.systems.networking.GearSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.magneticraft2.common.block.stage.copper.LargeGearWithHandleBlock_wood.ACTIVE;
import static com.magneticraft2.common.systems.mgc2Network.CHANNEL;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class LargeGearWithHandleBlockEntity_wood extends GearBlockEntity {
    private static final Set<BlockPos> updatedGears = new HashSet<>();
    private boolean isMoving = false;
    private int activeTicks = 0; // Tracks how long the handle is active

    public LargeGearWithHandleBlockEntity_wood(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GEAR_LARGE_WITH_HANDLE_BE_WOOD.get(), pos, state);
        this.gearNode = new GearNode(pos);
    }

    public void handleRightClick() {
        // Start motion
        if (!isMoving) {
            isMoving = true;
            this.gearNode.setSpeed(5.0f); // Example: Initial speed for starting motion
            this.gearNode.setTorque(1.0f);
            updateGearNetwork();
            sendGearSyncPacket();
            GearNetworkManager.getInstance().updateNetwork();
            syncWithClient();
        }
    }
    public static <E extends BlockEntity> void serverTick(Level level, BlockPos pos, BlockState estate, E e) {
        if (!level.isClientSide()) {
            LargeGearWithHandleBlockEntity_wood entity = (LargeGearWithHandleBlockEntity_wood) level.getBlockEntity(pos);
            if (entity.isMoving) {
                float currentSpeed = entity.gearNode.getSpeed();
                if (currentSpeed > 0.0f) {
                    entity.gearNode.updateClientData(entity.gearNode.getSpeed(), entity.gearNode.getTorque());
                    entity.updateGearNetwork();
                    entity.syncWithClient();
                    GearNetworkManager.getInstance().updateNetwork();
                    BlockState currentState = level.getBlockState(pos);
                    level.setBlock(pos, currentState, 2);
                    GearNetworkManager.getInstance().printNetworkState();
                    entity.gearNode.setSpeed(currentSpeed - 0.1f); // Slow down over time
                    entity.checkAndUpdatePower();
                } else {
                    entity.gearNode.setSpeed(0.0f);
                    entity.isMoving = false;
                    BlockState currentState = level.getBlockState(pos);
                    BlockState newstate = currentState.setValue(ACTIVE, false);
                    level.setBlock(pos, newstate, 2);
                }
            }
        }
    }

    private void syncWithClient() {
        if (level != null && !level.isClientSide) {
            // Use GearSyncPacket to sync GearNode data
            GearSyncPacket packet = new GearSyncPacket(worldPosition, gearNode.getSpeed(), gearNode.getTorque(), gearNode.getDirectionMultiplier(), gearNode.getSourcePos());
            CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return ClientboundBlockEntityDataPacket.create(this, (blockEntity) -> tag);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
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
    public void load(CompoundTag pTag) {
        super.load(pTag);
        isMoving = pTag.getBoolean("IsMoving");
        CompoundTag gearNodeTag = pTag.getCompound("GearNode");
        gearNode.loadFromNBT(gearNodeTag);
    }

    @Override
    public void setPowered(boolean powered) {
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("IsMoving", isMoving);
        if (gearNode != null) {
            tag.put("GearNode", gearNode.saveToNBT());
        }
    }
}
