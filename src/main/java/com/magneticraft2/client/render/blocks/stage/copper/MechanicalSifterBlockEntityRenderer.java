package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.block.stage.copper.MechanicalSifterBlock;
import com.magneticraft2.common.blockentity.stage.copper.MechanicalSifterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
 * replacement model selected in JSON. The two screens and the reciprocating
 * crank linkage render separately so the formed machine explains how the
 * rotational crank becomes a back-and-forth classification motion.
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
    private static final ResourceLocation LINKAGE =
            new ResourceLocation(
                    "magneticraft2",
                    "multiblock/mechanical_sifter_linkage"
            );
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

        renderDriveWheel(
                sifter.getDriveRotationDegrees(
                        partialTicks
                ),
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        // The connecting rod follows the screen carriage so the side drive
        // visibly explains the reciprocating sieve motion.
        poseStack.pushPose();
        poseStack.translate(
                0.0D,
                0.0D,
                shake
        );
        renderModel(
                LINKAGE,
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

    private void renderDriveWheel(
            float rotation,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        poseStack.pushPose();

        /*
         * Large external side flywheel. The previous small ring read as a
         * random gear/lump; this deliberately follows the reference machine:
         * one obvious wooden wheel outside the right frame, a visible axle,
         * spokes, copper hub and an eccentric crank pin for the linkage.
         */
        poseStack.translate(
                2.02D,
                0.98D,
                1.16D
        );

        // Local wheel geometry is in X/Y, so rotate it onto the machine's
        // right side (Y/Z plane, axle along world X).
        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        90.0F
                )
        );
        poseStack.mulPose(
                Axis.ZP.rotationDegrees(
                        rotation
                )
        );

        TextureAtlasSprite wood =
                Minecraft.getInstance()
                        .getTextureAtlas(
                                TextureAtlas.LOCATION_BLOCKS
                        )
                        .apply(
                                new ResourceLocation(
                                        "minecraft",
                                        "block/oak_planks"
                                )
                        );

        TextureAtlasSprite darkWood =
                Minecraft.getInstance()
                        .getTextureAtlas(
                                TextureAtlas.LOCATION_BLOCKS
                        )
                        .apply(
                                new ResourceLocation(
                                        "minecraft",
                                        "block/stripped_spruce_log"
                                )
                        );

        TextureAtlasSprite copper =
                Minecraft.getInstance()
                        .getTextureAtlas(
                                TextureAtlas.LOCATION_BLOCKS
                        )
                        .apply(
                                new ResourceLocation(
                                        "minecraft",
                                        "block/cut_copper"
                                )
                        );

        VertexConsumer consumer =
                buffer.getBuffer(
                        RenderType.solid()
                );

        // Axle through the side bearing.
        renderWheelBox(
                poseStack, consumer, packedLight, darkWood,
                -0.11D, -0.11D, -0.42D,
                 0.11D,  0.11D,  0.42D
        );

        // Large octagonal wooden rim.
        renderWheelBox(
                poseStack, consumer, packedLight, wood,
                -0.38D,  0.42D, -0.12D,
                 0.38D,  0.56D,  0.12D
        );
        renderWheelBox(
                poseStack, consumer, packedLight, wood,
                -0.38D, -0.56D, -0.12D,
                 0.38D, -0.42D,  0.12D
        );
        renderWheelBox(
                poseStack, consumer, packedLight, wood,
                -0.56D, -0.38D, -0.12D,
                -0.42D,  0.38D,  0.12D
        );
        renderWheelBox(
                poseStack, consumer, packedLight, wood,
                 0.42D, -0.38D, -0.12D,
                 0.56D,  0.38D,  0.12D
        );

        double[][] rimCorners = {
                {-0.405D,  0.405D},
                { 0.405D,  0.405D},
                {-0.405D, -0.405D},
                { 0.405D, -0.405D}
        };

        for (double[] corner : rimCorners) {
            renderWheelBox(
                    poseStack,
                    consumer,
                    packedLight,
                    wood,
                    corner[0] - 0.11D,
                    corner[1] - 0.11D,
                    -0.12D,
                    corner[0] + 0.11D,
                    corner[1] + 0.11D,
                    0.12D
            );
        }

        // Four broad wooden spokes.
        renderWheelBox(
                poseStack, consumer, packedLight, darkWood,
                -0.43D, -0.055D, -0.085D,
                 0.43D,  0.055D,  0.085D
        );
        renderWheelBox(
                poseStack, consumer, packedLight, darkWood,
                -0.055D, -0.43D, -0.085D,
                 0.055D,  0.43D,  0.085D
        );

        // Copper hub.
        renderWheelBox(
                poseStack, consumer, packedLight, copper,
                -0.14D, -0.14D, -0.17D,
                 0.14D,  0.14D,  0.17D
        );

        // Copper rim straps make it read as an early-industrial flywheel,
        // not a gear with teeth.
        renderWheelBox(
                poseStack, consumer, packedLight, copper,
                -0.16D,  0.50D, -0.14D,
                 0.16D,  0.58D,  0.14D
        );
        renderWheelBox(
                poseStack, consumer, packedLight, copper,
                -0.16D, -0.58D, -0.14D,
                 0.16D, -0.50D,  0.14D
        );
        renderWheelBox(
                poseStack, consumer, packedLight, copper,
                -0.58D, -0.16D, -0.14D,
                -0.50D,  0.16D,  0.14D
        );
        renderWheelBox(
                poseStack, consumer, packedLight, copper,
                 0.50D, -0.16D, -0.14D,
                 0.58D,  0.16D,  0.14D
        );

        // Eccentric crank pin: visible reason for the connecting rod.
        renderWheelBox(
                poseStack, consumer, packedLight, copper,
                 0.24D, -0.08D, -0.22D,
                 0.36D,  0.08D,  0.22D
        );

        poseStack.popPose();
    }

    private void renderWheelBox(
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            TextureAtlasSprite sprite,
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1) {
        PoseStack.Pose pose =
                poseStack.last();

        float uvX =
                (float) Math.min(
                        1.0D,
                        Math.max(
                                1.0D / 16.0D,
                                Math.abs(x1 - x0)
                        )
                );
        float uvY =
                (float) Math.min(
                        1.0D,
                        Math.max(
                                1.0D / 16.0D,
                                Math.abs(y1 - y0)
                        )
                );
        float uvZ =
                (float) Math.min(
                        1.0D,
                        Math.max(
                                1.0D / 16.0D,
                                Math.abs(z1 - z0)
                        )
                );

        // +Y / -Y
        wheelQuad(
                consumer, pose, sprite, packedLight,
                x0, y1, z0, x1, y1, z0,
                x1, y1, z1, x0, y1, z1,
                0.0F, 1.0F, 0.0F, uvX, uvZ
        );
        wheelQuad(
                consumer, pose, sprite, packedLight,
                x0, y0, z1, x1, y0, z1,
                x1, y0, z0, x0, y0, z0,
                0.0F, -1.0F, 0.0F, uvX, uvZ
        );

        // +Z / -Z
        wheelQuad(
                consumer, pose, sprite, packedLight,
                x0, y0, z1, x1, y0, z1,
                x1, y1, z1, x0, y1, z1,
                0.0F, 0.0F, 1.0F, uvX, uvY
        );
        wheelQuad(
                consumer, pose, sprite, packedLight,
                x1, y0, z0, x0, y0, z0,
                x0, y1, z0, x1, y1, z0,
                0.0F, 0.0F, -1.0F, uvX, uvY
        );

        // +X / -X
        wheelQuad(
                consumer, pose, sprite, packedLight,
                x1, y0, z1, x1, y0, z0,
                x1, y1, z0, x1, y1, z1,
                1.0F, 0.0F, 0.0F, uvZ, uvY
        );
        wheelQuad(
                consumer, pose, sprite, packedLight,
                x0, y0, z0, x0, y0, z1,
                x0, y1, z1, x0, y1, z0,
                -1.0F, 0.0F, 0.0F, uvZ, uvY
        );
    }

    private void wheelQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            int packedLight,
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            float normalX,
            float normalY,
            float normalZ,
            float uScale,
            float vScale) {
        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float u1 =
                u0
                        + (sprite.getU1() - u0)
                        * uScale;
        float v1 =
                v0
                        + (sprite.getV1() - v0)
                        * vScale;

        wheelVertex(
                consumer, pose, packedLight,
                x0, y0, z0, u0, v0,
                normalX, normalY, normalZ
        );
        wheelVertex(
                consumer, pose, packedLight,
                x1, y1, z1, u1, v0,
                normalX, normalY, normalZ
        );
        wheelVertex(
                consumer, pose, packedLight,
                x2, y2, z2, u1, v1,
                normalX, normalY, normalZ
        );
        wheelVertex(
                consumer, pose, packedLight,
                x3, y3, z3, u0, v1,
                normalX, normalY, normalZ
        );
    }

    private void wheelVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            double x,
            double y,
            double z,
            float u,
            float v,
            float normalX,
            float normalY,
            float normalZ) {
        consumer.vertex(
                        pose.pose(),
                        (float) x,
                        (float) y,
                        (float) z
                )
                .color(
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F
                )
                .uv(u, v)
                .overlayCoords(
                        OverlayTexture.NO_OVERLAY
                )
                .uv2(packedLight)
                .normal(
                        pose.normal(),
                        normalX,
                        normalY,
                        normalZ
                )
                .endVertex();
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
