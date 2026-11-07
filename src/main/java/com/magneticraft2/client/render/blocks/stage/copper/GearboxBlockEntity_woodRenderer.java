package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.GearboxBlockEntity_wood;
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
 * Visible wooden miter gears + shaft stubs inside the right-angle gearbox casing.
 */
public class GearboxBlockEntity_woodRenderer implements BlockEntityRenderer<GearboxBlockEntity_wood> {
    private static final ResourceLocation OAK =
            new ResourceLocation("minecraft", "textures/block/oak_planks.png");
    private static final ResourceLocation SHAFT =
            new ResourceLocation("minecraft", "textures/block/stripped_oak_log.png");
    private static final ResourceLocation SUPPORT =
            new ResourceLocation("minecraft", "textures/block/oak_planks.png");

    private final ResourceLocation gearTexture;
    private final ResourceLocation shaftTexture;
    private final ResourceLocation supportTexture;
    private final boolean proportionalUv;
    // The wooden coupling is intentionally broad; the Iron drivetrain
    // uses the 2-pixel axle visible on the established Iron Medium Gear.
    private final double portAxleThickness;
    private final double portCouplingThickness;

    public GearboxBlockEntity_woodRenderer(BlockEntityRendererProvider.Context context) {
        // Preserve the accepted wooden gearbox's original 2-texel-minimum
        // sampling without silently changing its texture appearance.
        this(context, OAK, SHAFT, SUPPORT, false, 0.105D, 0.375D);
    }

    /** Reuse the same geometry with appropriate materials and true scale UVs. */
    public GearboxBlockEntity_woodRenderer(BlockEntityRendererProvider.Context context,
                                           ResourceLocation gearTexture,
                                           ResourceLocation shaftTexture,
                                           ResourceLocation supportTexture) {
        this(context, gearTexture, shaftTexture, supportTexture, true, 0.105D, 0.375D);
    }

    /**
     * Explicit physical axle/coupling dimensions for differently-sized
     * drivetrains. Does not confuse model dimensions with the UV policy.
     */
    public GearboxBlockEntity_woodRenderer(BlockEntityRendererProvider.Context context,
                                           ResourceLocation gearTexture,
                                           ResourceLocation shaftTexture,
                                           ResourceLocation supportTexture,
                                           double portAxleThickness,
                                           double portCouplingThickness) {
        this(context, gearTexture, shaftTexture, supportTexture, true,
                portAxleThickness, portCouplingThickness);
    }

    private GearboxBlockEntity_woodRenderer(BlockEntityRendererProvider.Context context,
                                            ResourceLocation gearTexture,
                                            ResourceLocation shaftTexture,
                                            ResourceLocation supportTexture,
                                            boolean proportionalUv,
                                            double portAxleThickness,
                                            double portCouplingThickness) {
        this.gearTexture = gearTexture;
        this.shaftTexture = shaftTexture;
        this.supportTexture = supportTexture;
        this.proportionalUv = proportionalUv;
        this.portAxleThickness = portAxleThickness;
        this.portCouplingThickness = portCouplingThickness;
    }

    @Override
    public void render(GearboxBlockEntity_wood gearbox,
                       float partialTicks,
                       PoseStack stack,
                       MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {
        stack.pushPose();
        stack.translate(0.5D, 0.5D, 0.5D);

        Direction input = gearbox.getInputDirection();
        Direction output = gearbox.getOutputDirection();

        float inputAngle = gearbox.getVisualRotationDegrees(partialTicks);
        float outputAngle = gearbox.getOutputVisualRotationDegrees(partialTicks);

        // A MultiBufferSource may reuse its underlying BufferBuilder across
        // different RenderTypes. Fetch the consumer JUST BEFORE each material
        // pass; holding Iron and Bronze consumers simultaneously previously
        // caused the bronze texture to cover the entire Iron gearbox.
        // Draw both ports of each material together to avoid extra flushes.
        VertexConsumer gears =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(gearTexture));
        renderBevelGear(stack, gears, packedLight,
                input, inputAngle * axisDirectionSign(input), false);
        renderBevelGear(stack, gears, packedLight,
                output, outputAngle * axisDirectionSign(output) + 22.5F, true);

        VertexConsumer shafts =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(shaftTexture));
        renderPortShaft(stack, shafts, packedLight,
                input, inputAngle * axisDirectionSign(input));
        renderPortShaft(stack, shafts, packedLight,
                output, outputAngle * axisDirectionSign(output));

