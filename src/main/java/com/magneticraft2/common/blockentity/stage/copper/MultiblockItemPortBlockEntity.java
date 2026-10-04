package com.magneticraft2.common.blockentity.stage.copper;

import com.magneticraft2.common.block.stage.copper.MultiblockItemPortBlock;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.systems.Multiblocking.core.IMultiblockModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Item I/O module that forwards directly into the formed controller inventory.
 *
 * Input port -> controller slot 0, insertion only.
 * Output port -> controller slots 1 and 2, extraction only.
 */
public class MultiblockItemPortBlockEntity
        extends BlockEntity
        implements IMultiblockModule {

    private int controllerX;
    private int controllerY;
    private int controllerZ;
    private boolean formedModule;

    private LazyOptional<IItemHandler> itemCapability =
            LazyOptional.of(
                    () -> new PortItemHandler()
            );

    public MultiblockItemPortBlockEntity(
            BlockPos pos,
            BlockState state) {
        super(
                BlockEntityRegistry
                        .MULTIBLOCK_ITEM_PORT_BE
                        .get(),
                pos,
                state
        );
    }

    public boolean isOutputPort() {
        return getBlockState().getBlock()
                == BlockRegistry
                        .MULTIBLOCK_ITEM_OUTPUT
                        .get();
    }

    @Nullable
    public BlockPos getControllerPos() {
        if (!formedModule) {
            return null;
        }

        return new BlockPos(
                controllerX,
                controllerY,
                controllerZ
        );
    }

    @Nullable
    private BaseBlockEntityMagneticraft2 getController() {
        if (level == null || !formedModule) {
            return null;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(
                        new BlockPos(
                                controllerX,
                                controllerY,
                                controllerZ
                        )
                );

        return blockEntity
                instanceof BaseBlockEntityMagneticraft2 controller
                ? controller
                : null;
    }

    public boolean insertFromPlayer(ItemStack stack) {
        if (isOutputPort()) {
            return false;
        }

        BaseBlockEntityMagneticraft2 controller =
                getController();

        if (controller == null
                || controller.itemHandler == null) {
            return false;
        }

        return controller.itemHandler
                .insertItem(
                        0,
                        stack,
                        false
                )
                .isEmpty();
    }

    public ItemStack extractForPlayer() {
        if (!isOutputPort()) {
            return ItemStack.EMPTY;
        }

        BaseBlockEntityMagneticraft2 controller =
                getController();

        if (controller == null
                || controller.itemHandler == null) {
            return ItemStack.EMPTY;
        }

        ItemStack primary =
                controller.itemHandler
                        .extractItem(
                                1,
                                64,
                                false
                        );

        if (!primary.isEmpty()) {
            return primary;
        }

        return controller.itemHandler
                .extractItem(
                        2,
                        64,
                        false
                );
    }

    @Override
    public boolean isValid(Level world, BlockPos pos) {
        return world.getBlockEntity(pos) == this;
    }

    @Override
    public void onActivate(Level world, BlockPos pos) {
        if (world.isClientSide) {
            return;
        }

        formedModule = true;
        setChanged();
        sync();
    }

    @Override
    public void onDeactivate(Level world, BlockPos pos) {
        if (world.isClientSide) {
            return;
        }

        formedModule = false;
        setChanged();
        sync();
    }

    @Override
    public String getModuleKey() {
        return isOutputPort()
                ? "item_output"
                : "item_input";
    }

    @Override
    public BlockPos getModuleOffset() {
        return worldPosition;
    }

    @Override
    public IMultiblockModule createModule(
            Level world,
            BlockPos pos) {
        return this;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap,
            @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCapability.cast();
        }

        return super.getCapability(
                cap,
                side
        );
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemCapability =
                LazyOptional.of(
                        () -> new PortItemHandler()
                );
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        controllerX =
                tag.getInt("controller_x");
        controllerY =
                tag.getInt("controller_y");
        controllerZ =
                tag.getInt("controller_z");
        formedModule =
                tag.getBoolean("isformed");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(
                "controller_x",
                controllerX
        );
        tag.putInt(
                "controller_y",
                controllerY
        );
        tag.putInt(
                "controller_z",
                controllerZ
        );
        tag.putBoolean(
                "isformed",
                formedModule
        );
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag =
                new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener>
    getUpdatePacket() {
        return ClientboundBlockEntityDataPacket
                .create(this);
    }

    @Override
    public void onDataPacket(
            Connection net,
            ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag =
                packet.getTag();

        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    private void sync() {
        if (level != null) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_ALL
            );
        }
    }

    private final class PortItemHandler
            implements IItemHandler {

        @Override
        public int getSlots() {
            return isOutputPort()
                    ? 2
                    : 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(
                int slot) {
            BaseBlockEntityMagneticraft2 controller =
                    getController();

            if (controller == null
                    || controller.itemHandler == null) {
                return ItemStack.EMPTY;
            }

            int mapped =
                    mapSlot(slot);

            if (mapped < 0
                    || mapped
                    >= controller.itemHandler
                    .getSlots()) {
                return ItemStack.EMPTY;
            }

            return controller.itemHandler
                    .getStackInSlot(mapped);
        }

        @Override
        public @NotNull ItemStack insertItem(
                int slot,
                @NotNull ItemStack stack,
                boolean simulate) {
            if (isOutputPort()) {
                return stack;
            }

            BaseBlockEntityMagneticraft2 controller =
                    getController();

            if (controller == null
                    || controller.itemHandler == null) {
                return stack;
            }

            return controller.itemHandler
                    .insertItem(
                            0,
                            stack,
                            simulate
                    );
        }

        @Override
        public @NotNull ItemStack extractItem(
                int slot,
                int amount,
                boolean simulate) {
            if (!isOutputPort()) {
                return ItemStack.EMPTY;
            }

            BaseBlockEntityMagneticraft2 controller =
                    getController();

            if (controller == null
                    || controller.itemHandler == null) {
                return ItemStack.EMPTY;
            }

            int mapped =
                    mapSlot(slot);

            if (mapped < 0
                    || mapped
                    >= controller.itemHandler
                    .getSlots()) {
                return ItemStack.EMPTY;
            }

            return controller.itemHandler
                    .extractItem(
                            mapped,
                            amount,
                            simulate
                    );
        }

        @Override
        public int getSlotLimit(int slot) {
            BaseBlockEntityMagneticraft2 controller =
                    getController();

            if (controller == null
                    || controller.itemHandler == null) {
                return 64;
            }

            int mapped =
                    mapSlot(slot);

            return mapped >= 0
                    && mapped
                    < controller.itemHandler
                    .getSlots()
                    ? controller.itemHandler
                    .getSlotLimit(mapped)
                    : 64;
        }

        @Override
        public boolean isItemValid(
                int slot,
                @NotNull ItemStack stack) {
            if (isOutputPort()) {
                return false;
            }

            BaseBlockEntityMagneticraft2 controller =
                    getController();

            return controller != null
                    && controller.itemHandler != null
                    && controller.itemHandler
                    .isItemValid(
                            0,
                            stack
                    );
        }

        private int mapSlot(int slot) {
            if (!isOutputPort()) {
                return slot == 0
                        ? 0
                        : -1;
            }

            return switch (slot) {
                case 0 -> 1;
                case 1 -> 2;
                default -> -1;
            };
        }
    }
}
