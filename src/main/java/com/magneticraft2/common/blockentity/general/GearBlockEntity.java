package com.magneticraft2.common.blockentity.general;

import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.GEAR.GearNode;
import com.magneticraft2.common.systems.networking.GearSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.magneticraft2.common.systems.mgc2Network.CHANNEL;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public abstract class GearBlockEntity extends BlockEntity {
    public static final Logger LOGGER = LogManager.getLogger("GearBlockEntityMagneticraft2");
    protected GearNode gearNode;
    protected float clientSpeed;
    protected float clientTorque;

    public GearBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
//        this.gearNode = new GearNode(pos);
    }
    public GearNode getGearNode() {
        return gearNode;
    }

    // Set powered state
    public abstract void setPowered(boolean powered);

    // Update the power and synchronization logic
    public void updateGearNetwork() {
        // Update the gear network with this gear's current state
        GearNetworkManager networkManager = GearNetworkManager.getInstance();
        networkManager.addGear(gearNode);
        // Handle synchronization of power state and rotation here if necessary
        sendGearSyncPacket();  // Ensure that the packet sync is sent when necessary
    }

    // Send a sync packet to ensure client updates
    public void sendGearSyncPacket() {
        if (level != null && !level.isClientSide) {
            GearSyncPacket packet = new GearSyncPacket(gearNode.getPosition(), gearNode.getSpeed(), gearNode.getTorque(),
                    gearNode.getDirectionMultiplier(), gearNode.getSourcePos());
            CHANNEL.send(PacketDistributor.ALL.noArg(),packet);  // Send to all clients in the network
        }
    }

    // Sync gear state with the server side
    public void syncGearState(float speed, float torque, int directionMultiplier, BlockPos sourcePos) {
        this.gearNode.updateClientData(speed, torque);
        this.gearNode.setDirectionMultiplier(directionMultiplier);
        this.gearNode.setSourcePos(sourcePos);
    }

    // Handle power and rotation changes based on surrounding blocks
    public void checkAndUpdatePower() {
        // Check if the surrounding blocks provide power and update accordingly
        GearNetworkManager manager = GearNetworkManager.getInstance();
        GearNode sourceGear = manager.getGear(gearNode.getSourcePos());
        if (sourceGear != null && sourceGear.getSpeed() > 0) {
            this.setPowered(true);
            this.syncGearState(sourceGear.getSpeed(), sourceGear.getTorque(),
                    sourceGear.getDirectionMultiplier(), sourceGear.getSourcePos());
            LOGGER.info("Gear at location " + sourceGear.getPosition() + " updated to " + sourceGear.getSpeed());
        } else {
            this.setPowered(false);
        }
    }

    // Override the BlockEntity save method to persist gear state
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // Save gearNode data
        tag = gearNode.saveToNBT();
    }

    // Override the BlockEntity load method to restore gear state
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // Load gearNode data
        gearNode.loadFromNBT(tag);
    }


//    public abstract void updateNetwork(GearNetworkManager networkManager);
//
//    public GearNode getGearNode() {
//        return this.gearNode;
//    }
//
//    public void updateClientData(float speed, float torque) {
//        this.clientSpeed = speed;
//        this.clientTorque = torque;
//    }
//
//    @Override
//    public void setChanged() {
//        super.setChanged();
//        if (level != null && !level.isClientSide) {
//            CHANNEL.send(PacketDistributor.ALL.noArg(), new GearSyncPacket(worldPosition, gearNode.getSpeed(), gearNode.getTorque()));
//        }
//    }
}