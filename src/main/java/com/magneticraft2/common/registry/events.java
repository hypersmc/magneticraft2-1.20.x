package com.magneticraft2.common.registry;

import com.magneticraft2.common.magneticraft2;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.magneticraft2.common.magneticraft2.MOD_ID;
import static net.minecraft.world.item.Items.COPPER_INGOT;

/**
 * @author JumpWatch on 27-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
@Mod.EventBusSubscriber(modid = MOD_ID,value = Dist.CLIENT)
public class events {

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event){
        if (event.getItemStack().getItem().asItem().equals(COPPER_INGOT)){
            if (Screen.hasShiftDown()){
                event.getToolTip().add(Component.translatable("tooltip.magneticraft2.copper_ingot_high"));
            } else {
                event.getToolTip().add(Component.translatable("tooltip.magneticraft2.press_shift"));

            }

        }
    }
}
