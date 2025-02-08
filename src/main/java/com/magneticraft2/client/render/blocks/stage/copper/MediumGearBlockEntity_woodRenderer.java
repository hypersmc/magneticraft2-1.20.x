package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.MediumGearBlockEntity_wood;
import com.magneticraft2.common.systems.GEAR.GearNode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.magneticraft2.common.block.stage.copper.MediumGearBlock_wood.*;
import static net.minecraft.world.level.block.DirectionalBlock.FACING;

/**
 * @author JumpWatch on 15-01-2025
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MediumGearBlockEntity_woodRenderer implements BlockEntityRenderer<MediumGearBlockEntity_wood> {
    public static final Logger LOGGER = LogManager.getLogger("MGC2GearRenderer");
    public MediumGearBlockEntity_woodRenderer(BlockEntityRendererProvider.Context context) {}
    @Override
    public void render(MediumGearBlockEntity_wood pBlockEntity, float partialTicks, PoseStack stack, MultiBufferSource bufferSource, int pPackedLight, int pPackedOverlay) {
        GearNode gearNode = pBlockEntity.getGearNode();
        if (gearNode != null) {
            int direction = gearNode.getDirectionMultiplier();
            float clientSpeed = gearNode.getClientSpeed();
            // Calculate rotation based on speed and partialTicks
            // If speed is 0 or less, stop the rotation
            float rotationAngle = 0.0f;
            if (clientSpeed > 0) {
                rotationAngle = clientSpeed * 360; // Adjust this formula as needed
            }
//            LOGGER.info("gearnode data " + gearNode.toString());

            // Push the current transformation stack
            stack.pushPose();

            // Translate to the block's center for proper rotation
            stack.translate(0.5, 0.5, 0.5);

            // Apply rotation
            if (direction == 1) {
                if (pBlockEntity.getBlockState().getValue(VERTICAL_FACING_up) || pBlockEntity.getBlockState().getValue(VERTICAL_FACING_down)) {
                    stack.mulPose(Axis.YP.rotationDegrees(rotationAngle));
                }
                if (pBlockEntity.getBlockState().getValue(FACING) == Direction.EAST && !(pBlockEntity.getBlockState().getValue(VERTICAL_FACING_up) || pBlockEntity.getBlockState().getValue(VERTICAL_FACING_down))) {
                    stack.mulPose(Axis.XP.rotationDegrees(rotationAngle));
                }
                if (pBlockEntity.getBlockState().getValue(FACING) == Direction.WEST && !(pBlockEntity.getBlockState().getValue(VERTICAL_FACING_up) || pBlockEntity.getBlockState().getValue(VERTICAL_FACING_down))) {
                    stack.mulPose(Axis.XP.rotationDegrees(rotationAngle));
                }
                if (pBlockEntity.getBlockState().getValue(FACING) == Direction.SOUTH && !(pBlockEntity.getBlockState().getValue(VERTICAL_FACING_up) || pBlockEntity.getBlockState().getValue(VERTICAL_FACING_down))) {
                    stack.mulPose(Axis.ZP.rotationDegrees(rotationAngle));
                }
                if (pBlockEntity.getBlockState().getValue(FACING) == Direction.NORTH && !(pBlockEntity.getBlockState().getValue(VERTICAL_FACING_up) || pBlockEntity.getBlockState().getValue(VERTICAL_FACING_down))) {
                    stack.mulPose(Axis.ZN.rotationDegrees(rotationAngle));
                }
            }else {
                // Apply rotation
                if (pBlockEntity.getBlockState().getValue(VERTICAL_FACING_up) || pBlockEntity.getBlockState().getValue(VERTICAL_FACING_down)){
                    stack.mulPose(Axis.YP.rotationDegrees(-rotationAngle));
                }
                if (pBlockEntity.getBlockState().getValue(FACING) == Direction.EAST && !(pBlockEntity.getBlockState().getValue(VERTICAL_FACING_up) || pBlockEntity.getBlockState().getValue(VERTICAL_FACING_down))){
                    stack.mulPose(Axis.XP.rotationDegrees(-rotationAngle));
                }
                if (pBlockEntity.getBlockState().getValue(FACING) == Direction.WEST&& !(pBlockEntity.getBlockState().getValue(VERTICAL_FACING_up) || pBlockEntity.getBlockState().getValue(VERTICAL_FACING_down))){
                    stack.mulPose(Axis.XP.rotationDegrees(-rotationAngle));
                }
                if (pBlockEntity.getBlockState().getValue(FACING) == Direction.SOUTH&& !(pBlockEntity.getBlockState().getValue(VERTICAL_FACING_up) || pBlockEntity.getBlockState().getValue(VERTICAL_FACING_down))){
                    stack.mulPose(Axis.ZP.rotationDegrees(-rotationAngle));
                }
                if (pBlockEntity.getBlockState().getValue(FACING) == Direction.NORTH&& !(pBlockEntity.getBlockState().getValue(VERTICAL_FACING_up) || pBlockEntity.getBlockState().getValue(VERTICAL_FACING_down))){
                    stack.mulPose(Axis.ZN.rotationDegrees(-rotationAngle));
                }
            }



            // Translate back to original position
            stack.translate(-0.5, -0.5, -0.5);

            // Render the block's existing model
            if (pBlockEntity.getBlockState().getValue(POWERED)) {
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                        pBlockEntity.getBlockState().setValue(POWERED, false),
                        stack,
                        bufferSource,
                        pPackedLight,
                        pPackedOverlay
                );
            }

            // Pop the transformation stack
            stack.popPose();

        }
    }
}
