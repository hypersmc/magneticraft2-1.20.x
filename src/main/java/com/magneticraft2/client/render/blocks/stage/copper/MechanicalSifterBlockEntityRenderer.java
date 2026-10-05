package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalSifterBlock;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalSifterBlockEntity;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Formed Mechanical Sifter renderer.
 *
 * Like the other multiblocks, the complete static frame comes from the
 * replacement model selected in JSON. Only the two moving screen assemblies are
 * rendered separately.
 */
public class MechanicalSifterBlockEntityRenderer
        implements BlockEntityRenderer<MechanicalSifterBlockEntity> {

    private static final ResourceLocation UPPER_TRAY =
            new ResourceLocation(
                    "magneticraft2",
                    "multiblock/mechanical_sifter_upper_tray"
            );
    private static final ResourceLocation LOWER_TRAY =
            new ResourceLocation(
                    "magneticraft2",
                    "multiblock/mechanical_sifter_lower_tray"
            );

    private final Map<ResourceLocation, List<BakedQuad>> quadCache =
            new HashMap<>();

    public MechanicalSifterBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public void render(
            MechanicalSifterBlockEntity sifter,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {

        BlockState formedState =
                sifter.getBlockState();

        if (!formedState.hasProperty(MechanicalSifterBlock.IS_FORMED)
                || !formedState.getValue(MechanicalSifterBlock.IS_FORMED)) {
            return;
        }

        Direction facing =
                formedState.getValue(MechanicalSifterBlock.FACING);

        poseStack.pushPose();
        applySouthFacingTransform(
                poseStack,
                facing
        );

        double shake =
                sifter.getShakeOffset(
                        partialTicks
                );

        poseStack.pushPose();
        poseStack.translate(
                0.0D,
                0.0D,
                -shake * 0.65D
        );
        renderModel(
                UPPER_TRAY,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(
                0.0D,
                0.0D,
                shake
        );
        renderModel(
                LOWER_TRAY,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();

        renderInputItems(
                sifter,
                shake,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        renderStoredItem(
                sifter,
                sifter.getOutputStack(),
                0.08D,
                0.36D,
                1.74D,
                0.36F,
                402,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        renderStoredItem(
                sifter,
                sifter.getByproductStack(),
                0.92D,
                0.36D,
                1.74D,
                0.34F,
                403,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    private void renderInputItems(
            MechanicalSifterBlockEntity sifter,
            double shake,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        ItemStack stack =
                sifter.getInputStack();

        if (stack.isEmpty()) {
            return;
        }

        /*
         * Keep queued feed readable on the upper screen. During processing one
         * workpiece is treated as the material currently being classified;
         * the rest stay as a stable pile instead of collapsing into one small
         * sprite.
         */
        int queuedCount =
                sifter.isProcessing()
                        ? Math.max(
                                0,
                                stack.getCount() - 1
                        )
                        : stack.getCount();

        int visibleQueued =
                Math.min(
                        queuedCount,
                        3
                );

        double[][] offsets = {
                {-0.16D, 0.000D, -0.020D},
                {0.16D, 0.018D, 0.015D},
                {0.00D, 0.036D, 0.070D}
        };

        for (int i = 0;
             i < visibleQueued;
             i++) {
            renderStoredItem(
                    sifter,
                    stack,
                    0.5D + offsets[i][0],
                    1.59D + offsets[i][1],
                    0.43D
                            + offsets[i][2]
                            - shake * 0.65D,
                    0.42F,
                    410 + i,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay
            );
        }

        if (!sifter.isProcessing()) {
            return;
        }

        /*
         * The active piece rides the moving upper sieve. A small sideways
         * component makes the classification action readable without inventing
         * random positions or allowing the queued pile to wander.
         */
        renderStoredItem(
                sifter,
                stack,
                0.5D + shake * 0.34D,
                1.61D,
                0.53D - shake * 0.65D,
                0.45F,
                499,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
    }

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
                buffer.getBuffer(
                        RenderType.solid()
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

    private void renderStoredItem(
            MechanicalSifterBlockEntity sifter,
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
                        sifter.getLevel(),
                        seed
                );

        poseStack.popPose();
    }
}
