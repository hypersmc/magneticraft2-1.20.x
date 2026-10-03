package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.block.stage.copper.WaterWheelBlock;
import com.magneticraft2.common.blockentity.stage.copper.WaterWheelBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/**
 * Rotates the complete wheel model from Gear V2's authoritative angular state.
 */
public class WaterWheelBlockEntityRenderer implements BlockEntityRenderer<WaterWheelBlockEntity> {
    public WaterWheelBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WaterWheelBlockEntity blockEntity,
                       float partialTicks,
                       PoseStack stack,
                       MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {
        if (!blockEntity.getBlockState().getValue(WaterWheelBlock.ACTIVE)) {
            return;
        }

        stack.pushPose();
        stack.translate(0.5D, 0.5D, 0.5D);

        float rotation = blockEntity.getVisualRotationDegrees(partialTicks);
        Direction.Axis axis = blockEntity.getGearAxis();

        if (axis == Direction.Axis.X) {
            stack.mulPose(Axis.XP.rotationDegrees(rotation));
        } else {
            stack.mulPose(Axis.ZP.rotationDegrees(rotation));
        }

        stack.translate(-0.5D, -0.5D, -0.5D);

        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                blockEntity.getBlockState().setValue(WaterWheelBlock.ACTIVE, false),
                stack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        stack.popPose();
    }
}
