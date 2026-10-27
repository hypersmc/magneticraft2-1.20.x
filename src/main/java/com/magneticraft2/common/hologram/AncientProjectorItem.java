package com.magneticraft2.common.hologram;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;

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

}
