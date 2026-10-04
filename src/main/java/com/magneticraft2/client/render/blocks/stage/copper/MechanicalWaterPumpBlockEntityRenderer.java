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
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MechanicalWaterPumpBlockEntityRenderer
        implements BlockEntityRenderer<MechanicalWaterPumpBlockEntity> {

    private static final ResourceLocation PULLEY_MODEL =
            new ResourceLocation(
                    "magneticraft2",
                    "block/pulley_small_wood"
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

        Direction facing =
                state.getValue(
                        MechanicalWaterPumpBlock.FACING
                );

        Vec3 outward =
                Vec3.atLowerCornerOf(
                        facing.getNormal()
                ).scale(0.28D);

        poseStack.pushPose();
        poseStack.translate(
                0.5D + outward.x,
                0.5D + outward.y,
                0.5D + outward.z
        );

        Direction.Axis axis =
                facing.getAxis();

        if (axis == Direction.Axis.X) {
            poseStack.mulPose(
                    Axis.ZP.rotationDegrees(-90.0F)
            );
        } else if (axis == Direction.Axis.Z) {
            poseStack.mulPose(
                    Axis.XP.rotationDegrees(90.0F)
            );
        }

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        pump.getVisualRotationDegrees(
                                partialTicks
                        )
                )
        );

        poseStack.scale(
                0.46F,
                0.46F,
                0.46F
        );
        poseStack.translate(
                -0.5D,
                -0.5D,
                -0.5D
        );

        BakedModel model =
                Minecraft.getInstance()
                        .getModelManager()
                        .getModel(PULLEY_MODEL);

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
}
