package com.magneticraft2.client.gui.container.primitivefurnace;

import com.magneticraft2.common.blockentity.stage.stone.PrimitiveFurnaceMultiblockEntity;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.registry.registers.ContainerAndScreenRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author JumpWatch on 14-11-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class primitivefurnace_container extends AbstractContainerMenu {
    public BlockEntity blockEntity;
    private Player player;
    private IItemHandler itemHandler;

    public primitivefurnace_container(int windowid, Level world, BlockPos pos, Inventory playerInventory, Player player) {
        super(ContainerAndScreenRegistry.Primitivefurnace_container.get(), windowid);
        blockEntity = world.getBlockEntity(pos);
        this.player = player;
        this.itemHandler = new InvWrapper(playerInventory);
        layoutPlayerInventorySlots(8, 84);
        if (blockEntity != null){
            blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h -> {
                this.addSlot(new SlotItemHandler(h, 0, 53, 12)); //Slot 1
                this.addSlot(new SlotItemHandler(h, 1, 53, 54)); //Slot 2
                this.addSlot(new SlotItemHandler(h, 2, 107, 33){
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return false;
                    }
                }); //Slot 3 output
                this.addSlot(new SlotItemHandler(h, 3, 107, 54){
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return false;
                    }
                }); //Slot 4 output
            });
        }
    }


    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, BlockRegistry.primitivefurnacemultiblock.get());
    }
    private int addSlotRange(IItemHandler handler, int index, int x, int y, int amount, int dx) {
        for (int i = 0 ; i < amount ; i++) {
            addSlot(new SlotItemHandler(handler, index, x, y));
            x += dx;
            index++;
        }
        return index;
    }

    public int addSlotBox(IItemHandler handler, int index, int x, int y, int horAmount, int dx, int verAmount, int dy) {
        for (int j = 0 ; j < verAmount ; j++) {
            index = addSlotRange(handler, index, x, y, horAmount, dx);
            y += dy;
        }
        return index;
    }
    private void layoutPlayerInventorySlots(int leftCol, int topRow){
        addSlotBox(itemHandler, 9, leftCol, topRow, 9, 18, 3, 18);
        topRow += 58;
        addSlotRange(itemHandler, 0, leftCol, topRow, 9, 18);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int slotIndex) {
        {
            Slot slot = this.slots.get(slotIndex);
            if (slot == null || !slot.hasItem())
            {
                return ItemStack.EMPTY;
            }

            ItemStack stack = slot.getItem();
            assert stack.getCount() > 0;
            ItemStack stackCopy = stack.copy();

            int startIndex;
            int endIndex;

            if (slotIndex >= 3)
            {
                if (slotIndex < (27 + 3))
                {
                    startIndex = 27 + 3;
                    endIndex = startIndex + 9;
                }
                else if (slotIndex >= (27 + 3))
                {
                    startIndex = 3;
                    endIndex = startIndex + 27;
                }
                else
                {
                    return ItemStack.EMPTY;
                }
            }
            else
            {
                startIndex = 3;
                endIndex = startIndex + 9 * 4;
            }

            if (!this.moveItemStackTo(stack, startIndex, endIndex, false))
            {
                return ItemStack.EMPTY;
            }

            if (stack.getCount() == 0)
            {
                slot.set(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }

            if (stack.getCount() == stackCopy.getCount())
            {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
            return stackCopy;
        }
    }
    public PrimitiveFurnaceMultiblockEntity getMultiblockEntity() {
        return (PrimitiveFurnaceMultiblockEntity) blockEntity;
    }

    //    @Override
//    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
//        ItemStack itemStack = ItemStack.EMPTY;
//        Slot slot = this.slots.get(pIndex);
//        if (slot != null && slot.hasItem()){
//            ItemStack itemStack1 = slot.getItem();
//            itemStack = itemStack1.copy();
//            if (this.slots.get(1).hasItem() || !this.slots.get(1).mayPlace(itemStack1)){
//                return ItemStack.EMPTY;
//            }
//            ItemStack itemStack2 = itemStack1.copyWithCount(1);
//            itemStack1.shrink(1);
//            this.slots.get(1).setByPlayer(itemStack2);
//            if (itemStack1.isEmpty()) {
//                slot.setByPlayer(ItemStack.EMPTY);
//            } else {
//                slot.setChanged();
//            }
//
//            if (itemStack1.getCount() == itemStack.getCount()) {
//                return ItemStack.EMPTY;
//            }
//            slot.onTake(pPlayer,itemStack1);
//        }
//        return itemStack;
//    }
}
