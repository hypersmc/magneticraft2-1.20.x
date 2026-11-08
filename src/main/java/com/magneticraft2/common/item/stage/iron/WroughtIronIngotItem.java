package com.magneticraft2.common.item.stage.iron;

import com.magneticraft2.common.systems.material.HotBloomWorkpiece;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Existing ingot registry ID with client-only display of server-owned heat. */
public final class WroughtIronIngotItem extends Item {
    public WroughtIronIngotItem() { super(new Item.Properties()); }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (level != null && HotBloomWorkpiece.isHot(stack, level)) {
            long seconds = (HotBloomWorkpiece.remainingTicks(stack, level) + 19L) / 20L;
            tooltip.add(Component.translatable(
                    "tooltip.magneticraft2.wrought_iron_ingot.hot", seconds)
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable(
                    "tooltip.magneticraft2.wrought_iron_ingot.cold")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
