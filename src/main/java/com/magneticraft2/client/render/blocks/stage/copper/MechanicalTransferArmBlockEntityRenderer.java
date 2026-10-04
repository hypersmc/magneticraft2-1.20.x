package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.MechanicalTransferArmBlockEntity;
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
 * The static baked block is only the heavy plinth. Everything that mechanically
 * moves is drawn here:
 *
 *   base gear -> yawing turret -> shoulder -> elbow/forearm -> wrist -> claw
 *
 * The arm raises/retracts during the middle of each sweep, then reaches back
 * down at the source/destination. This makes it read as a crude geared robot
 * rather than one rigid crane boom rotating through inventories.
 */
public class MechanicalTransferArmBlockEntityRenderer
        implements BlockEntityRenderer<MechanicalTransferArmBlockEntity> {

    private static final ResourceLocation GEAR_MODEL =
            model("mechanical_transfer_arm_gear");
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

    // Authored model pivots in block-local coordinates.
    private static final double SHOULDER_Y =
            11.0D / 16.0D;
    private static final double ELBOW_Y =
            20.0D / 16.0D;
    private static final double WRIST_Y =
            30.5D / 16.0D;

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

        if (source != null) {
            renderDirectionalModel(
                    ORANGE_BUTTON_MODEL,
                    source,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay
            );
        }

        if (destination != null) {
            renderDirectionalModel(
                    BLUE_BUTTON_MODEL,
                    destination,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay
            );
        }

        renderFilter(
                arm,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
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

        // 0 at source/destination, 1 in the middle of the sweep.
        float lift =
                (float) Math.sin(
                        Math.PI
                                * Mth.clamp(
                                legProgress,
                                0.0F,
                                1.0F
                        )
                );

        // Endpoint pose totals exactly -90 degrees so the forearm reaches
        // horizontally into the neighbouring inventory. Mid-sweep it folds up
        // and retracts so the claw visibly clears the machine before turning.
        float shoulderPitch =
                Mth.lerp(
                        lift,
                        -28.0F,
                        -12.0F
                );
        float elbowPitch =
                Mth.lerp(
                        lift,
                        -62.0F,
                        -43.0F
                );

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
                22.1D / 16.0D,
                0.5D
        );

        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        90.0F
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
                        710
                );

        poseStack.popPose();
    }

    private void renderFilter(
            MechanicalTransferArmBlockEntity arm,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        ItemStack filter =
                arm.getFilterStack();

        if (filter.isEmpty()) {
            return;
        }

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

        // The rotating base gear has an open center specifically so this ghost
        // filter stays readable while the drivetrain spins around it.
        poseStack.translate(
                0.5D,
                6.35D / 16.0D,
                0.5D
        );

        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        90.0F
                )
        );

        poseStack.scale(
                0.26F,
                0.26F,
                0.26F
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

        if (progress <= 0.5F) {
            float t =
                    smoothStep(
                            progress * 2.0F
                    );

            return lerpAngle(
                    yawFor(source),
                    yawFor(destination),
                    t
            );
        }

        float t =
                smoothStep(
                        (progress - 0.5F)
                                * 2.0F
                );

        return lerpAngle(
                yawFor(destination),
                yawFor(source),
                t
        );
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
        return switch (direction) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> -90.0F;
            default -> 0.0F;
        };
    }

    private void renderDirectionalModel(
            ResourceLocation model,
            Direction direction,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        poseStack.pushPose();

        rotateAroundY(
                poseStack,
                0.5D,
                0.5D,
                yawFor(direction)
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
