package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.PulleyBlock_wood;
import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import static com.magneticraft2.common.block.stage.copper.PulleyBlock_wood.POWERED;
import static net.minecraft.world.level.block.DirectionalBlock.FACING;

/**
 * Gear V2 pulley node with one optional belt partner.
 */
public class PulleyBlockEntity_wood extends GearBlockEntity {
    @Nullable
    private BlockPos beltPartner;

    public PulleyBlockEntity_wood(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.PULLEY_BE_WOOD.get(), pos, state);
    }

    public static <E extends BlockEntity> void serverTick(Level level,
                                                          BlockPos pos,
                                                          BlockState state,
                                                          E blockEntity) {
        if (level.isClientSide || !(blockEntity instanceof PulleyBlockEntity_wood pulley)) {
            return;
        }

        pulley.serverTickGear();
        pulley.updatePoweredState();
    }

    private void updatePoweredState() {
        if (level == null || level.isClientSide) {
            return;
        }

        markHasEverRotatedIfMoving(getServerSpeed());
        boolean dynamic = shouldRenderGearWithBlockEntity();
        BlockState state = getBlockState();
        if (state.hasProperty(POWERED) && state.getValue(POWERED) != dynamic) {
            level.setBlock(worldPosition, state.setValue(POWERED, dynamic), 2);
        }
    }

    @Override
    public int getGearTeeth() {
        if (getBlockState().getBlock() instanceof PulleyBlock_wood pulley) {
            return pulley.getPulleyTeeth();
        }
        return 8;
    }

    @Override
    public float getGearMaxTorque() {
        return 8.0F;
    }

    @Override
    public boolean isShaftLike() {
        return true;
    }

    @Override
    public Direction.Axis getGearAxis() {
        BlockState state = getBlockState();
        return state.hasProperty(FACING) ? state.getValue(FACING).getAxis() : Direction.Axis.Y;
    }

    public double getPulleyRadius() {
        return getGearTeeth() > 8 ? 0.68D : 0.38D;
    }

    @Nullable
    public BlockPos getBeltPartner() {
        return beltPartner;
    }

    public boolean isLinkedTo(BlockPos pos) {
        return beltPartner != null && beltPartner.equals(pos);
    }

    public void linkBelt(BlockPos partner) {
        if (partner == null || partner.equals(worldPosition)) {
            return;
        }

        beltPartner = partner.immutable();
        syncBeltState();
        updateGearNetwork();
    }

    public void disconnectBelt(boolean notifyPartner) {
        BlockPos oldPartner = beltPartner;
        if (oldPartner == null) {
            return;
        }

        beltPartner = null;
        syncBeltState();
        updateGearNetwork();

        if (notifyPartner && level != null && level.getBlockEntity(oldPartner) instanceof PulleyBlockEntity_wood other
                && other.isLinkedTo(worldPosition)) {
            other.disconnectBelt(false);
        }
    }

    private void syncBeltState() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (beltPartner != null) {
            tag.putLong("BeltPartner", beltPartner.asLong());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        beltPartner = tag.contains("BeltPartner")
                ? BlockPos.of(tag.getLong("BeltPartner"))
                : null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        if (beltPartner != null) {
            tag.putLong("BeltPartner", beltPartner.asLong());
        }
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            load(tag);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        if (beltPartner == null) {
            return super.getRenderBoundingBox();
        }

        return new AABB(
                Math.min(worldPosition.getX(), beltPartner.getX()) - 1.0D,
                Math.min(worldPosition.getY(), beltPartner.getY()) - 1.0D,
                Math.min(worldPosition.getZ(), beltPartner.getZ()) - 1.0D,
                Math.max(worldPosition.getX(), beltPartner.getX()) + 2.0D,
                Math.max(worldPosition.getY(), beltPartner.getY()) + 2.0D,
                Math.max(worldPosition.getZ(), beltPartner.getZ()) + 2.0D
        );
    }
}
