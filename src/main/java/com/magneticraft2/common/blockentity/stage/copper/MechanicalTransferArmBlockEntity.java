package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.blockentity.general.GearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import com.magneticraft2.common.systems.GEAR.GearNode;
import com.magneticraft2.common.systems.GEAR.ItemBeltConnectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

public class MechanicalTransferArmBlockEntity
        extends GearBlockEntity {

    public static final float MIN_SPEED = 10.0F;
    public static final float TORQUE_DEMAND = 2.0F;
    public static final float MAX_PROGRESS_PER_TICK = 0.05F;

    @Nullable
    private Direction sourceSide;
    @Nullable
    private Direction destinationSide;

    private ItemStack filterStack =
            ItemStack.EMPTY;
    private boolean blacklist = false;

    /**
     * A real extracted item lives in the arm during the outbound half-cycle.
     * This makes destination back-pressure lossless and restart-safe.
     */
    private ItemStack carriedStack =
            ItemStack.EMPTY;

    /**
     * 0.0 = source, 0.5 = destination, 1.0 = source again.
     */
    private float cycleProgress = 0.0F;
    private boolean active = false;

    private float clientCycleProgress = 0.0F;
    private float clientCycleSyncTime = Float.NaN;

    public MechanicalTransferArmBlockEntity(
            BlockPos pos,
            BlockState state) {
        super(
                BlockEntityRegistry
                        .MECHANICAL_TRANSFER_ARM_BE
                        .get(),
                pos,
                state
        );
    }

    public static <E extends BlockEntity> void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            E blockEntity) {
        if (level.isClientSide
                || !(blockEntity
                instanceof MechanicalTransferArmBlockEntity arm)) {
            return;
        }

        arm.serverTickGear();
        arm.tickTransfer();
    }

    private void tickTransfer() {
        if (level == null) {
            return;
        }

        if (sourceSide == null
                || destinationSide == null
                || sourceSide == destinationSide) {
            stopMechanicalLoad();
            setActive(false);
            return;
        }

        GearNode node =
                getOrCreateGearNode();

        float speed =
                Math.abs(
                        node.getEffectiveSpeed()
                );

        if (carriedStack.isEmpty()
                && cycleProgress <= 0.0001F) {
            ItemStack candidate =
                    findExtractableItem(true);

            if (candidate.isEmpty()
                    || !canInsert(candidate, true)) {
                stopMechanicalLoad();
                setActive(false);
                return;
            }

            if (!hasUsablePower(node, speed, true)) {
                setActive(false);
                return;
            }

            ItemStack extracted =
                    findExtractableItem(false);

            if (extracted.isEmpty()) {
                stopMechanicalLoad();
                setActive(false);
                return;
            }

            carriedStack =
                    extracted.copy();
            cycleProgress = 0.0F;
            setActive(true);
            sync();
            return;
        }

        // The arm reached the destination while carrying an item. If insertion
        // is blocked, simply hold position and keep the item in the claw.
        if (!carriedStack.isEmpty()
                && cycleProgress >= 0.5F - 0.0001F) {
            if (!canInsert(
                    carriedStack,
                    true
            )) {
                stopMechanicalLoad();
                setActive(false);
                return;
            }

            if (!hasUsablePower(node, speed, true)) {
                setActive(false);
                return;
            }

            ItemStack remainder =
                    insertIntoDestination(
                            carriedStack,
                            false
                    );

            if (!remainder.isEmpty()) {
                carriedStack =
                        remainder;
                stopMechanicalLoad();
                setActive(false);
                sync();
                return;
            }

            carriedStack =
                    ItemStack.EMPTY;
            cycleProgress =
                    Math.max(
                            cycleProgress,
                            0.5001F
                    );
            setActive(true);
            sync();
            return;
        }

        // Empty claw after delivery: return to the orange/source side.
        boolean moving =
                !carriedStack.isEmpty()
                        || cycleProgress > 0.0F;

        if (!moving) {
            stopMechanicalLoad();
            setActive(false);
            return;
        }

        if (!hasUsablePower(
                node,
                speed,
                true
        )) {
            setActive(false);
            return;
        }

        float before =
                cycleProgress;

        cycleProgress +=
                progressPerTick(speed);

        if (!carriedStack.isEmpty()
                && before < 0.5F
                && cycleProgress >= 0.5F) {
            cycleProgress = 0.5F;
        }

        if (carriedStack.isEmpty()
                && cycleProgress >= 1.0F) {
            cycleProgress = 0.0F;
            setActive(false);
            stopMechanicalLoad();
            sync();
            return;
        }

        setActive(true);
        setChanged();

        if (level.getGameTime() % 4L == 0L) {
            sync();
        }
    }

    private boolean hasUsablePower(
            GearNode node,
            float speed,
            boolean loadRequested) {
        GearNetworkManager network =
                GearNetworkManager.getInstance();

        network.setMechanicalLoad(
                level,
                worldPosition,
                worldPosition,
                TORQUE_DEMAND,
                loadRequested
        );

        GearNetworkManager.MechanicalLoadState load =
                network.getMechanicalLoadState(
                        level,
                        worldPosition
                );

        return loadRequested
                && load.supplied()
                && !node.isOverloaded()
                && speed >= MIN_SPEED
                && node.getTorque()
                + 0.001F
                >= TORQUE_DEMAND;
    }

    private void stopMechanicalLoad() {
        if (level != null
                && !level.isClientSide) {
            GearNetworkManager.getInstance()
                    .removeMechanicalLoad(
                            level,
                            worldPosition
                    );
        }
    }

    private float progressPerTick(
            float speed) {
        return Math.min(
                MAX_PROGRESS_PER_TICK,
                Math.max(
                        0.0025F,
                        speed / 4800.0F
                )
        );
    }

    private ItemStack findExtractableItem(
            boolean simulate) {
        if (level == null
                || sourceSide == null) {
            return ItemStack.EMPTY;
        }

        /*
         * Belt cargo is position-based rather than inventory-based. Ask the
         * belt transport system first so the arm can pick the item actually
         * passing through the adjacent belt cell.
         */
        ItemStack beltItem =
                ItemBeltConnectionManager
                        .extractForAutomationAt(
                                level,
                                worldPosition.relative(
                                        sourceSide
                                ),
                                this::passesFilter,
                                simulate
                        );

        if (!beltItem.isEmpty()) {
            return beltItem;
        }

        IItemHandler source =
                getHandler(sourceSide);

        if (source == null) {
            return ItemStack.EMPTY;
        }

        for (int slot = 0;
             slot < source.getSlots();
             slot++) {
            ItemStack candidate =
                    source.extractItem(
                            slot,
                            1,
                            true
                    );

            if (candidate.isEmpty()
                    || !passesFilter(candidate)) {
                continue;
            }

            if (simulate) {
                return candidate.copy();
            }

            ItemStack extracted =
                    source.extractItem(
                            slot,
                            1,
                            false
                    );

            if (!extracted.isEmpty()
                    && passesFilter(extracted)) {
                return extracted;
            }
        }

        return ItemStack.EMPTY;
    }

    private boolean passesFilter(
            ItemStack stack) {
        if (filterStack.isEmpty()) {
            return true;
        }

        boolean matches =
                ItemStack.isSameItemSameTags(
                        filterStack,
                        stack
                );

        return blacklist
                ? !matches
                : matches;
    }

    private boolean canInsert(
            ItemStack stack,
            boolean simulate) {
        return insertIntoDestination(
                stack,
                simulate
        ).isEmpty();
    }

    private ItemStack insertIntoDestination(
            ItemStack stack,
            boolean simulate) {
        if (level == null
                || destinationSide == null) {
            return stack.copy();
        }

        BlockPos targetPos =
                worldPosition.relative(
                        destinationSide
                );

        int beltAccepted =
                ItemBeltConnectionManager
                        .insertFromAutomationAt(
                                level,
                                targetPos,
                                stack,
                                simulate
                        );

        if (beltAccepted > 0) {
            ItemStack remainder =
                    stack.copy();
            remainder.shrink(
                    beltAccepted
            );
            return remainder;
        }

        IItemHandler destination =
                getHandler(destinationSide);

        if (destination == null) {
            return stack.copy();
        }

        ItemStack remainder =
                stack.copy();

        for (int slot = 0;
             slot < destination.getSlots()
                     && !remainder.isEmpty();
             slot++) {
            remainder =
                    destination.insertItem(
                            slot,
                            remainder,
                            simulate
                    );
        }

        return remainder;
    }

    @Nullable
    private IItemHandler getHandler(
            @Nullable Direction side) {
        if (level == null
                || side == null) {
            return null;
        }

        BlockPos target =
                worldPosition.relative(side);

        BlockEntity blockEntity =
                level.getBlockEntity(target);

        if (blockEntity == null) {
            return null;
        }

        IItemHandler sided =
                blockEntity
                        .getCapability(
                                ForgeCapabilities.ITEM_HANDLER,
                                side.getOpposite()
                        )
                        .orElse(null);

        if (sided != null) {
            return sided;
        }

        // Some modded inventories expose only an unsided Forge handler.
        // Prefer that before falling back to vanilla Container wrappers.
        IItemHandler unsided =
                blockEntity
                        .getCapability(
                                ForgeCapabilities.ITEM_HANDLER,
                                null
                        )
                        .orElse(null);

        if (unsided != null) {
            return unsided;
        }

        /*
         * Vanilla inventories are not required to expose Forge's ITEM_HANDLER
         * capability. Adapt them here instead of special-casing "chest" as a
         * transfer target. ChestBlock.getContainer also preserves a double
         * chest as one logical inventory.
         */
        BlockState targetState =
                level.getBlockState(target);

        if (targetState.getBlock()
                instanceof ChestBlock chestBlock) {
            Container chest =
                    ChestBlock.getContainer(
                            chestBlock,
                            targetState,
                            level,
                            target,
                            true
                    );

            if (chest != null) {
                return new InvWrapper(chest);
            }
        }

        if (blockEntity
                instanceof WorldlyContainer sidedContainer) {
            return new SidedInvWrapper(
                    sidedContainer,
                    side.getOpposite()
            );
        }

        if (blockEntity
                instanceof Container container) {
            return new InvWrapper(container);
        }

        return null;
    }

    public SideRole cycleSide(
            Direction side) {
        if (side == null
                || !side.getAxis().isHorizontal()) {
            return SideRole.NONE;
        }

        SideRole current =
                getRole(side);

        if (!carriedStack.isEmpty()) {
            return current;
        }

        switch (current) {
            case NONE -> {
                sourceSide = side;

                if (destinationSide == side) {
                    destinationSide = null;
                }
            }
            case SOURCE -> {
                sourceSide = null;
                destinationSide = side;
            }
            case DESTINATION ->
                    destinationSide = null;
        }

        cycleProgress = 0.0F;

        setChanged();
        sync();
        return getRole(side);
    }

    public SideRole getRole(
            Direction side) {
        if (side == sourceSide) {
            return SideRole.SOURCE;
        }

        if (side == destinationSide) {
            return SideRole.DESTINATION;
        }

        return SideRole.NONE;
    }

    public void setFilter(
            ItemStack stack) {
        filterStack =
                stack == null
                        ? ItemStack.EMPTY
                        : stack.copy();

        if (!filterStack.isEmpty()) {
            filterStack.setCount(1);
        }

        setChanged();
        sync();
    }

    public void clearFilter() {
        filterStack = ItemStack.EMPTY;
        blacklist = false;
        setChanged();
        sync();
    }

    public void toggleBlacklist() {
        if (filterStack.isEmpty()) {
            return;
        }

        blacklist = !blacklist;
        setChanged();
        sync();
    }

    @Nullable
    public Direction getSourceSide() {
        return sourceSide;
    }

    @Nullable
    public Direction getDestinationSide() {
        return destinationSide;
    }

    public ItemStack getFilterStack() {
        return filterStack.copy();
    }

    public boolean isBlacklist() {
        return blacklist;
    }

    public ItemStack getCarriedStack() {
        return carriedStack.copy();
    }

    public ItemStack takeCarriedStackForDrop() {
        if (carriedStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack dropped =
                carriedStack.copy();
        carriedStack =
                ItemStack.EMPTY;
        setChanged();
        return dropped;
    }

    public boolean isActive() {
        return active;
    }

    public float getCycleProgress() {
        return cycleProgress;
    }

    public float getVisualCycleProgress(
            float partialTicks) {
        if (level == null) {
            return cycleProgress;
        }

        if (!active) {
            clientCycleProgress =
                    cycleProgress;
            clientCycleSyncTime =
                    level.getGameTime()
                            + partialTicks;
            return cycleProgress;
        }

        float now =
                level.getGameTime()
                        + partialTicks;

        if (Float.isNaN(
                clientCycleSyncTime)) {
            clientCycleProgress =
                    cycleProgress;
            clientCycleSyncTime = now;
        }

        float elapsed =
                Math.max(
                        0.0F,
                        Math.min(
                                4.0F,
                                now - clientCycleSyncTime
                        )
                );

        float predicted =
                cycleProgress
                        + progressPerTick(
                        Math.abs(
                                getClientSpeed()
                        )
                ) * elapsed;

        if (!carriedStack.isEmpty()) {
            predicted =
                    Math.min(
                            0.5F,
                            predicted
                    );
        } else {
            predicted =
                    Math.min(
                            1.0F,
                            predicted
                    );
        }

        clientCycleProgress =
                predicted;

        return predicted;
    }

    public String getRelativeSideName(
            Direction side) {
        BlockState state =
                getBlockState();

        Direction forward =
                state.hasProperty(
                        DirectionalBlock.FACING
                )
                        ? state.getValue(
                        DirectionalBlock.FACING
                )
                        : Direction.NORTH;

        if (side == forward) {
            return "Front";
        }

        if (side
                == forward.getOpposite()) {
            return "Back";
        }

        if (side
                == forward.getClockWise()) {
            return "Left";
        }

        if (side
                == forward.getCounterClockWise()) {
            return "Right";
        }

        return side.getName();
    }

    private void setActive(
            boolean value) {
        if (active == value) {
            return;
        }

        active = value;
        setChanged();
        sync();
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
    public Direction.Axis getGearAxis() {
        return Direction.Axis.Y;
    }

    @Override
    public boolean isShaftLike() {
        return false;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag) {
        super.saveAdditional(tag);

        if (sourceSide != null) {
            tag.putString(
                    "SourceSide",
                    sourceSide.getName()
            );
        }

        if (destinationSide != null) {
            tag.putString(
                    "DestinationSide",
                    destinationSide.getName()
            );
        }

        if (!filterStack.isEmpty()) {
            tag.put(
                    "Filter",
                    filterStack.save(
                            new CompoundTag()
                    )
            );
        }

        if (!carriedStack.isEmpty()) {
            tag.put(
                    "Carried",
                    carriedStack.save(
                            new CompoundTag()
                    )
            );
        }

        tag.putBoolean(
                "Blacklist",
                blacklist
        );
        tag.putFloat(
                "CycleProgress",
                cycleProgress
        );
        tag.putBoolean(
                "Active",
                active
        );
    }

    @Override
    public void load(
            CompoundTag tag) {
        super.load(tag);

        sourceSide =
                readDirection(
                        tag.getString(
                                "SourceSide"
                        )
                );
        destinationSide =
                readDirection(
                        tag.getString(
                                "DestinationSide"
                        )
                );

        filterStack =
                tag.contains("Filter")
                        ? ItemStack.of(
                        tag.getCompound(
                                "Filter"
                        )
                )
                        : ItemStack.EMPTY;

        carriedStack =
                tag.contains("Carried")
                        ? ItemStack.of(
                        tag.getCompound(
                                "Carried"
                        )
                )
                        : ItemStack.EMPTY;

        blacklist =
                tag.getBoolean(
                        "Blacklist"
                );
        cycleProgress =
                Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                tag.getFloat(
                                        "CycleProgress"
                                )
                        )
                );
        active =
                tag.getBoolean(
                        "Active"
                );

        clientCycleProgress =
                cycleProgress;
        clientCycleSyncTime =
                level == null
                        ? Float.NaN
                        : (float) level
                        .getGameTime();
    }

    @Nullable
    private Direction readDirection(
            String value) {
        if (value == null
                || value.isBlank()) {
            return null;
        }

        Direction direction =
                Direction.byName(value);

        return direction != null
                && direction.getAxis()
                .isHorizontal()
                ? direction
                : null;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition)
                .inflate(1.5D, 1.0D, 1.5D);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener>
    getUpdatePacket() {
        return ClientboundBlockEntityDataPacket
                .create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag =
                new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void onDataPacket(
            Connection net,
            ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag =
                packet.getTag();

        if (tag != null) {
            load(tag);
        }
    }

    private void sync() {
        if (level != null) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_CLIENTS
            );
        }
    }

    @Override
    public void setRemoved() {
        stopMechanicalLoad();
        super.setRemoved();
    }

    public enum SideRole {
        NONE("None"),
        SOURCE("Source (orange)"),
        DESTINATION("Destination (blue)");

        private final String displayName;

        SideRole(String displayName) {
            this.displayName =
                    displayName;
        }

        public String displayName() {
            return displayName;
        }
    }
}
