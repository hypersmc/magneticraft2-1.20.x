package com.magneticraft2.common.item.stage.bronze;

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
 * Existing iron_bloom registry ID with a visible hot/cold distinction.
 * No temperature changes are made client-side; only the NBT timestamp
 * written by the server kiln determines Forge Hammer eligibility.
 */
public final class IronBloomItem extends Item {
    public IronBloomItem() { super(new Item.Properties()); }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (level != null && HotBloomWorkpiece.isHot(stack, level)) {
            long seconds = (HotBloomWorkpiece.remainingTicks(stack, level) + 19L) / 20L;
            tooltip.add(Component.translatable("tooltip.magneticraft2.iron_bloom.hot", seconds)
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("tooltip.magneticraft2.iron_bloom.cold")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
