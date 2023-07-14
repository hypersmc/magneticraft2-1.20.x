package com.magneticraft2.client.render.blocks;

import com.magneticraft2.common.blockentity.stage.stone.PitKilnBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author JumpWatch on 01-07-2023
 * @Project mgc2-1.20
 * v1.0.0
 */
public class PitKilnBlockEntityRenderer implements BlockEntityRenderer<PitKilnBlockEntity> {
    private static final Logger LOGGER = LogManager.getLogger("Pitrender");
    public PitKilnBlockEntityRenderer(BlockEntityRendererProvider.Context context) {

    }
    @Override
    public void render(PitKilnBlockEntity blockEntity, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {

        matrixStack.pushPose();
        matrixStack.translate(0.5D, 0.5D, 0.5D);
        for (int i = 2; i <= 5; i++) {
            IItemHandler itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                matrixStack.pushPose();
                matrixStack.translate((i - 2) % 2 * 0.6D - 0.3D, -0.25D, (i - 2) / 2 * 0.6D - 0.3D);
                matrixStack.scale(0.5F, 0.5F, 0.5F);
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, combinedLight, combinedOverlay, matrixStack, buffer, blockEntity.getLevel() , 0);
                matrixStack.popPose();
            }
        }
        matrixStack.popPose();
    }
}
