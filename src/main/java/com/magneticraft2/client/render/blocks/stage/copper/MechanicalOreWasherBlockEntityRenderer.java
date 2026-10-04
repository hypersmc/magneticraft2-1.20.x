package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalOreWasherBlock;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalOreWasherBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Formed Ore Washer renderer.
 *
 * Static geometry is the exact replacement model selected by the multiblock JSON,
 * matching the Blueprint Maker / Primitive Grinder path. Only genuinely moving
 * pieces are separate baked models.
 */
public class MechanicalOreWasherBlockEntityRenderer
        implements BlockEntityRenderer<MechanicalOreWasherBlockEntity> {

    private static final ResourceLocation ROTOR_MODEL =
            new ResourceLocation(
                    "magneticraft2",
                    "multiblock/mechanical_ore_washer_rotor"
            );
    private static final ResourceLocation INPUT_PULLEY_MODEL =
            new ResourceLocation(
                    "magneticraft2",
                    "multiblock/mechanical_ore_washer_input_pulley"
            );

    // Open-belt drive ratio: small input pulley -> larger trommel pulley.
    private static final float DRUM_SPEED_RATIO =
            2.25F / 3.65F;
    private static final ResourceLocation WATER_MODEL =
            new ResourceLocation(
                    "magneticraft2",
                    "multiblock/mechanical_ore_washer_water"
            );

    private final Map<ResourceLocation, List<BakedQuad>> quadCache =
            new HashMap<>();

    public MechanicalOreWasherBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public void render(
            MechanicalOreWasherBlockEntity washer,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {

        BlockState formedState =
                washer.getBlockState();

        if (!formedState.hasProperty(MechanicalOreWasherBlock.IS_FORMED)
                || !formedState.getValue(MechanicalOreWasherBlock.IS_FORMED)) {
            return;
        }

        Direction facing =
                formedState.getValue(MechanicalOreWasherBlock.FACING);

        poseStack.pushPose();
        applySouthFacingTransform(
                poseStack,
                facing
        );

        float inputRotation =
                washer.getMechanicalVisualRotationDegrees(
                        partialTicks
                );

        // The visible Gear V2 bearing drives a small upper pulley. Two static
        // leather belt spans connect it to the larger pulley on the lower drum.
        poseStack.pushPose();
        poseStack.translate(
                0.5D,
                1.5D,
                0.5D
        );
        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        inputRotation
                )
        );
        poseStack.translate(
                -0.5D,
                -1.5D,
                -0.5D
        );

        renderModel(
                INPUT_PULLEY_MODEL,
                RenderType.solid(),
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();

        // Open belts keep both pulleys turning in the same direction. The
        // larger driven pulley slows the trommel so the visual gearing is
        // understandable instead of looking like a shaft floating in mid-air.
        float drumRotation =
                inputRotation * DRUM_SPEED_RATIO;

        poseStack.pushPose();
        poseStack.translate(
                0.5D,
                0.90625D,
                0.5D
        );
        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        drumRotation
                )
        );
        poseStack.translate(
                -0.5D,
                -0.90625D,
                -0.5D
        );

        renderModel(
                ROTOR_MODEL,
                RenderType.solid(),
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();

        if (washer.hasWaterSupply()) {
            renderModel(
                    WATER_MODEL,
                    RenderType.translucent(),
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay
            );
        }

        renderStoredItem(
                washer,
                washer.getInputStack(),
                0.5D,
                0.82D,
                -0.62D,
                0.32F,
                301,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        renderStoredItem(
                washer,
                washer.getOutputStack(),
                0.34D,
                0.46D,
                1.50D,
                0.27F,
                302,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        renderStoredItem(
                washer,
                washer.getByproductStack(),
                0.66D,
                0.46D,
                1.50D,
                0.25F,
                303,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    /**
     * Canonical formed models face SOUTH. Rotate around the controller block
     * centre so moving models/items remain aligned with the matching JSON
     * replacement model for the other three facings.
     */
    private void applySouthFacingTransform(
            PoseStack poseStack,
            Direction facing) {
        float yaw =
                switch (facing) {
                    case EAST -> 90.0F;
                    case NORTH -> 180.0F;
                    case WEST -> -90.0F;
                    default -> 0.0F;
                };

        if (yaw == 0.0F) {
            return;
        }

        poseStack.translate(
                0.5D,
                0.0D,
                0.5D
        );
        poseStack.mulPose(
                Axis.YP.rotationDegrees(yaw)
        );
        poseStack.translate(
                -0.5D,
                0.0D,
                -0.5D
        );
    }

    private void renderModel(
            ResourceLocation modelLocation,
            RenderType renderType,
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
                buffer.getBuffer(renderType);

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

    private void renderStoredItem(
            MechanicalOreWasherBlockEntity washer,
            ItemStack stack,
            double x,
            double y,
            double z,
            float scale,
            int seed,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {

        if (stack.isEmpty()) {
            return;
        }

        ItemStack display =
                stack.copy();
        display.setCount(1);

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        90.0F
                )
        );
        poseStack.scale(
                scale,
                scale,
                scale
        );

        Minecraft.getInstance()
                .getItemRenderer()
                .renderStatic(
                        display,
                        ItemDisplayContext.NONE,
                        packedLight,
                        packedOverlay,
                        poseStack,
                        buffer,
                        washer.getLevel(),
                        seed
                );

        poseStack.popPose();
    }
}
