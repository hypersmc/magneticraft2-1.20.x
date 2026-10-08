package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.CustomGearboxBlockEntity_wood;
import com.magneticraft2.common.blockentity.stage.copper.CustomGearboxBlockEntity_wood.InternalComponent;
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
 * Renders the player's actual 3x3x3 custom gearbox assembly inside the open housing.
 */
public class CustomGearboxBlockEntity_woodRenderer
        implements BlockEntityRenderer<CustomGearboxBlockEntity_wood> {
    private static final ResourceLocation OAK =
            new ResourceLocation("minecraft", "textures/block/oak_planks.png");
    private static final ResourceLocation SHAFT =
            new ResourceLocation("minecraft", "textures/block/stripped_oak_log.png");
    private static final ResourceLocation SUPPORT =
            new ResourceLocation("minecraft", "textures/block/stripped_spruce_log.png");

    private static final double GRID_SPACING = 0.26D;
    private static final double SHAFT_THICKNESS = 0.085D;
    private static final double SHAFT_CELL_LENGTH = 0.30D;
    private static final double OUTSIDE_SHAFT_THICKNESS = 6.0D / 16.0D;

    public CustomGearboxBlockEntity_woodRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CustomGearboxBlockEntity_wood gearbox,
                       float partialTicks,
                       PoseStack stack,
                       MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {
        stack.pushPose();
        stack.translate(0.5D, 0.5D, 0.5D);

        VertexConsumer oak =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(OAK));
        VertexConsumer shaft =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(SHAFT));
        VertexConsumer support =
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(SUPPORT));

        float referenceAngle =
                gearbox.getVisualRotationDegrees(partialTicks);

        for (int index = 0;
             index < CustomGearboxBlockEntity_wood.CELL_COUNT;
             index++) {
            InternalComponent component = gearbox.getComponent(index);
            if (component == InternalComponent.EMPTY
                    || component.getAxis() == null) {
                continue;
            }

            int[] xyz =
                    CustomGearboxBlockEntity_wood.coordinates(index);

            double cx = (xyz[0] - 1) * GRID_SPACING;
            double cy = (xyz[1] - 1) * GRID_SPACING;
            double cz = (xyz[2] - 1) * GRID_SPACING;

            int directionSign =
                    gearbox.getComponentDirectionSign(index);
            float angle = directionSign == 0
                    ? 0.0F
                    : referenceAngle * directionSign;
            float toothPhase =
                    gearbox.getComponentVisualPhaseDegrees(index);

            stack.pushPose();
            stack.translate(cx, cy, cz);

            if (component.isGear()) {
                renderInternalGear(
                        stack,
                        oak,
                        shaft,
                        packedLight,
                        component.getAxis(),
                        angle,
                        toothPhase,
                        gearbox.getBevelSideMask(index)
                );
            } else {
                renderInternalShaft(
                        stack,
                        shaft,
                        packedLight,
                        component.getAxis(),
                        angle
                );
            }

            stack.popPose();
        }

        for (Direction port : gearbox.getActivePorts()) {
            int portSign = gearbox.getPortDirectionSign(port);
            float portAngle = portSign == 0
                    ? 0.0F
                    : referenceAngle
                    * portSign
                    * axisDirectionSign(port);

            renderPortAdapter(
                    stack,
                    shaft,
                    support,
                    packedLight,
                    port,
                    portAngle
            );
        }

        stack.popPose();
    }

    private void renderInternalShaft(PoseStack stack,
                                     VertexConsumer consumer,
                                     int packedLight,
                                     Direction.Axis axis,
                                     float rotation) {
        stack.pushPose();
        orientLocalXToAxis(stack, axis);
        stack.mulPose(Axis.XP.rotationDegrees(rotation));

        drawBox(
                stack,
                consumer,
                packedLight,
                SHAFT_CELL_LENGTH,
                SHAFT_THICKNESS,
                SHAFT_THICKNESS
        );

        stack.popPose();
    }

    private void renderInternalGear(PoseStack stack,
                                    VertexConsumer oak,
                                    VertexConsumer shaft,
                                    int packedLight,
                                    Direction.Axis axis,
                                    float rotation,
                                    float toothPhase,
                                    int bevelSideMask) {
        orientLocalXToAxis(stack, axis);
        stack.mulPose(Axis.XP.rotationDegrees(rotation));

        // Axle/hub through the center. The old renderer was essentially eight
        // disconnected wooden paddles around this box; keep the hub but build a
        // real rim, spokes and teeth around it.
        drawBox(
                stack,
                shaft,
                packedLight,
                0.24D,
                0.070D,
                0.070D
        );

        // Tooth phase is an installation/indexing offset, not a shaft
        // direction change. Apply it only to the wheel profile so the square
        // axle remains aligned with connected internal/external shafts.
        stack.pushPose();
        stack.mulPose(
                Axis.XP.rotationDegrees(toothPhase)
        );

        if (bevelSideMask == 0) {
            renderSpurGearProfile(
                    stack,
                    oak,
                    packedLight
            );
        } else {
            if ((bevelSideMask & 1) != 0) {
                renderBevelGearProfile(
                        stack,
                        oak,
                        packedLight,
                        -1
                );
            }

            if ((bevelSideMask & 2) != 0) {
                renderBevelGearProfile(
                        stack,
                        oak,
                        packedLight,
                        1
                );
            }
        }

        stack.popPose();
    }

    /**
     * Blocky wooden spur gear with a connected rim, four spokes and eight
     * compact teeth. It deliberately keeps the 8-tooth mechanical identity
     * while reading as one gear instead of eight floating cubes.
     */
    private void renderSpurGearProfile(PoseStack stack,
                                       VertexConsumer oak,
                                       int packedLight) {
        renderRing(
                stack,
                oak,
                packedLight,
                0.095D,
                0.034D,
                0.043D,
                0.050D,
                16,
                0.0D
        );

        for (int spoke = 0; spoke < 4; spoke++) {
            stack.pushPose();
            stack.mulPose(
                    Axis.XP.rotationDegrees(spoke * 90.0F)
            );
            stack.translate(0.0D, 0.057D, 0.0D);
            drawBox(
                    stack,
                    oak,
                    packedLight,
                    0.046D,
                    0.090D,
                    0.032D
            );
            stack.popPose();
        }

        for (int tooth = 0; tooth < 8; tooth++) {
            stack.pushPose();
            stack.mulPose(
                    Axis.XP.rotationDegrees(tooth * 45.0F)
            );
            stack.translate(0.0D, 0.132D, 0.0D);
            drawBox(
                    stack,
                    oak,
                    packedLight,
                    0.060D,
                    0.050D,
                    0.050D
            );
            stack.popPose();
        }
    }

    /**
     * Stepped miter/bevel profile. The side sign points at the actual common
     * shaft intersection for this gear, so EAST/WEST and UP/DOWN arrangements
     * no longer all lean in the same arbitrary direction.
     */
    private void renderBevelGearProfile(PoseStack stack,
                                        VertexConsumer oak,
                                        int packedLight,
                                        int sideSign) {
        stack.pushPose();

        // Pull the wheel toward its real miter intersection. This is what makes
        // perpendicular pairs visually converge instead of floating in their
        // individual grid cells.
        stack.translate(
                sideSign * (GRID_SPACING * 0.5D),
                0.0D,
                0.0D
        );

        renderRing(
                stack,
                oak,
                packedLight,
                0.125D,
                0.036D,
                0.046D,
                0.040D,
                16,
                -sideSign * 0.030D
        );
        renderRing(
                stack,
                oak,
                packedLight,
                0.098D,
                0.032D,
                0.041D,
                0.036D,
                16,
                sideSign * 0.008D
        );
        renderRing(
                stack,
                oak,
                packedLight,
                0.070D,
                0.028D,
                0.036D,
                0.032D,
                16,
                sideSign * 0.042D
        );

        for (int spoke = 0; spoke < 4; spoke++) {
            stack.pushPose();
            stack.mulPose(
                    Axis.XP.rotationDegrees(spoke * 90.0F)
            );
            stack.translate(
                    -sideSign * 0.012D,
                    0.071D,
                    0.0D
            );
            stack.mulPose(
                    Axis.ZP.rotationDegrees(
                            -sideSign * 18.0F
                    )
            );
            drawBox(
                    stack,
                    oak,
                    packedLight,
                    0.040D,
                    0.100D,
                    0.030D
            );
            stack.popPose();
        }

        for (int tooth = 0; tooth < 8; tooth++) {
            stack.pushPose();
            stack.mulPose(
                    Axis.XP.rotationDegrees(tooth * 45.0F)
            );
            stack.translate(
                    -sideSign * 0.037D,
                    0.145D,
                    0.0D
            );
            stack.mulPose(
                    Axis.ZP.rotationDegrees(
                            -sideSign * 40.0F
                    )
            );
            drawBox(
                    stack,
                    oak,
                    packedLight,
                    0.070D,
                    0.052D,
                    0.050D
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

    /**
     * Bridges the compact internal gearbox shaft to the full-size Wooden Shaft
     * outside the block. The last stage is exactly 6/16 blocks thick, matching
     * shaft_wood.json at the block boundary.
     */
    private void renderPortAdapter(PoseStack stack,
                                   VertexConsumer consumer,
                                   VertexConsumer support,
                                   int packedLight,
                                   Direction port,
                                   float rotation) {
        stack.pushPose();
        orientLocalXToDirection(stack, port);

        renderPortSupport(
                stack,
                support,
                packedLight
        );

        stack.mulPose(Axis.XP.rotationDegrees(rotation));

        // The port cell center is 0.26 blocks from the gearbox center. Its
        // internal shaft already reaches outward to ~0.41, so these overlapping
        // stages make one continuous wooden coupling from the tiny mechanism to
        // the external shaft instead of a floating pencil-thin stub.
        drawBoxAtLocalX(
                stack,
                consumer,
                packedLight,
                0.355D,
                0.190D,
                0.105D
        );
        drawBoxAtLocalX(
                stack,
                consumer,
                packedLight,
                0.405D,
                0.130D,
                0.165D
        );
        drawBoxAtLocalX(
                stack,
                consumer,
                packedLight,
                0.450D,
                0.100D,
                0.255D
        );
        drawBoxAtLocalX(
                stack,
                consumer,
                packedLight,
                0.475D,
                0.050D,
                OUTSIDE_SHAFT_THICKNESS
        );

        stack.popPose();
    }

    private void renderPortSupport(
            PoseStack stack,
            VertexConsumer consumer,
            int packedLight) {
        // Fixed wooden bearing with straps tied into the open housing frame.
        stack.pushPose();

        stack.pushPose();
        stack.translate(0.405D, 0.0D, 0.0D);
        drawBox(
                stack,
                consumer,
                packedLight,
                0.095D,
                0.235D,
                0.235D
        );
        stack.popPose();

        stack.pushPose();
        stack.translate(0.405D, 0.285D, 0.0D);
        drawBox(
                stack,
                consumer,
                packedLight,
                0.085D,
                0.325D,
                0.075D
        );
        stack.popPose();

        stack.pushPose();
        stack.translate(0.405D, -0.285D, 0.0D);
        drawBox(
                stack,
                consumer,
                packedLight,
                0.085D,
                0.325D,
                0.075D
        );
        stack.popPose();

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

    private void orientLocalXToDirection(PoseStack stack,
                                         Direction direction) {
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

    private int axisDirectionSign(Direction direction) {
        return switch (direction) {
            case EAST, UP, SOUTH -> 1;
            case WEST, DOWN, NORTH -> -1;
        };
    }

    private void orientLocalXToAxis(PoseStack stack,
                                    Direction.Axis axis) {
        if (axis == Direction.Axis.Y) {
            stack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        } else if (axis == Direction.Axis.Z) {
            stack.mulPose(Axis.YN.rotationDegrees(90.0F));
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
         * Keep Minecraft's native texel density: one block = one full 16x16
         * texture. Small mechanical cuboids should therefore use only the
         * matching fraction of the texture instead of stretching the whole
         * image over every face.
         */
        float uvX = (float) Math.min(1.0D, Math.max(0.001D, sizeX));
        float uvY = (float) Math.min(1.0D, Math.max(0.001D, sizeY));
        float uvZ = (float) Math.min(1.0D, Math.max(0.001D, sizeZ));

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
                .normal(
                        pose.normal(),
                        normalX,
                        normalY,
                        normalZ
                )
                .endVertex();
    }
}
