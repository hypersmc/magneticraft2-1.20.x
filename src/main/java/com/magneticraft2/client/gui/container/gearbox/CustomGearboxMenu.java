package com.magneticraft2.client.gui.container.gearbox;

import com.magneticraft2.common.blockentity.stage.copper.CustomGearboxBlockEntity_wood;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.magneticraft2.common.registry.registers.ContainerAndScreenRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Slotless configuration menu for the custom wooden gearbox.
 */
public class CustomGearboxMenu extends AbstractContainerMenu {
    private final BlockEntity blockEntity;
    private final Player player;

    public CustomGearboxMenu(int containerId,
                             Level level,
                             BlockPos pos,
                             Inventory inventory,
                             Player player) {
        super(
                ContainerAndScreenRegistry.CUSTOM_GEARBOX_MENU.get(),
                containerId
        );
        this.blockEntity = level.getBlockEntity(pos);
        this.player = player;
    }

    public BlockPos getBlockEntityPos() {
        return blockEntity == null
                ? BlockPos.ZERO
                : blockEntity.getBlockPos();
    }

    public CustomGearboxBlockEntity_wood getGearbox() {
        return blockEntity instanceof CustomGearboxBlockEntity_wood gearbox
                ? gearbox
                : null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() == null) {
            return false;
        }

        return stillValid(
                ContainerLevelAccess.create(
                        blockEntity.getLevel(),
                        blockEntity.getBlockPos()
                ),
                player,
                BlockRegistry.CUSTOM_GEARBOX_WOOD.get()
        );
    }
}
