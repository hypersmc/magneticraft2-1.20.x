package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalWaterPumpBlock;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalWaterPumpBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Renders the pump's one-sided input shaft and crank wheel.
 *
 * The shaft is authored at the exact 6x6 cross-section used by Wooden Shaft,
 * so it meets the Gear V2 network without the tiny scaled-pulley mismatch.
 */
public class MechanicalWaterPumpBlockEntityRenderer
        implements BlockEntityRenderer<MechanicalWaterPumpBlockEntity> {

    private static final ResourceLocation DRIVE_MODEL =
            new ResourceLocation(
                    "magneticraft2",
                    "block/mechanical_water_pump_drive"
            );

    public MechanicalWaterPumpBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            MechanicalWaterPumpBlockEntity pump,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        BlockState state = pump.getBlockState();

        if (!state.hasProperty(MechanicalWaterPumpBlock.FACING)) {
            return;
        }

        Direction inputSide =
                state.getValue(
                        MechanicalWaterPumpBlock.FACING
                );

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);

        orientPositiveYTo(
                inputSide,
                poseStack
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        pump.getVisualRotationDegrees(
                                partialTicks
                        )
                )
        );

        poseStack.translate(-0.5D, -0.5D, -0.5D);

        BakedModel model =
                Minecraft.getInstance()
                        .getModelManager()
                        .getModel(DRIVE_MODEL);

        if (model != null) {
            var consumer =
                    buffer.getBuffer(
                            RenderType.solid()
                    );

            List<BakedQuad> quads =
                    model.getQuads(
                            null,
                            null,
                            RandomSource.create(0L)
                    );

            for (BakedQuad quad : quads) {
                consumer.putBulkData(
                        poseStack.last(),
                        quad,
                        1.0F,
                        1.0F,
                        1.0F,
                        packedLight,
                        packedOverlay
                );
            }
        }

        poseStack.popPose();
    }

    private void orientPositiveYTo(
            Direction direction,
            PoseStack poseStack) {
        switch (direction) {
            case DOWN -> poseStack.mulPose(
                    Axis.XP.rotationDegrees(180.0F)
            );
            case NORTH -> poseStack.mulPose(
                    Axis.XP.rotationDegrees(-90.0F)
            );
            case SOUTH -> poseStack.mulPose(
                    Axis.XP.rotationDegrees(90.0F)
            );
            case EAST -> poseStack.mulPose(
                    Axis.ZP.rotationDegrees(-90.0F)
            );
            case WEST -> poseStack.mulPose(
                    Axis.ZP.rotationDegrees(90.0F)
            );
            default -> {
            }
        }
    }
}
