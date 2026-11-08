package com.magneticraft2.common.item.stage.steel;

import com.magneticraft2.common.systems.material.HotBloomWorkpiece;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Existing Refined Steel Ingot ID and new Steel Bar ID use the same
 * server-owned hot-until tag. Tooltip is visual only, never a heat source.
 */
public final class SteelStockItem extends Item {
    public SteelStockItem() { super(new Item.Properties()); }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (level != null && HotBloomWorkpiece.isHot(stack, level)) {
            long seconds = (HotBloomWorkpiece.remainingTicks(stack, level) + 19L) / 20L;
            tooltip.add(Component.translatable(
                    "tooltip.magneticraft2.steel_stock.hot", seconds)
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("tooltip.magneticraft2.steel_stock.cold")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
