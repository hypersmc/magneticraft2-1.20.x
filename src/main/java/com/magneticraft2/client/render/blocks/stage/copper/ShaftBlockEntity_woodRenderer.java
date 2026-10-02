package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.ShaftBlockEntity_wood;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

import static com.magneticraft2.common.block.stage.copper.ShaftBlock_wood.ROTATING;
import static net.minecraft.world.level.block.DirectionalBlock.FACING;

public class ShaftBlockEntity_woodRenderer implements BlockEntityRenderer<ShaftBlockEntity_wood> {
    public ShaftBlockEntity_woodRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ShaftBlockEntity_wood blockEntity,
                       float partialTicks,
                       PoseStack stack,
                       MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {
        if (!blockEntity.getBlockState().getValue(ROTATING)) {
            return;
        }

        stack.pushPose();
        stack.translate(0.5D, 0.5D, 0.5D);

        float rotationAngle = blockEntity.getVisualRotationDegrees(partialTicks);
        Direction.Axis axis = blockEntity.getBlockState().getValue(FACING).getAxis();

        if (axis == Direction.Axis.X) {
            stack.mulPose(Axis.XP.rotationDegrees(rotationAngle));
        } else if (axis == Direction.Axis.Z) {
            stack.mulPose(Axis.ZP.rotationDegrees(rotationAngle));
        } else {
            stack.mulPose(Axis.YP.rotationDegrees(rotationAngle));
        }

        stack.translate(-0.5D, -0.5D, -0.5D);

        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                blockEntity.getBlockState().setValue(ROTATING, false),
                stack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        stack.popPose();
    }
}
