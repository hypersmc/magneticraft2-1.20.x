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
        // Teaser tooltip: a few corrupted archive glyphs suggest the recording
        // is damaged, while the item's name and the clue remain recognizable.
        // Minecraft's obfuscated text animates automatically; there is no tick
        // handler, client-side randomization or extra renderer involved.
        lines.add(Component.literal("Archive 0")
                .withStyle(net.minecraft.ChatFormatting.GRAY)
                .append(Component.literal("17").withStyle(net.minecraft.ChatFormatting.OBFUSCATED))
                .append(Component.literal(" - Mechanical Trans"))
                .append(Component.literal("mission").withStyle(net.minecraft.ChatFormatting.OBFUSCATED)));
        lines.add(Component.literal("An ins")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
                .append(Component.literal("cription").withStyle(net.minecraft.ChatFormatting.OBFUSCATED))
                .append(Component.literal(" is etched along its side.")));
        lines.add(Component.literal("3 x ")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
                .append(Component.literal("2").withStyle(net.minecraft.ChatFormatting.OBFUSCATED))
                .append(Component.literal(" x 2")));
    }
}
