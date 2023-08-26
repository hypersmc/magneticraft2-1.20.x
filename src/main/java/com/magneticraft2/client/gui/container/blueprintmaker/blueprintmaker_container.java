package com.magneticraft2.client.gui.container.blueprintmaker;

import com.magneticraft2.common.blockentity.general.blueprintmakerBlockEntity;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.registry.registers.ContainerAndScreenRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

/**
 * @author JumpWatch on 20-08-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class blueprintmaker_container extends AbstractContainerMenu {
    public BlockEntity blockEntity;
    private Player player;
    private IItemHandler itemHandler;
    public blueprintmaker_container(int windowid, Level world, BlockPos pos, Inventory playerInventory, Player player) {
        super(ContainerAndScreenRegistry.Blueprintmaker_container.get(), windowid);
        blockEntity = world.getBlockEntity(pos);
        this.player = player;
        this.itemHandler = new InvWrapper(playerInventory);
        layoutPlayerInventorySlots(48, 139);
        if (blockEntity != null){
            blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h -> {
                this.addSlot(new SlotItemHandler(h, 0, 36, 50)); //Slot 1
            });
        }
    }


    public blueprintmakerBlockEntity getBlueprintmaker(){
        blueprintmakerBlockEntity entity = (blueprintmakerBlockEntity) blockEntity;
        return entity;
    }
    public long gettest(){
        BlockPos pos = blockEntity.getBlockPos();
        BlockEntity entity = blockEntity.getLevel().getBlockEntity(pos);
        blueprintmakerBlockEntity blockEntity2 = (blueprintmakerBlockEntity) entity;
        return blockEntity2.getPos2long();
    }
    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, BlockRegistry.blueprintmaker.get());
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
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        if (slot != null && slot.hasItem()){
            ItemStack itemStack1 = slot.getItem();
            itemStack = itemStack1.copy();
            if (this.slots.get(1).hasItem() || !this.slots.get(1).mayPlace(itemStack1)){
                return ItemStack.EMPTY;
            }
            ItemStack itemStack2 = itemStack1.copyWithCount(1);
            itemStack1.shrink(1);
            this.slots.get(1).setByPlayer(itemStack2);
            if (itemStack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemStack1.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(pPlayer,itemStack1);
        }
        return itemStack;
    }
}
