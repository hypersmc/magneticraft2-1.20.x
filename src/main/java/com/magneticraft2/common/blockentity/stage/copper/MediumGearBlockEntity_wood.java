package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.GEAR.GearNode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author JumpWatch on 27-12-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MediumGearBlockEntity_wood extends GearBlockEntity {
    private static final Set<BlockPos> updatedGears = new HashSet<>();

    public MediumGearBlockEntity_wood(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GEAR_MEDIUM_BE_WOOD.get(), pos, state);
        this.gearNode = new GearNode(pos);
    }

    public static <E extends BlockEntity> void serverTick(Level level, BlockPos pos, BlockState estate, E e) {
        GearNetworkManager manager = GearNetworkManager.getInstance();
        if (!level.isClientSide()) {
            MediumGearBlockEntity_wood entity = (MediumGearBlockEntity_wood) level.getBlockEntity(pos);
            manager.addGear(entity.gearNode);
            entity.gearNode.updateClientData(entity.gearNode.getSpeed(), entity.gearNode.getTorque());
            entity.checkAndUpdatePower();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        LOGGER.info("Trying to save TAG DATA");
        if (gearNode != null) {
            tag.put("GearNode", gearNode.saveToNBT());
        }

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