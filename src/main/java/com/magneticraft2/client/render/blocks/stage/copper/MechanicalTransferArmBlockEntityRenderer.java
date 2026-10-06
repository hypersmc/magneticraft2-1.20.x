package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.MechanicalTransferArmBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Articulated Copper Age transfer arm.
 *
 * The lower baked block is the drive/control plinth. A lightweight reserved
 * upper block makes the machine physically 1x2x1, while everything that
 * mechanically moves is drawn here:
 *
 *   exposed Gear V2 wheel -> hollow turntable -> shoulder -> reinforced boom
 *   -> forked forearm -> compact wrist/claw
 *
 * The drive wheel terminates below the hollow turntable; the Transfer Arm never
 * renders the normal gear block's full-height shaft. The movement timing is
 * intentionally unchanged: it lifts/retracts during the sweep and reaches down
 * at the configured source/destination.
 */
public class MechanicalTransferArmBlockEntityRenderer
        implements BlockEntityRenderer<MechanicalTransferArmBlockEntity> {

    private static final ResourceLocation GEAR_MODEL =
            model("mechanical_transfer_arm_drive_gear");
    private static final ResourceLocation TURRET_MODEL =
            model("mechanical_transfer_arm_turret");
    private static final ResourceLocation LOWER_ARM_MODEL =
            model("mechanical_transfer_arm_lower_arm");
    private static final ResourceLocation FOREARM_MODEL =
            model("mechanical_transfer_arm_forearm");
    private static final ResourceLocation CLAW_BODY_MODEL =
            model("mechanical_transfer_arm_claw_body");
    private static final ResourceLocation CLAW_LEFT_MODEL =
            model("mechanical_transfer_arm_claw_left");
    private static final ResourceLocation CLAW_RIGHT_MODEL =
            model("mechanical_transfer_arm_claw_right");

    private static final ResourceLocation ORANGE_BUTTON_MODEL =
            model("mechanical_transfer_arm_button_orange");
    private static final ResourceLocation BLUE_BUTTON_MODEL =
            model("mechanical_transfer_arm_button_blue");
    private static final ResourceLocation FILTER_ALLOW_MODEL =
            model("mechanical_transfer_arm_filter_allow");
    private static final ResourceLocation FILTER_DENY_MODEL =
            model("mechanical_transfer_arm_filter_deny");

    // Authored model pivots in block-local coordinates. The machine is a real
    // 1x2x1 structure: gear + controls occupy the lower block while these
    // articulated pivots live in the reserved upper block.
    private static final double SHOULDER_Y =
            18.35D / 16.0D;
    private static final double ELBOW_Y =
            24.0D / 16.0D;
    private static final double WRIST_Y =
            30.1D / 16.0D;

    private final Map<ResourceLocation, List<BakedQuad>> quadCache =
            new HashMap<>();

    public MechanicalTransferArmBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    private static ResourceLocation model(
            String path) {
        return new ResourceLocation(
                "magneticraft2",
                "block/" + path
        );
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public void render(
            MechanicalTransferArmBlockEntity arm,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {

        renderBaseGear(
                arm,
                partialTicks,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        Direction source =
                arm.getSourceSide();
        Direction destination =
                arm.getDestinationSide();

        Direction facing =
                Direction.NORTH;
        BlockState blockState =
                arm.getBlockState();

        if (blockState.hasProperty(
                net.minecraft.world.level.block.DirectionalBlock.FACING
        )) {
            facing =
                    blockState.getValue(
                            net.minecraft.world.level.block.DirectionalBlock.FACING
                    );
        }

        if (source != null) {
            renderControlButton(
                    ORANGE_BUTTON_MODEL,
                    facing,
                    source,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay
            );
        }

        if (destination != null) {
            renderControlButton(
                    BLUE_BUTTON_MODEL,
                    facing,
                    destination,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay
            );
        }

        renderFilter(
                arm,
                facing,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        renderPanelReadout(
                arm,
                facing,
                poseStack,
                buffer,
                packedLight
        );

        float progress =
                arm.getVisualCycleProgress(
                        partialTicks
                );
        float yaw =
                getArmYaw(
                        arm,
                        progress
                );

        float legProgress =
                progress <= 0.5F
                        ? progress * 2.0F
                        : (progress - 0.5F) * 2.0F;

        boolean outbound =
                progress <= 0.5F;

        boolean startHigh =
                outbound
                        ? arm.isSourceHigh()
                        : arm.isDestinationHigh();
        boolean endHigh =
                outbound
                        ? arm.isDestinationHigh()
                        : arm.isSourceHigh();

        /*
         * Each transfer leg is deliberately split into three mechanical
         * phases rather than one continuous arc:
         *
         *  0.00 - 0.22  lift straight away from the current endpoint
         *  0.22 - 0.78  rotate while the claw is safely raised
         *  0.78 - 1.00  lower onto the next endpoint
         *
         * LOW/HIGH only changes the endpoint reach. The travel pose is shared,
         * which makes it obvious that the arm first clears the production line
         * before it turns.
         */
        float liftEnd =
                0.22F;
        float lowerStart =
                0.78F;

        float startShoulder =
                endpointShoulder(
                        startHigh
                );
        float startElbow =
                endpointElbow(
                        startHigh
                );
        float endShoulder =
                endpointShoulder(
                        endHigh
                );
        float endElbow =
                endpointElbow(
                        endHigh
                );

        float travelShoulder =
                -8.0F;
        float travelElbow =
                -34.0F;

        float shoulderPitch;
        float elbowPitch;

        if (legProgress < liftEnd) {
            float t =
                    smoothStep(
                            legProgress
                                    / liftEnd
                    );

            shoulderPitch =
                    Mth.lerp(
                            t,
                            startShoulder,
                            travelShoulder
                    );
            elbowPitch =
                    Mth.lerp(
                            t,
                            startElbow,
                            travelElbow
                    );
        } else if (legProgress < lowerStart) {
            shoulderPitch =
                    travelShoulder;
            elbowPitch =
                    travelElbow;
        } else {
            float t =
                    smoothStep(
                            (legProgress
                                    - lowerStart)
                                    / (1.0F
                                    - lowerStart)
                    );

            shoulderPitch =
                    Mth.lerp(
                            t,
                            travelShoulder,
                            endShoulder
                    );
            elbowPitch =
                    Mth.lerp(
                            t,
                            travelElbow,
                            endElbow
                    );
        }

        // Keep the claw hanging vertically even while the two links articulate.
        float wristPitch =
                -(shoulderPitch
                        + elbowPitch);

        poseStack.pushPose();

        rotateAroundY(
                poseStack,
                0.5D,
                0.5D,
                yaw
        );

        renderModel(
                TURRET_MODEL,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        rotateAroundX(
                poseStack,
                0.5D,
                SHOULDER_Y,
                0.5D,
                shoulderPitch
        );

        renderModel(
                LOWER_ARM_MODEL,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        rotateAroundX(
                poseStack,
                0.5D,
                ELBOW_Y,
                0.5D,
                elbowPitch
        );

        renderModel(
                FOREARM_MODEL,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        rotateAroundX(
                poseStack,
                0.5D,
                WRIST_Y,
                0.5D,
                wristPitch
        );

        renderModel(
                CLAW_BODY_MODEL,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        ItemStack carried =
                arm.getCarriedStack();

        boolean gripping =
                !carried.isEmpty();

        renderClawFingers(
                gripping,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        if (gripping) {
            renderCarriedItem(
                    arm,
                    carried,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay
            );
        }

        poseStack.popPose();
    }

    private void renderBaseGear(
            MechanicalTransferArmBlockEntity arm,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        poseStack.pushPose();

        // This model is the existing Gear V2 wooden wheel geometry with only
        // the stock model's full-height center shaft omitted. The wheel still
        // uses the same teeth and textures, but now terminates beneath the
        // hollow transfer-arm turntable.
        poseStack.translate(
                0.0D,
                -1.50D / 16.0D,
                0.0D
        );

        rotateAroundY(
                poseStack,
                0.5D,
                0.5D,
                arm.getVisualRotationDegrees(
                        partialTicks
                )
        );

        renderModel(
                GEAR_MODEL,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    private void renderClawFingers(
            boolean gripping,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        // Empty claw opens wide. While carrying an item the fingers close
        // inward but never overlap the item itself.
        double spread =
                gripping
                        ? 0.018D
                        : 0.090D;

        poseStack.pushPose();
        poseStack.translate(
                -spread,
                0.0D,
                0.0D
        );
        renderModel(
                CLAW_LEFT_MODEL,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(
                spread,
                0.0D,
                0.0D
        );
        renderModel(
                CLAW_RIGHT_MODEL,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    private void renderCarriedItem(
            MechanicalTransferArmBlockEntity arm,
            ItemStack carried,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        ItemStack display =
                carried.copy();
        display.setCount(1);

        poseStack.pushPose();

        poseStack.translate(
                0.5D,
                25.9D / 16.0D,
                0.5D
        );

        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        90.0F
                )
        );

        poseStack.scale(
                0.22F,
                0.22F,
                0.22F
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
                        arm.getLevel(),
                        710
                );

        poseStack.popPose();
    }

    private void renderFilter(
            MechanicalTransferArmBlockEntity arm,
            Direction facing,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        ItemStack filter =
                arm.getFilterStack();

        if (filter.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        rotateAroundY(
                poseStack,
                0.5D,
                0.5D,
                yawFor(facing)
        );

        // One dedicated filter bay sits below the 2x2 direction grid. The
        // green/red underline communicates mode without layering a pile of
        // nearly-coplanar decorative bars on the panel.
        renderModel(
                arm.isBlacklist()
                        ? FILTER_DENY_MODEL
                        : FILTER_ALLOW_MODEL,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        ItemStack display =
                filter.copy();
        display.setCount(1);

        poseStack.pushPose();

        // Visually this is the right-hand half of the filter bay when viewed
        // from the operator side. Local model X is mirrored from that view.
        poseStack.translate(
                5.45D / 16.0D,
                9.05D / 16.0D,
                -1.40D / 16.0D
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        180.0F
                )
        );

        poseStack.scale(
                0.38F,
                0.38F,
                0.38F
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
                        arm.getLevel(),
                        711
                );

        poseStack.popPose();
        poseStack.popPose();
    }

    private float getArmYaw(
            MechanicalTransferArmBlockEntity arm,
            float progress) {
        Direction source =
                arm.getSourceSide();
        Direction destination =
                arm.getDestinationSide();

        if (source == null) {
            BlockState state =
                    arm.getBlockState();

            if (state.hasProperty(
                    net.minecraft.world.level.block.DirectionalBlock.FACING
            )) {
                source =
                        state.getValue(
                                net.minecraft.world.level.block.DirectionalBlock.FACING
                        );
            } else {
                source =
                        Direction.NORTH;
            }
        }

        if (destination == null) {
            destination = source;
        }

        boolean outbound =
                progress <= 0.5F;
        float legProgress =
                outbound
                        ? progress * 2.0F
                        : (progress - 0.5F) * 2.0F;

        Direction start =
                outbound
                        ? source
                        : destination;
        Direction end =
                outbound
                        ? destination
                        : source;

        // Rotation happens only while the claw is in the raised travel pose.
        float rotateStart =
                0.22F;
        float rotateEnd =
                0.78F;

        if (legProgress <= rotateStart) {
            return yawFor(start);
        }

        if (legProgress >= rotateEnd) {
            return yawFor(end);
        }

        float t =
                smoothStep(
                        (legProgress - rotateStart)
                                / (rotateEnd - rotateStart)
                );

        return lerpAngle(
                yawFor(start),
                yawFor(end),
                t
        );
    }

    private float endpointShoulder(
            boolean high) {
        // HIGH reaches beside the upper arm block. LOW reaches down beside the
        // controller/base block.
        return high
                ? -24.0F
                : -48.0F;
    }

    private float endpointElbow(
            boolean high) {
        return high
                ? -48.0F
                : -72.0F;
    }

    private float smoothStep(
            float value) {
        float t =
                Mth.clamp(
                        value,
                        0.0F,
                        1.0F
                );

        return t * t
                * (3.0F - 2.0F * t);
    }

    private float lerpAngle(
            float from,
            float to,
            float t) {
        float difference =
                Mth.wrapDegrees(
                        to - from
                );

        return from
                + difference * t;
    }

    private float yawFor(
            Direction direction) {
        // PoseStack's positive Y rotation is opposite the blockstate JSON
        // convention used by the static model. The arm models are authored
        // facing NORTH, so EAST must rotate -90 and WEST +90. NORTH/SOUTH hid
        // this mismatch because 0/180 degrees are identical either way.
        return switch (direction) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    private void renderControlButton(
            ResourceLocation model,
            Direction facing,
            Direction configuredSide,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        poseStack.pushPose();

        rotateAroundY(
                poseStack,
                0.5D,
                0.5D,
                yawFor(facing)
        );

        // Clean 2x2 operator grid when viewed from the panel:
        //
        //   F | B
        //   L | R
        //
        // Local model X appears mirrored from the operator side, so the
        // visually-left column uses positive X here.
        double xOffset;
        double yOffset;

        if (configuredSide == facing) {
            xOffset = 2.80D / 16.0D;
            yOffset = 0.0D;
        } else if (configuredSide == facing.getOpposite()) {
            xOffset = -2.80D / 16.0D;
            yOffset = 0.0D;
        } else if (configuredSide == facing.getClockWise()) {
            xOffset = 2.80D / 16.0D;
            yOffset = -2.05D / 16.0D;
        } else {
            xOffset = -2.80D / 16.0D;
            yOffset = -2.05D / 16.0D;
        }

        poseStack.translate(
                xOffset,
                yOffset,
                0.0D
        );

        renderModel(
                model,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    private void renderPanelReadout(
            MechanicalTransferArmBlockEntity arm,
            Direction facing,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        poseStack.pushPose();

        rotateAroundY(
                poseStack,
                0.5D,
                0.5D,
                yawFor(facing)
        );

        Direction source =
                arm.getSourceSide();
        Direction destination =
                arm.getDestinationSide();

        renderPanelButtonText(
                arm,
                "F",
                facing,
                source,
                destination,
                10.80D / 16.0D,
                13.92D / 16.0D,
                poseStack,
                buffer,
                packedLight
        );
        renderPanelButtonText(
                arm,
                "B",
                facing.getOpposite(),
                source,
                destination,
                5.20D / 16.0D,
                13.92D / 16.0D,
                poseStack,
                buffer,
                packedLight
        );
        renderPanelButtonText(
                arm,
                "L",
                facing.getClockWise(),
                source,
                destination,
                10.80D / 16.0D,
                11.87D / 16.0D,
                poseStack,
                buffer,
                packedLight
        );
        renderPanelButtonText(
                arm,
                "R",
                facing.getCounterClockWise(),
                source,
                destination,
                5.20D / 16.0D,
                11.87D / 16.0D,
                poseStack,
                buffer,
                packedLight
        );

        ItemStack filter =
                arm.getFilterStack();

        String filterMode;
        int filterColor;

        if (filter.isEmpty()) {
            filterMode = "ANY";
            filterColor = 0xFFE0D5B8;
        } else if (arm.isBlacklist()) {
            filterMode = "BLOCK";
            filterColor = 0xFFE06060;
        } else {
            filterMode = "ALLOW";
            filterColor = 0xFF70D870;
        }

        renderFlatPanelText(
                filterMode,
                10.55D / 16.0D,
                8.95D / 16.0D,
                filterColor,
                0.0040F,
                poseStack,
                buffer,
                packedLight
        );

        poseStack.popPose();
    }

    private void renderPanelButtonText(
            MechanicalTransferArmBlockEntity arm,
            String idleLabel,
            Direction side,
            Direction source,
            Direction destination,
            double x,
            double y,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        String label =
                idleLabel;
        int color =
                0xFFE0D5B8;

        if (side == source) {
            label = "IN";
            color = 0xFFF09A3E;
        } else if (side == destination) {
            label = "OUT";
            color = 0xFF62A9FF;
        }

        renderFlatPanelText(
                label,
                x,
                y + (label.length() > 1
                        ? 0.20D / 16.0D
                        : 0.0D),
                color,
                label.length() > 1
                        ? 0.0042F
                        : 0.0052F,
                poseStack,
                buffer,
                packedLight
        );

        if (side == source
                || side == destination) {
            renderFlatPanelText(
                    arm.isHigh(side)
                            ? "H"
                            : "L",
                    x,
                    y - 0.70D / 16.0D,
                    color,
                    0.0030F,
                    poseStack,
                    buffer,
                    packedLight
            );
        }
    }

    private void renderFlatPanelText(
            String text,
            double x,
            double y,
            int color,
            float scale,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        Font font =
                Minecraft.getInstance().font;

        poseStack.pushPose();
        poseStack.translate(
                x,
                y,
                -1.36D / 16.0D
        );
        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        180.0F
                )
        );
        poseStack.scale(
                scale,
                -scale,
                scale
        );

        float width =
                font.width(text);

        font.drawInBatch(
                text,
                -width / 2.0F,
                -font.lineHeight / 2.0F,
                color,
                false,
                poseStack.last().pose(),
                buffer,
                Font.DisplayMode.NORMAL,
                0,
                packedLight
        );

        poseStack.popPose();
    }

    private void rotateAroundY(
            PoseStack poseStack,
            double pivotX,
            double pivotZ,
            float degrees) {
        if (Math.abs(degrees) < 0.001F) {
            return;
        }

        poseStack.translate(
                pivotX,
                0.0D,
                pivotZ
        );
        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        degrees
                )
        );
        poseStack.translate(
                -pivotX,
                0.0D,
                -pivotZ
        );
    }

    private void rotateAroundX(
            PoseStack poseStack,
            double pivotX,
            double pivotY,
            double pivotZ,
            float degrees) {
        if (Math.abs(degrees) < 0.001F) {
            return;
        }

        poseStack.translate(
                pivotX,
                pivotY,
                pivotZ
        );
        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        degrees
                )
        );
        poseStack.translate(
                -pivotX,
                -pivotY,
                -pivotZ
        );
    }

    private void renderModel(
            ResourceLocation location,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        List<BakedQuad> quads =
                quadCache.computeIfAbsent(
                        location,
                        modelLocation -> {
                            BakedModel model =
                                    Minecraft.getInstance()
                                            .getModelManager()
                                            .getModel(
                                                    modelLocation
                                            );

                            if (model == null) {
                                return List.of();
                            }

                            return List.copyOf(
                                    model.getQuads(
                                            null,
                                            null,
                                            RandomSource.create(
                                                    0L
                                            )
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
}
