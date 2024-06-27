package com.magneticraft2.common.item.general;

import net.minecraft.Util;
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
import java.util.Objects;

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
            if (player != null && player.isShiftKeyDown()){
                tag.putLong("pos1", blockPos.asLong());
                pContext.getPlayer().displayClientMessage(Component.translatable("message.magneticraft2.blueprintpos1", "§5" + pContext.getClickedPos().getX(), "§5" + pContext.getClickedPos().getY(), "§5" + pContext.getClickedPos().getZ()), true);
            }else {
                tag.putLong("pos2", blockPos.asLong());
                pContext.getPlayer().displayClientMessage(Component.translatable("message.magneticraft2.blueprintpos2", "§5" + pContext.getClickedPos().getX(), "§5" + pContext.getClickedPos().getY(), "§5" + pContext.getClickedPos().getZ()), true);

            }

        }
        return super.useOn(pContext);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        if (pStack.getTag() != null) {
            if (pStack.getTag().contains("pos1") && pStack.getTag().contains("pos2")) {
                pTooltipComponents.add(Component.translatable("tooltip.magneticraft2.blueprint.pos1andpos21", BlockPos.of(pStack.getTag().getLong("pos1")).toString().replace("BlockPos{", "").replace("}", "")));
                pTooltipComponents.add(Component.translatable("tooltip.magneticraft2.blueprint.pos1andpos22", BlockPos.of(pStack.getTag().getLong("pos2")).toString().replace("BlockPos{", "").replace("}", "")));
            } else if (pStack.getTag().contains("pos1")) {
                pTooltipComponents.add(Component.translatable("tooltip.magneticraft2.blueprint.pos1", BlockPos.of(pStack.getTag().getLong("pos1")).toString().replace("BlockPos{", "").replace("}", "")));
            } else if (pStack.getTag().contains("pos2")) {
                pTooltipComponents.add(Component.translatable("tooltip.magneticraft2.blueprint.pos2", BlockPos.of(pStack.getTag().getLong("pos2")).toString().replace("BlockPos{", "").replace("}", "")));
            }
        }
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }
}
