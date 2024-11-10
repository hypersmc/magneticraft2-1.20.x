package com.magneticraft2.common.item.creativeitems;

import com.magneticraft2.common.registry.registers.BlockRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author JumpWatch on 10-11-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class multiblock_filler_item extends BlockItem {
    public multiblock_filler_item(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        pTooltip.add(Component.translatable("tooltip.magneticraft2.how"));
        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);
    }
}