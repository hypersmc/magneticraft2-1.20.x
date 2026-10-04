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
    private static final ResourceLocation INPUT_GEAR_MODEL =
            new ResourceLocation(
                    "magneticraft2",
                    "multiblock/mechanical_ore_washer_input_gear"
            );
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

        // The external shaft now visibly drives a small wooden gear on its own
        // axis. That gear meshes with the lower drum gear instead of making the
        // trommel appear to receive rotation from empty air.
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
                INPUT_GEAR_MODEL,
                RenderType.solid(),
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();

        // Drum axis is deliberately lower than the input shaft. Meshed gears
        // reverse direction, so the trommel uses the opposite visual rotation.
        poseStack.pushPose();
        poseStack.translate(
                0.5D,
                1.21875D,
                0.5D
        );
        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        -inputRotation
                )
        );
        poseStack.translate(
                -0.5D,
                -1.21875D,
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
                0.78D,
                -0.5D,
                0.34F,
                301,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        renderStoredItem(
                washer,
                washer.getOutputStack(),
                0.32D,
                0.56D,
                1.5D,
                0.28F,
                302,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        renderStoredItem(
                washer,
                washer.getByproductStack(),
                0.68D,
                0.56D,
                1.5D,
                0.26F,
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
