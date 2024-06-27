package com.magneticraft2.common.blockitems;

import com.magneticraft2.common.block.general.BeltBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author JumpWatch on 08-01-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class beltblockitem extends BlockItem {
    private static final Logger LOGGER = LogManager.getLogger("MGC2-beltblockitem");

    public beltblockitem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }


    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        if (pContext.getLevel().isClientSide) InteractionResultHolder.pass(pContext.getPlayer());
        else {
            BlockPos blockPos = pContext.getClickedPos();
            Block block = pContext.getLevel().getBlockState(blockPos).getBlock();
            ItemStack itemStack = pContext.getItemInHand();
            CompoundTag tag = itemStack.getOrCreateTag();
            itemStack.setTag(tag);
            Player p = pContext.getPlayer();
            if (p != null && block instanceof BeltBlock && !p.isShiftKeyDown()){
                tag.putLong("connectedbelt", blockPos.asLong());
                pContext.getPlayer().displayClientMessage(Component.translatable("message.magneticraft2.beltconnected"), true);
                return InteractionResult.SUCCESS;
            }
            if (p != null && block instanceof BeltBlock && p.isShiftKeyDown()){
                tag.remove("connectedbelt");
                pContext.getPlayer().displayClientMessage(Component.translatable("message.magneticraft2.beltconnectionremoved"), true);
                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(pContext);
    }


}
