package com.magneticraft2.common.hologram;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import java.util.List;

public class AncientProjectorItem extends BlockItem {
    public AncientProjectorItem(Block block) {
        super(block, new Item.Properties().stacksTo(1));
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, net.minecraft.world.level.block.state.BlockState state) {
        boolean placed = super.placeBlock(context, state);
        if (placed && !context.getLevel().isClientSide()) {
            BlockPos pos = context.getClickedPos();
            if (context.getLevel().getBlockEntity(pos) instanceof AncientProjectorBlockEntity entity) {
                ItemStack stack = context.getItemInHand();
                if (stack.hasTag() && stack.getTag().contains("Recording")) {
                    net.minecraft.resources.ResourceLocation id =
                            net.minecraft.resources.ResourceLocation.tryParse(stack.getTag().getString("Recording"));
                    if (id != null) entity.setRecording(id);
                }
            }
        }
        return placed;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Archive 017 - Mechanical Transmission").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.literal("An inscription is etched along its side.").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        lines.add(Component.literal("8 x 4 x 7").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
