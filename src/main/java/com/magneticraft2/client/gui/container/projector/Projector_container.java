package com.magneticraft2.client.gui.container.projector;

import com.magneticraft2.common.blockentity.general.projectortestBlockEntity;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.registry.registers.ContainerAndScreenRegistry;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintManager;
import com.magneticraft2.common.systems.Blueprint.json.BlueprintRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * @author JumpWatch on 28-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class Projector_container extends AbstractContainerMenu {

    public BlockEntity blockEntity;
    private Player player;
    private IItemHandler itemHandler;
    private final DataSlot selectedBlueprintIndex = DataSlot.standalone();

    public Projector_container(int windowid, Level world, BlockPos pos, Inventory playerInventory, Player player) {
        super(ContainerAndScreenRegistry.Projector_container.get(), windowid);
        blockEntity = world.getBlockEntity(pos);
        this.player = player;
        this.itemHandler = new InvWrapper(playerInventory);
//        layoutPlayerInventorySlots(8, 84);
//        if (blockEntity != null){
//            blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h -> {
//                this.addSlot(new SlotItemHandler(h, 0, 53, 12)); //Slot 1
//                this.addSlot(new SlotItemHandler(h, 1, 53, 54)); //Slot 2
//                this.addSlot(new SlotItemHandler(h, 2, 107, 33)); //Slot 3
//                this.addSlot(new SlotItemHandler(h, 3, 107, 54)); //Slot 4
//            });
//        }
    }
    public Player getPlayer(){
        return player;
    }

    @Override
    public boolean clickMenuButton(Player pPlayer, int pId) {
        return super.clickMenuButton(pPlayer, pId);
    }

    @Override
    public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
        return null;
    }

    @Override
    public boolean stillValid(Player p_38874_) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, BlockRegistry.protectortest.get());
    }
    public BlockEntity getBlockEntity(){
        return blockEntity;
    }
    public projectortestBlockEntity getprojector(){
        projectortestBlockEntity block = (projectortestBlockEntity) blockEntity;
        return block;
    }

    private int addSlotRange(IItemHandler handler, int index, int x, int y, int amount, int dx) {
        for (int i = 0 ; i < amount ; i++) {
            addSlot(new SlotItemHandler(handler, index, x, y));
            x += dx;
            index++;
        }
        return index;
    }
    public int getNumRec(){
        return BlueprintRegistry.getRegisteredBlueprints().size();
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
}
