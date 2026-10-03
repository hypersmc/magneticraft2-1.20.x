package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.ItemBeltConnectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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

    // Items on a belt are no longer loose ItemEntities being shoved every tick. The
    // canonical roller owns their stack + exact distance along the carrying run, similar
    // to Create's transported-item concept. The renderer places the stack directly on
    // the moving belt surface.
    private final List<TransportedItem> transportedItems = new ArrayList<>();
    private float clientTransportSyncTime = Float.NaN;

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

    public List<TransportedItem> getTransportedItems() {
        return transportedItems;
    }

    public void addTransportedItem(ItemStack stack, double distance) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        transportedItems.add(new TransportedItem(stack.copy(), distance));
        syncTransportedItems();
    }

    public void syncTransportedItems() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public double getClientTransportDistance(TransportedItem transportedItem, float partialTicks) {
        double distance = transportedItem.getDistance();
        Level currentLevel = getLevel();
        if (currentLevel == null || Float.isNaN(clientTransportSyncTime)) {
            return distance;
        }

        float elapsed = (currentLevel.getGameTime() + partialTicks) - clientTransportSyncTime;
        if (elapsed < 0.0F) {
            elapsed = 0.0F;
        } else if (elapsed > 10.0F) {
            elapsed = 10.0F;
        }

        float rpm = isClientOverloaded() ? 0.0F : getClientSpeed();
        if (transportedItem.isStalled()
                || Math.abs(rpm) <= VISUAL_STOP_EPSILON) {
            return distance;
        }

        double blocksPerTick = Math.min(
                0.18D,
                (rpm / 1200.0D) * (Math.PI * 2.0D * ROLLER_RADIUS)
        );

        int transportDirection = itemBeltPartner == null
                ? (getDirectionMultiplier() < 0 ? -1 : 1)
                : ItemBeltConnectionManager.getTransportDirectionSign(
                currentLevel,
                worldPosition,
                itemBeltPartner,
                getDirectionMultiplier()
        );

        return distance + blocksPerTick * elapsed * transportDirection;
    }

    public double getClientHandoffProgress(TransportedItem transportedItem,
                                           float partialTicks) {
        if (!transportedItem.isHandoffActive()) {
            return 1.0D;
        }

        double progress = transportedItem.getHandoffProgress();
        Level currentLevel = getLevel();
        if (currentLevel == null
                || Float.isNaN(clientTransportSyncTime)
                || transportedItem.isStalled()) {
            return progress;
        }

        float elapsed = (currentLevel.getGameTime() + partialTicks)
                - clientTransportSyncTime;
        elapsed = Math.max(0.0F, Math.min(10.0F, elapsed));

        float rpm = isClientOverloaded() ? 0.0F : getClientSpeed();
        if (Math.abs(rpm) <= VISUAL_STOP_EPSILON) {
            return progress;
        }

        double blocksPerTick = Math.min(
                0.18D,
                (rpm / 1200.0D) * (Math.PI * 2.0D * ROLLER_RADIUS)
        );

        return Math.min(
                1.0D,
                progress + blocksPerTick * elapsed
                        / ItemBeltConnectionManager.HANDOFF_BLEND_DISTANCE
        );
    }

    public List<ItemStack> clearTransportedItems() {
        List<ItemStack> stacks = new ArrayList<>();
        for (TransportedItem transportedItem : transportedItems) {
            if (!transportedItem.getStack().isEmpty()) {
                stacks.add(transportedItem.getStack().copy());
            }
        }
        transportedItems.clear();
        syncTransportedItems();
        return stacks;
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

    /**
     * Clears only this endpoint's saved/rendered belt link. Used by the authoritative
     * connection manager while it is already tearing the full belt down, so it must not
     * recursively remove the same connection again.
     */
    public void clearItemBeltLink() {
        if (itemBeltPartner == null) {
            return;
        }

        itemBeltPartner = null;
        syncItemBeltState();
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

        ListTag transportedList = new ListTag();
        for (TransportedItem transportedItem : transportedItems) {
            if (transportedItem.getStack().isEmpty()) {
                continue;
            }

            CompoundTag transportedTag = new CompoundTag();
            transportedTag.putDouble("Distance", transportedItem.getDistance());
            transportedTag.putDouble("LateralOffset", transportedItem.getLateralOffset());
            transportedTag.putBoolean("Stalled", transportedItem.isStalled());
            transportedTag.putBoolean("HandoffActive", transportedItem.isHandoffActive());
            transportedTag.putDouble("HandoffProgress", transportedItem.getHandoffProgress());

            if (transportedItem.isHandoffActive()) {
                Vec3 origin = transportedItem.getHandoffOrigin();
                transportedTag.putDouble("HandoffOriginX", origin.x);
                transportedTag.putDouble("HandoffOriginY", origin.y);
                transportedTag.putDouble("HandoffOriginZ", origin.z);
            }

            CompoundTag stackTag = new CompoundTag();
            transportedItem.getStack().save(stackTag);
            transportedTag.put("Stack", stackTag);
            transportedList.add(transportedTag);
        }
        tag.put("TransportedItems", transportedList);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        BlockPos previousPartner = itemBeltPartner;
        itemBeltPartner = tag.contains("ItemBeltPartner")
                ? BlockPos.of(tag.getLong("ItemBeltPartner"))
                : null;

        transportedItems.clear();
        ListTag transportedList = tag.getList("TransportedItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < transportedList.size(); i++) {
            CompoundTag transportedTag = transportedList.getCompound(i);
            ItemStack stack = ItemStack.of(transportedTag.getCompound("Stack"));
            if (!stack.isEmpty()) {
                TransportedItem transportedItem = new TransportedItem(
                        stack,
                        transportedTag.getDouble("Distance")
                );
                transportedItem.setLateralOffset(
                        transportedTag.getDouble("LateralOffset")
                );
                transportedItem.setStalled(
                        transportedTag.getBoolean("Stalled")
                );

                if (transportedTag.getBoolean("HandoffActive")) {
                    transportedItem.beginHandoff(
                            new Vec3(
                                    transportedTag.getDouble("HandoffOriginX"),
                                    transportedTag.getDouble("HandoffOriginY"),
                                    transportedTag.getDouble("HandoffOriginZ")
                            ),
                            transportedTag.getDouble("HandoffProgress")
                    );
                }

                transportedItems.add(transportedItem);
            }
        }

        if (level != null && level.isClientSide) {
            clientTransportSyncTime = (float) level.getGameTime();
        }

        if (level != null
                && previousPartner != null
                && (itemBeltPartner == null || !previousPartner.equals(itemBeltPartner))) {
            ItemBeltConnectionManager.remove(level, worldPosition, previousPartner);
        }

        ensureItemBeltRegistered();
    }

    @Override
    public CompoundTag getUpdateTag() {
        // BlockEntity#getUpdateTag() is empty in vanilla. Use the same payload we persist
        // to disk so client-side renderers receive both the belt partner and the
        // transported ItemStack list.
        return saveWithoutMetadata();
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
    public static final class TransportedItem {
        private final ItemStack stack;
        private double distance;
        private double lateralOffset;
        private boolean stalled;
        private boolean handoffActive;
        private Vec3 handoffOrigin = Vec3.ZERO;
        private double handoffProgress = 1.0D;

        public TransportedItem(ItemStack stack, double distance) {
            this.stack = stack == null ? ItemStack.EMPTY : stack;
            this.distance = distance;
        }

        public ItemStack getStack() {
            return stack;
        }

        public double getDistance() {
            return distance;
        }

        public void setDistance(double distance) {
            this.distance = distance;
        }

        public double getLateralOffset() {
            return lateralOffset;
        }

        public void setLateralOffset(double lateralOffset) {
            this.lateralOffset = lateralOffset;
        }

        public boolean isStalled() {
            return stalled;
        }

        public void setStalled(boolean stalled) {
            this.stalled = stalled;
        }

        public boolean isHandoffActive() {
            return handoffActive;
        }

        public Vec3 getHandoffOrigin() {
            return handoffOrigin;
        }

        public double getHandoffProgress() {
            return handoffProgress;
        }

        public void beginHandoff(Vec3 origin, double progress) {
            handoffOrigin = origin == null ? Vec3.ZERO : origin;
            handoffProgress = Math.max(0.0D, Math.min(1.0D, progress));
            handoffActive = handoffProgress < 1.0D;
        }

        public void advanceHandoff(double progressDelta) {
            if (!handoffActive) {
                return;
            }

            handoffProgress = Math.min(
                    1.0D,
                    handoffProgress + Math.max(0.0D, progressDelta)
            );
            if (handoffProgress >= 1.0D) {
                handoffProgress = 1.0D;
                handoffActive = false;
            }
        }
    }

}
