package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.GEAR.GearNode;
import com.magneticraft2.common.systems.networking.GearSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.magneticraft2.common.block.stage.copper.MediumGearBlock_wood.POWERED;
import static com.magneticraft2.common.systems.mgc2Network.CHANNEL;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class LargeGearBlockEntity_wood extends GearBlockEntity {
    private static final Set<BlockPos> updatedGears = new HashSet<>();

    public LargeGearBlockEntity_wood(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GEAR_LARGE_BE_WOOD.get(), pos, state);
        this.gearNode = new GearNode(pos);
    }


    public static <E extends BlockEntity> void serverTick(Level level, BlockPos pos, BlockState estate, E e) {
        GearNetworkManager manager = GearNetworkManager.getInstance();
        if (!level.isClientSide()) {
            LargeGearBlockEntity_wood entity = (LargeGearBlockEntity_wood) level.getBlockEntity(pos);
            float currentSpeed = entity.gearNode.getSpeed();
            // Make sure the gear is in the manager
            manager.addGear(entity.gearNode,level);
            // Now propagate the speed to neighbors
            manager.updateNetwork(level);
            // Making sure the data is synced with client
            entity.syncWithClient();
            if (currentSpeed > 0.0f) {
                BlockState currentState = level.getBlockState(pos);
                BlockState newstate = currentState.setValue(POWERED, true);
                level.setBlock(pos, newstate, 2);
            }
            else {
                BlockState currentState = level.getBlockState(pos);
                BlockState newstate = currentState.setValue(POWERED, false);
                level.setBlock(pos, newstate, 2);
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
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("GearNode", gearNode.saveToNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        CompoundTag gearNodeTag = tag.getCompound("GearNode");
        gearNode.loadFromNBT(gearNodeTag);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public float getSpeed() {
        return gearNode.getClientSpeed();
    }

    public float getTorque() {
        return gearNode.getClientTorque();
    }

    @Override
    public void setPowered(boolean powered) {}
}
