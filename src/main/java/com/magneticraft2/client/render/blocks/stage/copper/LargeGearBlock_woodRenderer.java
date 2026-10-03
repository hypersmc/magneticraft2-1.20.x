package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.LargeGearBlockEntity_wood;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

import static com.magneticraft2.common.block.stage.copper.LargeGearBlock_wood.POWERED;
import static com.magneticraft2.common.block.stage.copper.LargeGearBlock_wood.VERTICAL_FACING_down;
import static com.magneticraft2.common.block.stage.copper.LargeGearBlock_wood.VERTICAL_FACING_up;
import static net.minecraft.world.level.block.DirectionalBlock.FACING;

/**
 * @author JumpWatch on 15-01-2025
 * @Project mgc2-1.20
 * @version 1.0.0
 */
public class LargeGearBlock_woodRenderer implements BlockEntityRenderer<LargeGearBlockEntity_wood> {
    public LargeGearBlock_woodRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(LargeGearBlockEntity_wood blockEntity, float partialTicks, PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!blockEntity.getBlockState().getValue(POWERED)) {
            return;
        }

        stack.pushPose();
        stack.translate(0.5, 0.5, 0.5);
        applyGearRotation(blockEntity, partialTicks, stack);
        stack.translate(-0.5, -0.5, -0.5);

        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                blockEntity.getBlockState().setValue(POWERED, false),
                stack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        stack.popPose();
    }

    private void applyGearRotation(LargeGearBlockEntity_wood blockEntity, float partialTicks, PoseStack stack) {
        float rotationAngle = blockEntity.getVisualRotationDegrees(partialTicks);
        boolean vertical = blockEntity.getBlockState().getValue(VERTICAL_FACING_up) || blockEntity.getBlockState().getValue(VERTICAL_FACING_down);

        if (vertical) {
            stack.mulPose(Axis.YP.rotationDegrees(rotationAngle));
            return;
        }

        Direction facing = blockEntity.getBlockState().getValue(FACING);
        if (facing == Direction.EAST) {
            stack.mulPose(Axis.XP.rotationDegrees(rotationAngle));
        } else if (facing == Direction.WEST) {
            stack.mulPose(Axis.XN.rotationDegrees(rotationAngle));
        } else if (facing == Direction.SOUTH) {
            stack.mulPose(Axis.ZP.rotationDegrees(rotationAngle));
        } else {
            stack.mulPose(Axis.ZN.rotationDegrees(rotationAngle));
        }
    }
}
