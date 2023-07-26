package com.magneticraft2.common.item.general.ingots.copper;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

/**
 * @author JumpWatch on 16-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class copperingot_low extends Item {
    public copperingot_low(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public void appendHoverText(ItemStack p_41421_, @Nullable Level p_41422_, List<Component> p_41423_, TooltipFlag p_41424_) {
        if (Screen.hasShiftDown()){
            p_41423_.add(Component.translatable("tooltip.magneticraft2.copper_ingot_low"));
        } else {
            p_41423_.add(Component.translatable("tooltip.magneticraft2.press_shift"));
        }
        super.appendHoverText(p_41421_, p_41422_, p_41423_, p_41424_);
    }

}
