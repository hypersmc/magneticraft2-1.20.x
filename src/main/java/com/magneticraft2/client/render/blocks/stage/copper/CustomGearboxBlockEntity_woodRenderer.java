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

    private static final double GRID_SPACING = 0.26D;
    private static final double SHAFT_THICKNESS = 0.085D;
    private static final double SHAFT_CELL_LENGTH = 0.30D;

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
                        gearbox.isBevelGear(index)
                );
            } else {
                renderInternalShaft(
                        stack,
                        shaft,
                        packedLight,
                        component.getAxis()
                );
            }

            stack.popPose();
        }

        for (Direction port : gearbox.getActivePorts()) {
            renderPortStub(
                    stack,
                    shaft,
                    packedLight,
                    port
            );
        }

        stack.popPose();
    }

    private void renderInternalShaft(PoseStack stack,
                                     VertexConsumer consumer,
                                     int packedLight,
                                     Direction.Axis axis) {
        if (axis == Direction.Axis.X) {
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    SHAFT_CELL_LENGTH,
                    SHAFT_THICKNESS,
                    SHAFT_THICKNESS
            );
        } else if (axis == Direction.Axis.Y) {
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    SHAFT_THICKNESS,
                    SHAFT_CELL_LENGTH,
                    SHAFT_THICKNESS
            );
        } else {
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    SHAFT_THICKNESS,
                    SHAFT_THICKNESS,
                    SHAFT_CELL_LENGTH
            );
        }
    }

    private void renderInternalGear(PoseStack stack,
                                    VertexConsumer oak,
                                    VertexConsumer shaft,
                                    int packedLight,
                                    Direction.Axis axis,
                                    float rotation,
                                    boolean bevel) {
        orientLocalXToAxis(stack, axis);
        stack.mulPose(Axis.XP.rotationDegrees(rotation));

        // Hub/axle.
        drawBox(
                stack,
                shaft,
                packedLight,
                0.14D,
                0.075D,
                0.075D
        );

        // Perpendicular miter pairs sit on diagonal cells, so their pitch circles need
        // to reach farther than ordinary same-plane spur gears. Keeping the two sizes
        // distinct makes both arrangements visibly meet instead of floating apart.
        double radius = bevel ? 0.160D : 0.110D;
        double radial = bevel ? 0.080D : 0.072D;
        double tangent = bevel ? 0.080D : 0.072D;
        double axial = bevel ? 0.065D : 0.050D;

        for (int tooth = 0; tooth < 8; tooth++) {
            stack.pushPose();
            stack.mulPose(
                    Axis.XP.rotationDegrees(tooth * 45.0F)
            );
            stack.translate(
                    bevel ? -0.028D : 0.0D,
                    radius,
                    0.0D
            );

            if (bevel) {
                // A stronger inward lean makes the diagonal pair converge at the corner
                // between their cells like a real miter set.
                stack.mulPose(Axis.ZP.rotationDegrees(30.0F));
            }

            drawBox(
                    stack,
                    oak,
                    packedLight,
                    axial,
                    radial,
                    tangent
            );

            stack.popPose();
        }
    }

    private void renderPortStub(PoseStack stack,
                                VertexConsumer consumer,
                                int packedLight,
                                Direction port) {
        double cx = port.getStepX() * 0.39D;
        double cy = port.getStepY() * 0.39D;
        double cz = port.getStepZ() * 0.39D;

        stack.pushPose();
        stack.translate(cx, cy, cz);

        if (port.getAxis() == Direction.Axis.X) {
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    0.24D,
                    SHAFT_THICKNESS,
                    SHAFT_THICKNESS
            );
        } else if (port.getAxis() == Direction.Axis.Y) {
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    SHAFT_THICKNESS,
                    0.24D,
                    SHAFT_THICKNESS
            );
        } else {
            drawBox(
                    stack,
                    consumer,
                    packedLight,
                    SHAFT_THICKNESS,
                    SHAFT_THICKNESS,
                    0.24D
            );
        }

        stack.popPose();
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

    private void quad(VertexConsumer consumer,
                      PoseStack.Pose pose,
                      int packedLight,
                      double x0,
                      double y0,
                      double z0,
                      double x1,
                      double y1,
                      double z1,
                      double x2,
                      double y2,
                      double z2,
                      double x3,
                      double y3,
                      double z3,
                      float normalX,
                      float normalY,
                      float normalZ) {
        vertex(
                consumer,
                pose,
                packedLight,
                x0, y0, z0,
                0.0F, 0.0F,
                normalX, normalY, normalZ
        );
        vertex(
                consumer,
                pose,
                packedLight,
                x1, y1, z1,
                0.0F, 1.0F,
                normalX, normalY, normalZ
        );
        vertex(
                consumer,
                pose,
                packedLight,
                x2, y2, z2,
                1.0F, 1.0F,
                normalX, normalY, normalZ
        );
        vertex(
                consumer,
                pose,
                packedLight,
                x3, y3, z3,
                1.0F, 0.0F,
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
