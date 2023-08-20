package com.magneticraft2.common.item.general;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author JumpWatch on 20-08-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class Blueprintmarker extends Item {
    private static final Logger LOGGER = LogManager.getLogger("MGC2-blueprintmarker");

    public Blueprintmarker() {
        super(new Properties().stacksTo(1).setNoRepair());
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        if (pContext.getLevel().isClientSide) InteractionResultHolder.pass(pContext.getPlayer());
        else {
            BlockPos blockPos = pContext.getClickedPos();

            ItemStack itemStack = pContext.getItemInHand();
            CompoundTag tag = itemStack.getOrCreateTag();
            itemStack.setTag(tag);
            Player player = pContext.getPlayer();
            if (player.isShiftKeyDown()){
                tag.putString("pos1", blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ());
            }else {
                tag.putString("pos2", blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ());
            }

        }
        return super.useOn(pContext);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        if (pStack.getTag() != null) {
            String pos1 = pStack.getTag().getString("pos1");
            String pos2 = pStack.getTag().getString("pos2");
            if (!pos1.isEmpty() && !pos2.isEmpty()) {
                pTooltipComponents.add(Component.translatable("tooltip.magneticraft2.blueprint.pos1andpos2", pStack.getTag().getString("pos1"), pStack.getTag().getString("pos2")));
            } else if (!pos1.isEmpty()) {
                pTooltipComponents.add(Component.translatable("tooltip.magneticraft2.blueprint.pos1", pStack.getTag().getString("pos1")));
            } else if (!pos2.isEmpty()) {
                pTooltipComponents.add(Component.translatable("tooltip.magneticraft2.blueprint.pos2", pStack.getTag().getString("pos2")));
            }
        }
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }
}
