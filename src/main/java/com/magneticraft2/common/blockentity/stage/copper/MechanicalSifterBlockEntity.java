package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalSifterBlock;
import com.magneticraft2.common.recipe.stage.copper.MechanicalSifterRecipe;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.systems.GEAR.GearNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MechanicalSifterBlockEntity extends BlockEntity {
    private static final float EPSILON = 0.01F;

    private final ItemStackHandler itemHandler =
            new ItemStackHandler(3) {
                @Override
                protected void onContentsChanged(int slot) {
                    setChanged();
                    sync();
                }

                @Override
                public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                    return slot == 0;
                }
            };

    private LazyOptional<IItemHandler> items =
            LazyOptional.of(() -> itemHandler);

    private int processTime = 0;
    private int totalProcessTime = 140;
    private boolean processing = false;

    public MechanicalSifterBlockEntity(BlockPos pos, BlockState state) {
        super(
                BlockEntityRegistry.MECHANICAL_SIFTER_BE.get(),
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
                || !(blockEntity instanceof MechanicalSifterBlockEntity sifter)) {
            return;
        }

        sifter.tickProcessing();
    }

    private void tickProcessing() {
        if (level == null) {
            return;
        }

        MechanicalSifterRecipe recipe =
                getMatchingRecipe();
        CrankBlockEntity_wood crank =
                getConnectedCrank();

        if (recipe == null) {
            setLoad(false, crank, 0.0F);
            processTime = 0;
            totalProcessTime = 140;
            setProcessing(false);
            return;
        }

        ItemStack output = recipe.getOutput();
        ItemStack byproduct = recipe.getByproduct();

        if (!canAccept(1, output)
                || (!byproduct.isEmpty()
                && !canAccept(2, byproduct))) {
            setLoad(false, crank, recipe.getTorque());
            setProcessing(false);
            return;
        }

        boolean canAttempt =
                crank != null
                        && crank.getOrCreateGearNode()
                        .getEffectiveSpeed()
                        >= recipe.getMinSpeed()
                        && crank.getOrCreateGearNode()
                        .getTorque()
                        + EPSILON
                        >= recipe.getTorque();

        setLoad(canAttempt, crank, recipe.getTorque());

        GearNetworkManager.MechanicalLoadState loadState =
                GearNetworkManager.getInstance()
                        .getMechanicalLoadState(level, worldPosition);

        if (!canAttempt || !loadState.supplied()) {
            setProcessing(false);
            return;
        }

        totalProcessTime = recipe.getProcessTime();
        setProcessing(true);
        processTime++;
        setChanged();

        if (processTime < totalProcessTime) {
            return;
        }

        itemHandler.extractItem(0, 1, false);
        insertOutput(1, output);

        if (!byproduct.isEmpty()
                && level.random.nextFloat()
                < recipe.getByproductChance()) {
            insertOutput(2, byproduct);
        }

        processTime = 0;
        setProcessing(false);
        sync();
    }

    private void setLoad(
            boolean active,
            @Nullable CrankBlockEntity_wood crank,
            float torque) {
        if (level == null) {
            return;
        }

        GearNetworkManager.getInstance()
                .setMechanicalLoad(
                        level,
                        worldPosition,
                        crank == null
                                ? worldPosition
                                : crank.getBlockPos(),
                        Math.max(0.0F, torque),
                        active && crank != null
                );
    }

    @Nullable
    public CrankBlockEntity_wood getConnectedCrank() {
        if (level == null) {
            return null;
        }

        Direction facing = getFacing();
        BlockPos crankPos =
                worldPosition.relative(
                        facing.getOpposite()
                );

        BlockEntity blockEntity =
                level.getBlockEntity(crankPos);

        if (blockEntity
                instanceof CrankBlockEntity_wood crank
                && crank.getRodOutputPos().equals(worldPosition)) {
            return crank;
        }

        return null;
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(MechanicalSifterBlock.FACING)
                ? state.getValue(MechanicalSifterBlock.FACING)
                : Direction.SOUTH;
    }

    public float getShakeOffset(float partialTicks) {
        CrankBlockEntity_wood crank =
                getConnectedCrank();

        if (crank == null) {
            return 0.0F;
        }

        return (crank.getStrokeProgress(partialTicks)
                - 0.5F) * 0.18F;
    }

    @Nullable
    private MechanicalSifterRecipe getMatchingRecipe() {
        if (level == null
                || itemHandler.getStackInSlot(0).isEmpty()) {
            return null;
        }

        return level.getRecipeManager()
                .getRecipeFor(
                        MechanicalSifterRecipe.Type.INSTANCE,
                        new SimpleContainer(
                                itemHandler.getStackInSlot(0)
                        ),
                        level
                )
                .orElse(null);
    }

    public boolean insertInput(ItemStack stack) {
        return itemHandler.insertItem(
                0,
                stack,
                false
        ).isEmpty();
    }

    public ItemStack extractForPlayer() {
        ItemStack result =
                itemHandler.extractItem(1, 64, false);
        if (!result.isEmpty()) {
            return result;
        }

        result = itemHandler.extractItem(2, 64, false);
        if (!result.isEmpty()) {
            return result;
        }

        return itemHandler.extractItem(0, 64, false);
    }

    public void dropContents() {
        if (level == null || level.isClientSide) {
            return;
        }

        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack stack =
                    itemHandler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Block.popResource(
                        level,
                        worldPosition,
                        stack.copy()
                );
                itemHandler.setStackInSlot(
                        slot,
                        ItemStack.EMPTY
                );
            }
        }
    }

    private boolean canAccept(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        ItemStack current =
                itemHandler.getStackInSlot(slot);

        if (current.isEmpty()) {
            return stack.getCount()
                    <= itemHandler.getSlotLimit(slot);
        }

        if (!ItemStack.isSameItemSameTags(current, stack)) {
            return false;
        }

        int limit = Math.min(
                itemHandler.getSlotLimit(slot),
                current.getMaxStackSize()
        );

        return current.getCount()
                + stack.getCount()
                <= limit;
    }

    private void insertOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemStack current =
                itemHandler.getStackInSlot(slot);

        if (current.isEmpty()) {
            itemHandler.setStackInSlot(
                    slot,
                    stack.copy()
            );
        } else {
            current.grow(stack.getCount());
            itemHandler.setStackInSlot(slot, current);
        }
    }

    public ItemStack getInputStack() {
        return itemHandler.getStackInSlot(0);
    }

    public ItemStack getOutputStack() {
        return itemHandler.getStackInSlot(1);
    }

    public ItemStack getByproductStack() {
        return itemHandler.getStackInSlot(2);
    }

    public boolean isProcessing() {
        return processing;
    }

    public float getProcessProgress() {
        if (totalProcessTime <= 0) {
            return 0.0F;
        }

        return Math.max(
                0.0F,
                Math.min(
                        1.0F,
                        processTime
                                / (float) totalProcessTime
                )
        );
    }

    private void setProcessing(boolean value) {
        if (processing == value) {
            return;
        }
        processing = value;
        sync();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap,
            @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return items.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putInt("ProcessTime", processTime);
        tag.putInt("TotalProcessTime", totalProcessTime);
        tag.putBoolean("Processing", processing);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            itemHandler.deserializeNBT(
                    tag.getCompound("Inventory")
            );
        }
        processTime = tag.getInt("ProcessTime");
        totalProcessTime = tag.contains("TotalProcessTime")
                ? Math.max(1, tag.getInt("TotalProcessTime"))
                : 140;
        processing = tag.getBoolean("Processing");
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        items.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        items = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void onDataPacket(
            Connection net,
            ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_CLIENTS
            );
        }
    }
}
