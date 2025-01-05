package com.magneticraft2.common.item.creativeitems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author JumpWatch on 14-11-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class pitkiln_item extends BlockItem {
    public pitkiln_item(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }
    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        pTooltip.add(Component.translatable("tooltip.magneticraft2.pitkiln"));
        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        item.setCount(0);
        return super.onDroppedByPlayer(item, player);
    }
}
