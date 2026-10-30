package com.magneticraft2.common.blockentity.stage.bronze;

import com.magneticraft2.common.registry.registers.BlockEntityRegistry;
import com.magneticraft2.common.registry.registers.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Physical, GUI-less sand mold press.
 * Slot 0 reusable wooden pattern; 1 sand; 2 clay; 3 finished mold.
 * Four empty-hand presses produce one consumable green sand mold.
 */
public class BronzeMoldTableBlockEntity extends BlockEntity {
    private static final int REQUIRED_PRESSES = 4;
    private int presses;
    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0 -> stack.is(ItemRegistry.ITEM_WOODEN_GEAR_PATTERN.get())
                        || stack.is(ItemRegistry.ITEM_WOODEN_BEARING_PATTERN.get());
                case 1 -> stack.is(Items.SAND) || stack.is(Items.RED_SAND);
                case 2 -> stack.is(Items.CLAY_BALL);
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? 1 : super.getSlotLimit(slot);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot < 3) presses = 0;
            sync();
        }
    };

    public BronzeMoldTableBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.BRONZE_MOLD_TABLE_BE.get(), pos, state);
    }

    public ItemStack getDisplayStack(int slot) {
        return inventory.getStackInSlot(slot);
    }

    public int getPresses() {
        return presses;
    }

    private ItemStack currentMold() {
        ItemStack pattern = inventory.getStackInSlot(0);
        if (pattern.is(ItemRegistry.ITEM_WOODEN_GEAR_PATTERN.get())) {
            return new ItemStack(ItemRegistry.ITEM_GEAR_SAND_MOLD.get());
        }
        if (pattern.is(ItemRegistry.ITEM_WOODEN_BEARING_PATTERN.get())) {
            return new ItemStack(ItemRegistry.ITEM_BEARING_SAND_MOLD.get());
        }
        return ItemStack.EMPTY;
    }

    private boolean ready() {
        ItemStack product = currentMold();
        ItemStack output = inventory.getStackInSlot(3);
        return !product.isEmpty() && !inventory.getStackInSlot(1).isEmpty()
                && !inventory.getStackInSlot(2).isEmpty()
                && (output.isEmpty() || (ItemStack.isSameItemSameTags(output, product)
                && output.getCount() < Math.min(output.getMaxStackSize(),
                inventory.getSlotLimit(3))));
    }

    public boolean handleUse(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (held.isEmpty()) {
            if (player.isShiftKeyDown()) {
                // Recover pattern or unused raw materials without breaking table.
                for (int slot = 0; slot < 3; slot++) {
                    if (!inventory.getStackInSlot(slot).isEmpty()) {
                        deliver(player, inventory.extractItem(slot, 64, false));
                        return true;
                    }
                }
            }

            if (!inventory.getStackInSlot(3).isEmpty()) {
                deliver(player, inventory.extractItem(3, 64, false));
                return true;
            }

            if (!ready()) {
                player.displayClientMessage(Component.translatable(
                        "message.magneticraft2.mold_table.missing"), true);
                return true;
            }

            presses++;
            if (level != null) {
                level.playSound(null, worldPosition, SoundEvents.WOOD_HIT,
                        SoundSource.BLOCKS, 0.6F, 0.75F);
            }

            if (presses >= REQUIRED_PRESSES) {
                ItemStack mold = currentMold();
                presses = 0;
                // Only a completed mold consumes binder and sand. The wooden
                // pattern in slot 0 stays installed and can be reused.
                if (ready()) {
                    inventory.extractItem(1, 1, false);
                    inventory.extractItem(2, 1, false);
                    ItemStack output = inventory.getStackInSlot(3);
                    if (output.isEmpty()) {
                        inventory.setStackInSlot(3, mold);
                    } else {
                        ItemStack grown = output.copy();
                        grown.grow(1);
                        inventory.setStackInSlot(3, grown);
                    }
                    player.displayClientMessage(Component.translatable(
                            "message.magneticraft2.mold_table.finished"), true);
                }
            } else {
                player.displayClientMessage(Component.translatable(
                        "message.magneticraft2.mold_table.presses",
                        presses, REQUIRED_PRESSES), true);
            }
            sync();
            return true;
        }

        int slot;
        if (held.is(ItemRegistry.ITEM_WOODEN_GEAR_PATTERN.get())
                || held.is(ItemRegistry.ITEM_WOODEN_BEARING_PATTERN.get())) slot = 0;
        else if (held.is(Items.SAND) || held.is(Items.RED_SAND)) slot = 1;
        else if (held.is(Items.CLAY_BALL)) slot = 2;
        else return false;

        ItemStack one = held.copy();
        one.setCount(1);
        ItemStack remainder = inventory.insertItem(slot, one, false);
        if (remainder.isEmpty()) {
            if (!player.getAbilities().instabuild) held.shrink(1);
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft2.mold_table.slot_full"), true);
        }
        return true;
    }

    private void deliver(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, worldPosition, stack.copy());
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(),
                    getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Items"));
        presses = Math.max(0, Math.min(REQUIRED_PRESSES - 1, tag.getInt("Presses")));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", inventory.serializeNBT());
        tag.putInt("Presses", presses);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection network,
                             ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) load(packet.getTag());
    }
}
