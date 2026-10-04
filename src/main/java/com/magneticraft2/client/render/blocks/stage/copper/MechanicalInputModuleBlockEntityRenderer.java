package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalInputModuleBlock;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalInputModuleBlockEntity;
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

import static net.minecraft.world.level.block.DirectionalBlock.FACING;

public class MechanicalInputModuleBlockEntityRenderer
        implements BlockEntityRenderer<MechanicalInputModuleBlockEntity> {

    private static final ResourceLocation SHAFT_MODEL =
            new ResourceLocation("magneticraft2", "block/shaft_wood");

    public MechanicalInputModuleBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            MechanicalInputModuleBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        BlockState state = blockEntity.getBlockState();

        boolean formed =
                state.hasProperty(MechanicalInputModuleBlock.FORMED)
                        && state.getValue(MechanicalInputModuleBlock.FORMED);
        boolean rotating =
                state.hasProperty(MechanicalInputModuleBlock.ROTATING)
                        && state.getValue(MechanicalInputModuleBlock.ROTATING);

        // A formed input module always leaves the shaft to the BER. This avoids
        // falling back to the full static module model when the network stops,
        // which produced the oversized copper slab seen on the washer.
        if (!formed && !rotating) {
            return;
        }

        Direction.Axis axis = state.getValue(FACING).getAxis();
        float rotation =
                blockEntity.getVisualRotationDegrees(partialTicks);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);

        if (axis == Direction.Axis.X) {
            poseStack.mulPose(Axis.XP.rotationDegrees(rotation));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        } else if (axis == Direction.Axis.Z) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        }

        poseStack.translate(-0.5D, -0.5D, -0.5D);

        BakedModel model =
                Minecraft.getInstance()
                        .getModelManager()
                        .getModel(SHAFT_MODEL);

        if (model != null) {
            List<BakedQuad> quads =
                    model.getQuads(
                            (BlockState) null,
                            (Direction) null,
                            RandomSource.create(0L)
                    );
            var consumer =
                    buffer.getBuffer(RenderType.solid());

            for (BakedQuad quad : quads) {
                consumer.putBulkData(
                        poseStack.last(),
                        quad,
                        1.0F, 1.0F, 1.0F,
                        packedLight,
                        packedOverlay
                );
            }
        }

        poseStack.popPose();
    }
}
