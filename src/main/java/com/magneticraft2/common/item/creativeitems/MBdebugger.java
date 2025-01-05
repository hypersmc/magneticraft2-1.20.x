package com.magneticraft2.common.item.creativeitems;

import com.magneticraft2.common.block.general.Multiblockfiller;
import com.magneticraft2.common.blockentity.general.BaseBlockEntityMagneticraft2;
import com.magneticraft2.common.systems.Multiblocking.core.IMultiblockModule;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockController;
import com.magneticraft2.common.systems.Multiblocking.core.MultiblockModule;
import com.magneticraft2.common.systems.Multiblocking.json.Multiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author JumpWatch on 16-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MBdebugger extends Item {
    public MBdebugger() {
        super(new Properties().stacksTo(1).setNoRepair());
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        Player player = pContext.getPlayer();

        // Ensure the action is on the server-side
        if (!level.isClientSide && player != null) {
            // Get the block position
            BlockPos pos = pContext.getClickedPos();
            // Get the block entity at the position
            BlockEntity blockEntity = level.getBlockEntity(pos);
            Block block = level.getBlockState(pos).getBlock();

            if (blockEntity != null) {
                // Check if the block entity is part of your multiblock system
                if (blockEntity instanceof BaseBlockEntityMagneticraft2) {
                    BaseBlockEntityMagneticraft2 multiblock = (BaseBlockEntityMagneticraft2) blockEntity;
                    MultiblockController controller = multiblock.getMultiblockController();
                    CompoundTag tag = blockEntity != null ? blockEntity.saveWithoutMetadata() : null;
                    // Retrieve and display useful info in chat
                    player.sendSystemMessage(Component.literal("Multiblock Debug Info:"));
                    player.sendSystemMessage(Component.literal(" - Structure Name: " + tag.getString("BlueprintName")));
                    player.sendSystemMessage(Component.literal(" - Controller Position: " + pos.toShortString()));
                    player.sendSystemMessage(Component.literal(" - Active: " + controller.getFormed()));

                    // Example for displaying NBT data
                    CompoundTag nbt = blockEntity.saveWithoutMetadata();
//                    player.sendSystemMessage(Component.literal(" - NBT Data: " + nbt.toString()));
                } else if (blockEntity instanceof IMultiblockModule) {
                    CompoundTag tag = blockEntity != null ? blockEntity.saveWithoutMetadata() : null;
                    if (tag != null && tag.contains("controller_x") && tag.contains("controller_y") && tag.contains("controller_z")) {
                        BlockPos controllerPos = new BlockPos(tag.getInt("controller_x"), tag.getInt("controller_y"), tag.getInt("controller_z"));
                        BlockEntity controllerEntity = level.getBlockEntity(controllerPos);
                        if (controllerEntity instanceof BaseBlockEntityMagneticraft2) {
                            BaseBlockEntityMagneticraft2 multiblock = (BaseBlockEntityMagneticraft2) controllerEntity;
                            MultiblockController controller = multiblock.getMultiblockController();
                            CompoundTag tagm = blockEntity != null ? controllerEntity.saveWithoutMetadata() : null;
                            player.sendSystemMessage(Component.literal("Multiblock Debug Info:"));
                            player.sendSystemMessage(Component.literal(" - Structure Name: " + tagm.getString("BlueprintName")));
                            player.sendSystemMessage(Component.literal(" - Controller Position: " + controllerPos.toShortString()));
                            player.sendSystemMessage(Component.literal(" - Active: " + controller.getFormed()));

                            // Example for displaying NBT data
                            CompoundTag nbt = blockEntity.saveWithoutMetadata();
//                            player.sendSystemMessage(Component.literal(" - NBT Data: " + nbt.toString()));

                        }
                    }
                } else if (block instanceof Multiblockfiller) {
                    CompoundTag tag = blockEntity != null ? blockEntity.saveWithoutMetadata() : null;
                    if (tag != null && tag.contains("controller_x") && tag.contains("controller_y") && tag.contains("controller_z")) {
                        BlockPos controllerPos = new BlockPos(tag.getInt("controller_x"), tag.getInt("controller_y"), tag.getInt("controller_z"));
                        BlockEntity controllerEntity = level.getBlockEntity(controllerPos);
                        if (controllerEntity instanceof BaseBlockEntityMagneticraft2) {
                            BaseBlockEntityMagneticraft2 multiblock = (BaseBlockEntityMagneticraft2) controllerEntity;
                            MultiblockController controller = multiblock.getMultiblockController();
                            CompoundTag tagm = blockEntity != null ? controllerEntity.saveWithoutMetadata() : null;
                            player.sendSystemMessage(Component.literal("Multiblock Debug Info:"));
                            player.sendSystemMessage(Component.literal(" - Structure Name: " + tagm.getString("BlueprintName")));
                            player.sendSystemMessage(Component.literal(" - Controller Position: " + controllerPos.toShortString()));
                            player.sendSystemMessage(Component.literal(" - Active: " + controller.getFormed()));

                            // Example for displaying NBT data
                            CompoundTag nbt = blockEntity.saveWithoutMetadata();
//                            player.sendSystemMessage(Component.literal(" - NBT Data: " + nbt.toString()));

                        }
                    }
                } else {
                    // Notify player if the block is not part of a multiblock
                    player.sendSystemMessage(Component.literal("This block is not part of a multiblock structure."));
                }
            } else {
                // Notify player if no block entity exists
                player.sendSystemMessage(Component.literal("No data found for this block."));
            }
        }

        return InteractionResult.SUCCESS;
    }


    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("tooltip.magneticraft2.debugger"));
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }
}
