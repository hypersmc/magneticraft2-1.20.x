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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.world.level.block.DirectionalBlock.FACING;

/**
 * Dynamic renderer for the reusable Mechanical Input module.
 *
 * Formed modules are completely invisible to the normal chunk renderer. This BER
 * owns both the copper bearing housing and the wooden shaft so there can never be
 * a one-frame/static-model disagreement between FORMED/ROTATING states.
 */
public class MechanicalInputModuleBlockEntityRenderer
        implements BlockEntityRenderer<MechanicalInputModuleBlockEntity> {

    private static final ResourceLocation SHAFT_MODEL =
            new ResourceLocation("magneticraft2", "block/shaft_wood");
    private static final ResourceLocation HOUSING_MODEL =
            new ResourceLocation("magneticraft2", "block/mechanical_input_module_housing");

    private final Map<ResourceLocation, List<BakedQuad>> quadCache =
            new HashMap<>();

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

        // Unformed + stopped is still supplied by the ordinary baked block model.
        if (!formed && !rotating) {
            return;
        }

        Direction.Axis axis = state.getValue(FACING).getAxis();
        float rotation =
                blockEntity.getVisualRotationDegrees(partialTicks);

        // Housing is always visible whenever the BER owns this module.
        poseStack.pushPose();
        orientCanonicalYAxisTo(axis, poseStack);
        renderModel(
                HOUSING_MODEL,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();

        // Shaft uses the exact same Gear V2 angle as the connected network.
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        orientCanonicalYAxisTo(axis, poseStack);
        poseStack.mulPose(
                Axis.YP.rotationDegrees(rotation)
        );
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        renderModel(
                SHAFT_MODEL,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    /**
     * Both the housing and shaft resource models are authored on +Y.
     * Only the axis matters for their shape; Gear V2 handles rotation direction.
     */
    private void orientCanonicalYAxisTo(
            Direction.Axis axis,
            PoseStack poseStack) {
        poseStack.translate(0.5D, 0.5D, 0.5D);

        if (axis == Direction.Axis.X) {
            poseStack.mulPose(
                    Axis.ZP.rotationDegrees(-90.0F)
            );
        } else if (axis == Direction.Axis.Z) {
            poseStack.mulPose(
                    Axis.XP.rotationDegrees(90.0F)
            );
        }

        poseStack.translate(-0.5D, -0.5D, -0.5D);
    }

    private void renderModel(
            ResourceLocation modelLocation,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        List<BakedQuad> quads =
                quadCache.computeIfAbsent(
                        modelLocation,
                        location -> {
                            BakedModel model =
                                    Minecraft.getInstance()
                                            .getModelManager()
                                            .getModel(location);

                            if (model == null) {
                                return List.of();
                            }

                            return List.copyOf(
                                    model.getQuads(
                                            (BlockState) null,
                                            (Direction) null,
                                            RandomSource.create(0L)
                                    )
                            );
                        }
                );

        if (quads.isEmpty()) {
            return;
        }

        var consumer =
                buffer.getBuffer(RenderType.solid());

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
}
