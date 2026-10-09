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
 * replacement model selected in JSON. A compact eccentric crank on the
 * Mechanical Input Bearing drives the reciprocating inclined sieve directly.
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
    private static final ResourceLocation CRANK_ARM = new ResourceLocation("magneticraft2", "multiblock/mechanical_sifter_crank_arm");
    private static final ResourceLocation CONNECTING_ROD = new ResourceLocation("magneticraft2", "multiblock/mechanical_sifter_connecting_rod");
    private static final ResourceLocation FOLLOWER = new ResourceLocation("magneticraft2", "multiblock/mechanical_sifter_follower");
    private static final float SCREEN_TILT_DEGREES =
            10.0F;

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

        float driveAngle = sifter.getDriveRotationDegrees(partialTicks);
        renderCrankLinkage(driveAngle, shake, poseStack, buffer, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(
                0.0D,
                0.0D,
                shake
        );

        // One coherent inclined sieve: rear/high under the hopper, front/low
        // toward the coarse discharge. Rotating the complete tray avoids the
        // staircase look the previous JSON-only slope had.
        poseStack.translate(
                0.50D,
                0.78D,
                1.20D
        );
        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        SCREEN_TILT_DEGREES
                )
        );
        poseStack.translate(
                -0.50D,
                -0.78D,
                -1.20D
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
                partialTicks,
                shake,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        renderStoredItem(
                sifter,
                sifter.getOutputStack(),
                -0.50D,
                0.72D,
                1.56D,
                0.42F,
                402,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        renderStoredItem(
                sifter,
                sifter.getByproductStack(),
                1.50D,
                0.72D,
                1.56D,
                0.38F,
                403,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    /**
     * An eccentric crank drives a rigid, pivoting connecting rod. The follower
     * shares the sieve's existing stroke and remains attached to its carriage.
     * All dimensions are in local SOUTH-facing multiblock coordinates.
     */
    private void renderCrankLinkage(float degrees, double shake,
            PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        // SOUTH-local coordinates: input bearing block is at (1, 0, 0),
        // and its X-oriented shaft centre is exactly (1.5, 0.5, 0.5).
        // The crank sits outside the bearing block on its machine-facing side
        // at X=0.925; its short hub meets the shaft at the X=1 block boundary.
        // Only the compact inboard crank moves; the real input-bearing block
        // remains the stationary support for the player's drive network.
        final double centerX = 0.925D;
        final double centerY = 0.50D;
        final double centerZ = 0.50D;
        final double radius = 0.12D;
        final double length = 7.0D / 16.0D;
        double theta = Math.toRadians(degrees);
        double pinY = centerY + radius * Math.cos(theta);
        double pinZ = centerZ + radius * Math.sin(theta);

        // Both the crank pin and the solid wooden rod use the same point.
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, centerZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(degrees));
        renderModel(CRANK_ARM, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();

        // Positive X rotation takes the rod's +Z axis toward -Y.
        // Hence the positive arcsine here, not an independently animated tilt.
        double rodAngle = Math.asin((pinY - centerY) / length);
        poseStack.pushPose();
        poseStack.translate(centerX, pinY, pinZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(
                (float) Math.toDegrees(rodAngle)));
        renderModel(CONNECTING_ROD, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();

        // The follower starts at the rod's far pin and has a compact offset
        // carriage bracket that meets the sieve's near side rail. It moves
        // with the same shake as the upper tray, never with the rotating crank.
        // Keeping this linkage at the near edge avoids the stationary rear
        // support and the output guides.
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, centerZ + length + shake);
        renderModel(FOLLOWER, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderInputItems(
            MechanicalSifterBlockEntity sifter,
            float partialTicks,
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
         * Waiting material lives in the feed hopper, not on the moving sieve.
         * That makes the feed path visually stable and leaves only the actual
         * workpiece moving through the classifier.
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
                {0.000D, 0.000D, 0.000D},
                {-0.115D, 0.025D, 0.035D},
                {0.115D, 0.045D, -0.025D}
        };

        for (int i = 0;
             i < visibleQueued;
             i++) {
            renderStoredItem(
                    sifter,
                    stack,
                    0.50D + offsets[i][0],
                    1.73D + offsets[i][1],
                    0.50D + offsets[i][2],
                    0.43F,
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

        double progress =
                Math.max(
                        0.0D,
                        Math.min(
                                1.0D,
                                sifter.getVisualProcessProgress(
                                        partialTicks
                                )
                        )
                );

        double x;
        double y;
        double z;

        /*
         * Stage 1: fall from the hopper throat onto the upper end of the screen.
         * Stage 2: travel down the single shaking screen bed.
         * Stage 3: drop into the fines catch pan/outlet.
         */
        if (progress < 0.18D) {
            double t =
                    smoothStep(
                            progress / 0.18D
                    );

            x = 0.50D;
            y = lerp(
                    1.68D,
                    0.91D,
                    t
            );
            z = lerp(
                    0.62D,
                    0.88D,
                    t
            );
        } else if (progress < 0.82D) {
            double t =
                    smoothStep(
                            (progress - 0.18D)
                                    / 0.64D
                    );

            x = 0.50D
                    + shake * 0.12D;
            y = lerp(
                    0.87D,
                    0.69D,
                    t
            );
            z = lerp(
                    0.89D,
                    1.48D,
                    t
            ) + shake;
        } else {
            double t =
                    smoothStep(
                            (progress - 0.82D)
                                    / 0.18D
                    );

            x = lerp(
                    0.50D,
                    0.98D,
                    t
            );
            y = lerp(
                    0.69D,
                    0.43D,
                    t
            );
            z = lerp(
                    1.48D,
                    1.78D,
                    t
            ) + shake * 0.35D;
        }

        renderStoredItem(
                sifter,
                stack,
                x,
                y,
                z,
                0.46F,
                499,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
    }

    private static double smoothStep(
            double value) {
        double clamped =
                Math.max(
                        0.0D,
                        Math.min(
                                1.0D,
                                value
                        )
                );

        return clamped
                * clamped
                * (3.0D - 2.0D * clamped);
    }

    private static double lerp(
            double from,
            double to,
            double progress) {
        return from
                + (to - from)
                * progress;
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
