package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.ItemBeltConnectionManager;
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

import static net.minecraft.world.level.block.DirectionalBlock.FACING;

/**
 * Gear V2 roller endpoint with one optional continuous item-belt partner.
 */
public class ConveyorRollerBlockEntity extends GearBlockEntity {
    public static final double ROLLER_RADIUS = 0.31D;

    @Nullable
    private BlockPos itemBeltPartner;

    private double clientBeltTravelDistance = 0.0D;
    private float lastClientBeltVisualTime = Float.NaN;

    public ConveyorRollerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.CONVEYOR_ROLLER_BE.get(), pos, state);
    }

    public static <E extends BlockEntity> void tick(Level level,
                                                    BlockPos pos,
                                                    BlockState state,
                                                    E blockEntity) {
        if (!(blockEntity instanceof ConveyorRollerBlockEntity roller)) {
            return;
        }

        roller.ensureItemBeltRegistered();

        if (!level.isClientSide) {
            roller.serverTickGear();
            ItemBeltConnectionManager.tickConnection(roller);
        }
    }

    private void ensureItemBeltRegistered() {
        if (level != null
                && itemBeltPartner != null
                && !ItemBeltConnectionManager.isRegistered(level, worldPosition, itemBeltPartner)) {
            ItemBeltConnectionManager.ensureRegistered(this);
        }
    }

    @Override
    public int getGearTeeth() {
        return 8;
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
        return state.hasProperty(FACING)
                ? state.getValue(FACING).getAxis()
                : Direction.Axis.X;
    }

    public double getRollerRadius() {
        return ROLLER_RADIUS;
    }

    public double getVisualItemBeltTravelDistance(float partialTicks) {
        Level currentLevel = getLevel();
        if (currentLevel == null) {
            return clientBeltTravelDistance;
        }

        float currentVisualTime = currentLevel.getGameTime() + partialTicks;
        if (Float.isNaN(lastClientBeltVisualTime)) {
            lastClientBeltVisualTime = currentVisualTime;
            return clientBeltTravelDistance;
        }

        float deltaTicks = currentVisualTime - lastClientBeltVisualTime;
        lastClientBeltVisualTime = currentVisualTime;

        if (deltaTicks < 0.0F) {
            deltaTicks = 0.0F;
        } else if (deltaTicks > 20.0F) {
            deltaTicks = 20.0F;
        }

        float rpm = isClientOverloaded() ? 0.0F : getClientSpeed();
        if (Math.abs(rpm) > VISUAL_STOP_EPSILON) {
            double circumference = Math.PI * 2.0D * ROLLER_RADIUS;
            double blocksPerTick = (rpm / 1200.0D) * circumference;
            clientBeltTravelDistance += blocksPerTick * deltaTicks * getDirectionMultiplier();

            if (Math.abs(clientBeltTravelDistance) > 1024.0D) {
                clientBeltTravelDistance %= 1.0D;
            }
        }

        return clientBeltTravelDistance;
    }

    @Nullable
    public BlockPos getItemBeltPartner() {
        return itemBeltPartner;
    }

    public boolean isItemBeltLinkedTo(BlockPos pos) {
        return itemBeltPartner != null && itemBeltPartner.equals(pos);
    }

    public void linkItemBelt(BlockPos partner) {
        if (partner == null || partner.equals(worldPosition)) {
            return;
        }

        itemBeltPartner = partner.immutable();
        syncItemBeltState();
        ItemBeltConnectionManager.ensureRegistered(this);
        updateGearNetwork();
    }

    public void disconnectItemBelt(boolean notifyPartner) {
        BlockPos oldPartner = itemBeltPartner;
        if (oldPartner == null) {
            return;
        }

        if (level != null) {
            ItemBeltConnectionManager.remove(level, worldPosition, oldPartner);
        }

        itemBeltPartner = null;
        syncItemBeltState();
        updateGearNetwork();

        if (notifyPartner
                && level != null
                && level.getBlockEntity(oldPartner) instanceof ConveyorRollerBlockEntity other
                && other.isItemBeltLinkedTo(worldPosition)) {
            other.disconnectItemBelt(false);
        }
    }

    private void syncItemBeltState() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (itemBeltPartner != null) {
            tag.putLong("ItemBeltPartner", itemBeltPartner.asLong());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        BlockPos previousPartner = itemBeltPartner;
        itemBeltPartner = tag.contains("ItemBeltPartner")
                ? BlockPos.of(tag.getLong("ItemBeltPartner"))
                : null;

        if (level != null
                && previousPartner != null
                && (itemBeltPartner == null || !previousPartner.equals(itemBeltPartner))) {
            ItemBeltConnectionManager.remove(level, worldPosition, previousPartner);
        }

        ensureItemBeltRegistered();
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        if (itemBeltPartner != null) {
            tag.putLong("ItemBeltPartner", itemBeltPartner.asLong());
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
    public void setRemoved() {
        if (level != null) {
            ItemBeltConnectionManager.removeFor(level, worldPosition);
        }
        super.setRemoved();
    }

    @Override
    public AABB getRenderBoundingBox() {
        if (itemBeltPartner == null) {
            return super.getRenderBoundingBox();
        }

        return new AABB(
                Math.min(worldPosition.getX(), itemBeltPartner.getX()) - 1.0D,
                Math.min(worldPosition.getY(), itemBeltPartner.getY()) - 1.0D,
                Math.min(worldPosition.getZ(), itemBeltPartner.getZ()) - 1.0D,
                Math.max(worldPosition.getX(), itemBeltPartner.getX()) + 2.0D,
                Math.max(worldPosition.getY(), itemBeltPartner.getY()) + 2.0D,
                Math.max(worldPosition.getZ(), itemBeltPartner.getZ()) + 2.0D
        );
    }
}