        // Structural supports are Iron for the Iron gearbox and Oak for
        // the original wooden gearbox. Only the actual bearing journals
        // receive the bronze material on the Iron variant.
        VertexConsumer structure = bufferSource.getBuffer(
                RenderType.entityCutoutNoCull(
                        proportionalUv ? shaftTexture : supportTexture));
        renderPortSupport(stack, structure, packedLight, input, false);
        renderPortSupport(stack, structure, packedLight, output, false);

        VertexConsumer bearings =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(supportTexture));
        renderPortSupport(stack, bearings, packedLight, input, true);
        renderPortSupport(stack, bearings, packedLight, output, true);

        stack.popPose();
    }

    /**
     * Connects a normal 6/16 Wooden Shaft at the block face to the compact
     * internal bevel-gear axle. The previous renderer used one 6/16-thick box
     * all the way through the gearbox; on UP/DOWN ports that appeared as a huge
     * wooden cube sitting directly on top of the gears.
     */
    /**
     * Rotating axle/coupling only. Bearings and structural supports are
     * drawn in independent material batches after the shafts.
     */
    private void renderPortShaft(PoseStack stack,
                                 VertexConsumer consumer,
                                 int packedLight,
                                 Direction port,
                                 float rotation) {
        stack.pushPose();
        orientLocalXToDirection(stack, port);
        stack.mulPose(Axis.XP.rotationDegrees(rotation));

        // Continuous axle from the gear hub to the outer coupling.
        drawBoxAtLocalX(
                stack, consumer, packedLight,
                0.265D, 0.470D, portAxleThickness
        );

        // Port coupling shares the Iron Shaft's 2px cross-section (wood
        // retains its existing 6px coupling).
        drawBoxAtLocalX(
                stack, consumer, packedLight,
                0.455D, 0.090D, portCouplingThickness
        );

        stack.popPose();
    }

    /**
     * Stationary gearbox support. Draw either the structural steel/wood
     * brace pieces OR the bronze/wood center bearing, never both through
     * simultaneous consumers backed by a shared mutable BufferBuilder.
     */
    private void renderPortSupport(PoseStack stack,
                                   VertexConsumer consumer,
                                   int packedLight,
                                   Direction port,
                                   boolean bearingOnly) {
        stack.pushPose();
        orientLocalXToDirection(stack, port);

        if (bearingOnly) {
            // Supported central bearing sleeve around the rotating axle.
            stack.pushPose();
            stack.translate(0.405D, 0.0D, 0.0D);
            drawBox(stack, consumer, packedLight,
                    0.125D, 0.245D, 0.245D);
            stack.popPose();
        } else {
            // Full-width brace terminates in the inner cage rails.
            stack.pushPose();
            stack.translate(0.405D, 0.0D, 0.0D);
            drawBox(stack, consumer, packedLight,
                    0.095D, 0.095D, 0.875D);
            stack.popPose();

            // Top/bottom retaining cheeks capture the bearing.
            stack.pushPose();
            stack.translate(0.405D, 0.170D, 0.0D);
            drawBox(stack, consumer, packedLight,
                    0.105D, 0.095D, 0.310D);
            stack.popPose();

            stack.pushPose();
            stack.translate(0.405D, -0.170D, 0.0D);
            drawBox(stack, consumer, packedLight,
                    0.105D, 0.095D, 0.310D);
            stack.popPose();
        }

        stack.popPose();
    }

    private void drawBoxAtLocalX(PoseStack stack,
                                 VertexConsumer consumer,
                                 int packedLight,
                                 double centerX,
                                 double length,
                                 double thickness) {
        stack.pushPose();
        stack.translate(centerX, 0.0D, 0.0D);
        drawBox(
                stack,
                consumer,
                packedLight,
                length,
                thickness,
                thickness
        );
        stack.popPose();
    }

    private void renderBevelGear(PoseStack stack,
                                 VertexConsumer consumer,
                                 int packedLight,
                                 Direction port,
                                 float rotation,
                                 boolean secondary) {
        stack.pushPose();

        // Local +X points out through the shaft. The miter apex is therefore
        // toward local -X at the block center.
        orientLocalXToDirection(stack, port);
        stack.translate(0.135D, 0.0D, 0.0D);
        stack.mulPose(Axis.XP.rotationDegrees(rotation));

        drawBox(
                stack,
                consumer,
                packedLight,
                0.13D,
                0.14D,
                0.14D
        );

        // Three connected stepped rings form a readable wooden bevel wheel.
        // The previous version was eight oversized floating cubes and looked
        // more like paddles than a gear.
        renderRing(
                stack,
                consumer,
                packedLight,
                0.150D,
                0.040D,
                0.052D,
                0.055D,
                16,
                0.035D
        );
        renderRing(
                stack,
                consumer,
                packedLight,
                0.116D,
                0.036D,
                0.047D,
                0.048D,
                16,
                -0.005D
        );
        renderRing(
                stack,
                consumer,
                packedLight,
                0.082D,
                0.030D,
                0.040D,
                0.040D,
                16,
                -0.045D
        );

        for (int spoke = 0; spoke < 4; spoke++) {
            stack.pushPose();
            stack.mulPose(
                    Axis.XP.rotationDegrees(spoke * 90.0F)
            );
            stack.translate(
                    0.005D,
                    0.082D,
                    0.0D
            );
            stack.mulPose(
                    Axis.ZP.rotationDegrees(18.0F)
            );
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    0.050D,
                    0.115D,
                    0.036D
            );
            stack.popPose();
        }

        for (int tooth = 0; tooth < 8; tooth++) {
            stack.pushPose();
            stack.mulPose(
                    Axis.XP.rotationDegrees(tooth * 45.0F)
            );
            stack.translate(
                    0.040D,
                    0.188D,
                    0.0D
            );
            stack.mulPose(
                    Axis.ZP.rotationDegrees(38.0F)
            );
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    0.082D,
                    0.070D,
                    0.060D
            );
            stack.popPose();
        }

        stack.popPose();
    }

    private void renderRing(PoseStack stack,
                            VertexConsumer consumer,
                            int packedLight,
                            double radius,
                            double radialDepth,
                            double tangentWidth,
                            double axialDepth,
                            int segments,
                            double axialOffset) {
        for (int segment = 0; segment < segments; segment++) {
            stack.pushPose();
            stack.mulPose(
                    Axis.XP.rotationDegrees(
                            segment * (360.0F / segments)
                    )
            );
            stack.translate(
                    axialOffset,
                    radius,
                    0.0D
            );
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    axialDepth,
                    radialDepth,
                    tangentWidth
            );
            stack.popPose();
        }
    }

    private int axisDirectionSign(Direction direction) {
        return switch (direction) {
            case EAST, UP, SOUTH -> 1;
            case WEST, DOWN, NORTH -> -1;
        };
    }

    private void orientLocalXToDirection(PoseStack stack, Direction direction) {
        switch (direction) {
            case EAST -> {
                // Local +X already points east.
            }
            case WEST -> stack.mulPose(
                    Axis.YP.rotationDegrees(180.0F)
            );
            case UP -> stack.mulPose(
                    Axis.ZP.rotationDegrees(90.0F)
            );
            case DOWN -> stack.mulPose(
                    Axis.ZN.rotationDegrees(90.0F)
            );
            case SOUTH -> stack.mulPose(
                    Axis.YN.rotationDegrees(90.0F)
            );
            case NORTH -> stack.mulPose(
                    Axis.YP.rotationDegrees(90.0F)
            );
        }
    }

    private void drawBox(PoseStack stack,
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

        /*
         * Do not stretch a whole 16x16 block texture across tiny mechanical
         * parts, but also do not sample a microscopic 1-2% strip of it.
         *
         * Quantise each face to an integer Minecraft texel window and keep a
         * minimum 2x2 texel patch. This gives small teeth/spokes readable wood
         * grain without smearing or over-compressing the texture.
         */
        // Metal geometry uses the face's real block-unit dimensions, so a
        // 0.04-block tooth never samples a 2/16-wide (stretched) texel strip.
        // Keep the previously accepted minimum texture window for WOOD only.
        float uvX = proportionalUv ? (float) sizeX : texelWindow(sizeX);
        float uvY = proportionalUv ? (float) sizeY : texelWindow(sizeY);
        float uvZ = proportionalUv ? (float) sizeZ : texelWindow(sizeZ);

        PoseStack.Pose pose = stack.last();

        // Top / bottom: X by Z.
        quad(consumer, pose, packedLight,
                x0, y1, z0,  x0, y1, z1,  x1, y1, z1,  x1, y1, z0,
                0.0F, 1.0F, 0.0F,
                uvX, uvZ);
        quad(consumer, pose, packedLight,
                x0, y0, z1,  x0, y0, z0,  x1, y0, z0,  x1, y0, z1,
                0.0F, -1.0F, 0.0F,
                uvX, uvZ);

        // North / south: X by Y.
        quad(consumer, pose, packedLight,
                x0, y0, z1,  x1, y0, z1,  x1, y1, z1,  x0, y1, z1,
                0.0F, 0.0F, 1.0F,
                uvX, uvY);
        quad(consumer, pose, packedLight,
                x1, y0, z0,  x0, y0, z0,  x0, y1, z0,  x1, y1, z0,
                0.0F, 0.0F, -1.0F,
                uvX, uvY);

        // East / west: Z by Y.
        quad(consumer, pose, packedLight,
                x1, y0, z1,  x1, y0, z0,  x1, y1, z0,  x1, y1, z1,
                1.0F, 0.0F, 0.0F,
                uvZ, uvY);
        quad(consumer, pose, packedLight,
                x0, y0, z0,  x0, y0, z1,  x0, y1, z1,  x0, y1, z0,
                -1.0F, 0.0F, 0.0F,
                uvZ, uvY);
    }

    private float texelWindow(double blockSize) {
        int texels =
                Math.max(
                        2,
                        Math.min(
                                16,
                                (int) Math.round(
                                        blockSize * 16.0D
                                )
                        )
                );

        return texels / 16.0F;
    }

    private void quad(VertexConsumer consumer,
                      PoseStack.Pose pose,
                      int packedLight,
                      double x0, double y0, double z0,
                      double x1, double y1, double z1,
                      double x2, double y2, double z2,
                      double x3, double y3, double z3,
                      float normalX,
                      float normalY,
                      float normalZ,
                      float maxU,
                      float maxV) {
        vertex(
                consumer, pose, packedLight,
                x0, y0, z0,
                0.0F, 0.0F,
                normalX, normalY, normalZ
        );
        vertex(
                consumer, pose, packedLight,
                x1, y1, z1,
                0.0F, maxV,
                normalX, normalY, normalZ
        );
        vertex(
                consumer, pose, packedLight,
                x2, y2, z2,
                maxU, maxV,
                normalX, normalY, normalZ
        );
        vertex(
                consumer, pose, packedLight,
                x3, y3, z3,
                maxU, 0.0F,
                normalX, normalY, normalZ
        );
    }

    private void vertex(VertexConsumer consumer,
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
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), normalX, normalY, normalZ)
                .endVertex();
    }
}
