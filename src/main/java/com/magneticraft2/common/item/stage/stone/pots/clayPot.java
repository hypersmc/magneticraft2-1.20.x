package com.magneticraft2.common.item.stage.stone.pots;

import com.magneticraft2.common.blockentity.stage.stone.PitKilnBlockEntity;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author JumpWatch on 01-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class clayPot extends Item {
    private static final Logger LOGGER = LogManager.getLogger("ClayPOT");
    public clayPot() {
        super(new Properties().stacksTo(1).setNoRepair());
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        double x = context.getClickLocation().x();
        double y = context.getClickLocation().y();
        double z = context.getClickLocation().z();

        InteractionHand hand = context.getPlayer().getUsedItemHand();
        ItemStack heldStack = player.getItemInHand(hand);
        // Check if the player is crouching
        if (player.isCrouching()){

            // get pos of the looked block
            BlockPos pos =  new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            LOGGER.info(pos);
            LOGGER.info(isAdjacentSolid(world, pos));
            LOGGER.info(world.getBlockState(pos.below()).getBlock());
            LOGGER.info(world.getBlockState(pos.below()).getBlock() != Blocks.AIR);
            LOGGER.info(world.isEmptyBlock(pos));
            //check if the block is a solid and the block above is air
            if (world.isEmptyBlock(pos) && world.getBlockState(pos.below()).getBlock() != Blocks.AIR && isAdjacentSolid(world, pos)){
                //place the block
                LOGGER.info("should be able to set.");
                world.setBlockAndUpdate(pos, BlockRegistry.PitKilnblock.get().defaultBlockState());
                // Get the block entity and item handler for the pit kiln
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof PitKilnBlockEntity) {
                    IItemHandler itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
                    // Try to insert the held item into the next available slot
                    for (int i = 2; i <= 5; i++) {
                        if (itemHandler.getStackInSlot(i).isEmpty()) {
                            itemHandler.insertItem(i, heldStack.copy().split(1), false);
                            heldStack.shrink(1);
                            break;
                        }
                    }
                }
            }
        }
        return super.onItemUseFirst(stack, context);
    }
    private boolean isAdjacentSolid(Level world, BlockPos pos) {
        return world.getBlockState(pos.north()).getBlock() != Blocks.AIR &&
                world.getBlockState(pos.south()).getBlock() != Blocks.AIR &&
                world.getBlockState(pos.east()).getBlock() != Blocks.AIR &&
                world.getBlockState(pos.west()).getBlock() != Blocks.AIR;
    }

}
