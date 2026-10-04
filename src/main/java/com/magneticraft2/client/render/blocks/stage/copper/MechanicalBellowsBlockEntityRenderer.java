package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.MechanicalBellowsBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * The static JSON model owns the base, front plate and nozzle. This renderer
 * only draws the moving back plate, push rod and leather accordion.
 */
public class MechanicalBellowsBlockEntityRenderer
        implements BlockEntityRenderer<MechanicalBellowsBlockEntity> {

    private static final ResourceLocation WOOD_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/oak_planks.png"
            );

    private static final ResourceLocation LEATHER_TEXTURE =
            new ResourceLocation(
                    "magneticraft2",
                    "textures/block/leather_belt.png"
            );

    private static final ResourceLocation COPPER_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/copper_block.png"
            );

    public MechanicalBellowsBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            MechanicalBellowsBlockEntity bellows,
            float partialTicks,
            PoseStack stack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        float compression =
                clamp01(
                        bellows.getCompressionProgress(
                                partialTicks
                        )
                );

        stack.pushPose();
        stack.translate(0.5D, 0.5D, 0.5D);
        orientLocalZToDirection(
                stack,
                bellows.getFacing()
        );

        double backZ =
                -0.34D
                        + compression * 0.29D;

        VertexConsumer woodConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                WOOD_TEXTURE
                        )
                );

        stack.pushPose();
        stack.translate(
                0.0D,
                0.0D,
                backZ
        );
        drawBox(
                stack,
                woodConsumer,
                packedLight,
                0.70D,
                0.70D,
                0.085D
        );
        stack.popPose();

        VertexConsumer leatherConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                LEATHER_TEXTURE
                        )
                );

        double frontFoldZ = 0.09D;

        renderAccordion(
                stack,
                leatherConsumer,
                packedLight,
                backZ + 0.045D,
                frontFoldZ
        );

        // Visible push rod from the crank side into the moving back plate.
        double rodBack = -0.50D;
        double rodFront = backZ - 0.04D;
        double rodLength =
                Math.max(
                        0.02D,
                        rodFront - rodBack
                );

        VertexConsumer copperConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                COPPER_TEXTURE
                        )
                );

        stack.pushPose();
        stack.translate(
                0.0D,
                0.0D,
                rodBack
                        + rodLength * 0.5D
        );
        drawBox(
                stack,
                copperConsumer,
                packedLight,
                0.10D,
                0.10D,
                rodLength
        );
        stack.popPose();

        stack.pushPose();
        stack.translate(
                0.0D,
                0.0D,
                backZ - 0.01D
        );
        drawBox(
                stack,
                copperConsumer,
                packedLight,
                0.18D,
                0.18D,
                0.09D
        );
        stack.popPose();

        stack.popPose();
    }

    private void renderAccordion(
            PoseStack stack,
            VertexConsumer consumer,
            int packedLight,
            double backZ,
            double frontZ) {

        // Real bellows are a hollow concertina, not a stack of solid leather
        // plates. Alternate wide/narrow square folds and connect each fold with
        // sloped leather panels so the whole envelope expands and collapses as
        // one continuous piece.
        final int nodes = 7;
        double[] z = new double[nodes];
        double[] radius = new double[nodes];

        for (int i = 0; i < nodes; i++) {
            double t = i / (double) (nodes - 1);
            z[i] = backZ + (frontZ - backZ) * t;

            if (i == 0 || i == nodes - 1) {
                radius[i] = 0.33D;
            } else {
                radius[i] =
                        i % 2 == 0
                                ? 0.31D
                                : 0.255D;
            }
        }

        for (int i = 0; i < nodes - 1; i++) {
            renderAccordionSegment(
                    stack,
                    consumer,
                    packedLight,
                    z[i],
                    radius[i],
                    z[i + 1],
                    radius[i + 1]
            );
        }

        // Dark seams at every fold make the concertina readable even while it
        // is nearly fully compressed.
        for (int i = 1; i < nodes - 1; i++) {
            double r = radius[i];

            for (double sign : new double[]{-1.0D, 1.0D}) {
                stack.pushPose();
                stack.translate(
                        0.0D,
                        sign * r,
                        z[i]
                );
                drawBox(
                        stack,
                        consumer,
                        packedLight,
                        r * 2.0D,
                        0.045D,
                        0.035D
                );
                stack.popPose();

                stack.pushPose();
                stack.translate(
                        sign * r,
                        0.0D,
                        z[i]
                );
                drawBox(
                        stack,
                        consumer,
                        packedLight,
                        0.045D,
                        r * 2.0D,
                        0.035D
                );
                stack.popPose();
            }
        }
    }

    private void renderAccordionSegment(
            PoseStack stack,
            VertexConsumer consumer,
            int packedLight,
            double z0,
            double r0,
            double z1,
            double r1) {

        double dz = z1 - z0;
        // Use the larger fold radius for the panel span. Using the smaller
        // radius left the four sloped sheets short at every wide/narrow
        // transition, creating visible holes straight through the bellows.
        // A tiny overlap also hides floating-point/raster seams at the corners.
        double panelWidth =
                Math.max(r0, r1) * 2.0D
                        + 0.04D;

        // Top and bottom leather sheets.
        for (double sign : new double[]{-1.0D, 1.0D}) {
            double y0 = sign * r0;
            double y1 = sign * r1;
            double dy = y1 - y0;
            double length =
                    Math.sqrt(
                            dy * dy + dz * dz
                    );
            float angle =
                    (float) Math.toDegrees(
                            Math.atan2(
                                    -dy,
                                    dz
                            )
                    );

            stack.pushPose();
            stack.translate(
                    0.0D,
                    (y0 + y1) * 0.5D,
                    (z0 + z1) * 0.5D
            );
            stack.mulPose(
                    Axis.XP.rotationDegrees(
                            angle
                    )
            );
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    panelWidth,
                    0.045D,
                    length + 0.035D
            );
            stack.popPose();
        }

        // Left and right sheets.
        for (double sign : new double[]{-1.0D, 1.0D}) {
            double x0 = sign * r0;
            double x1 = sign * r1;
            double dx = x1 - x0;
            double length =
                    Math.sqrt(
                            dx * dx + dz * dz
                    );
            float angle =
                    (float) Math.toDegrees(
                            Math.atan2(
                                    dx,
                                    dz
                            )
                    );

            stack.pushPose();
            stack.translate(
                    (x0 + x1) * 0.5D,
                    0.0D,
                    (z0 + z1) * 0.5D
            );
            stack.mulPose(
                    Axis.YP.rotationDegrees(
                            angle
                    )
            );
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    0.045D,
                    panelWidth,
                    length + 0.035D
            );
            stack.popPose();
        }
    }

    private float clamp01(float value) {
        return Math.max(
                0.0F,
                Math.min(
                        1.0F,
                        value
                )
        );
    }

    /**
     * Authored local +Z is the nozzle/output direction.
     */
    private void orientLocalZToDirection(
            PoseStack stack,
            Direction direction) {
        switch (direction) {
            case SOUTH -> {
            }
            case NORTH -> stack.mulPose(
                    Axis.YP.rotationDegrees(
                            180.0F
                    )
            );
            case EAST -> stack.mulPose(
                    Axis.YP.rotationDegrees(
                            90.0F
                    )
            );
            case WEST -> stack.mulPose(
                    Axis.YN.rotationDegrees(
                            90.0F
                    )
            );
            case UP -> stack.mulPose(
                    Axis.XN.rotationDegrees(
                            90.0F
                    )
            );
            case DOWN -> stack.mulPose(
                    Axis.XP.rotationDegrees(
                            90.0F
                    )
            );
        }
    }

    private void drawBox(
            PoseStack stack,
            VertexConsumer consumer,
            int packedLight,
            double sizeX,
            double sizeY,
            double sizeZ) {

        double x0 = -sizeX * 0.5D;
        double x1 = sizeX * 0.5D;
        double y0 = -sizeY * 0.5D;
        double y1 = sizeY * 0.5D;
        double z0 = -sizeZ * 0.5D;
        double z1 = sizeZ * 0.5D;

        PoseStack.Pose pose = stack.last();

        quad(consumer, pose, packedLight,
                x0, y1, z0, x0, y1, z1,
                x1, y1, z1, x1, y1, z0,
                0.0F, 1.0F, 0.0F);
        quad(consumer, pose, packedLight,
                x0, y0, z1, x0, y0, z0,
                x1, y0, z0, x1, y0, z1,
                0.0F, -1.0F, 0.0F);
        quad(consumer, pose, packedLight,
                x0, y0, z1, x1, y0, z1,
                x1, y1, z1, x0, y1, z1,
                0.0F, 0.0F, 1.0F);
        quad(consumer, pose, packedLight,
                x1, y0, z0, x0, y0, z0,
                x0, y1, z0, x1, y1, z0,
                0.0F, 0.0F, -1.0F);
        quad(consumer, pose, packedLight,
                x1, y0, z1, x1, y0, z0,
                x1, y1, z0, x1, y1, z1,
                1.0F, 0.0F, 0.0F);
        quad(consumer, pose, packedLight,
                x0, y0, z0, x0, y0, z1,
                x0, y1, z1, x0, y1, z0,
                -1.0F, 0.0F, 0.0F);
    }

    private void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            float nx, float ny, float nz) {

        vertex(consumer, pose, packedLight,
                x0, y0, z0, 0.0F, 0.0F,
                nx, ny, nz);
        vertex(consumer, pose, packedLight,
                x1, y1, z1, 0.0F, 1.0F,
                nx, ny, nz);
        vertex(consumer, pose, packedLight,
                x2, y2, z2, 1.0F, 1.0F,
                nx, ny, nz);
        vertex(consumer, pose, packedLight,
                x3, y3, z3, 1.0F, 0.0F,
                nx, ny, nz);
    }

    private void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            double x,
            double y,
            double z,
            float u,
            float v,
            float nx,
            float ny,
            float nz) {

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
                        nx,
                        ny,
                        nz
                )
                .endVertex();
    }
}
