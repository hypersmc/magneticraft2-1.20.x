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
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MechanicalTransferArmBlockEntityRenderer
        implements BlockEntityRenderer<MechanicalTransferArmBlockEntity> {

    private static final ResourceLocation GEAR_MODEL =
            new ResourceLocation(
                    "magneticraft2",
                    "block/mechanical_transfer_arm_gear"
            );
    private static final ResourceLocation ARM_MODEL =
            new ResourceLocation(
                    "magneticraft2",
                    "block/mechanical_transfer_arm_arm"
            );
    private static final ResourceLocation ORANGE_BUTTON_MODEL =
            new ResourceLocation(
                    "magneticraft2",
                    "block/mechanical_transfer_arm_button_orange"
            );
    private static final ResourceLocation BLUE_BUTTON_MODEL =
            new ResourceLocation(
                    "magneticraft2",
                    "block/mechanical_transfer_arm_button_blue"
            );
    private static final ResourceLocation FILTER_ALLOW_MODEL =
            new ResourceLocation(
                    "magneticraft2",
                    "block/mechanical_transfer_arm_filter_allow"
            );
    private static final ResourceLocation FILTER_DENY_MODEL =
            new ResourceLocation(
                    "magneticraft2",
                    "block/mechanical_transfer_arm_filter_deny"
            );

    private final Map<ResourceLocation, List<BakedQuad>> quadCache =
            new HashMap<>();

    public MechanicalTransferArmBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
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

        float armYaw =
                getArmYaw(
                        arm,
                        partialTicks
                );

        poseStack.pushPose();
        poseStack.translate(
                0.5D,
                0.0D,
                0.5D
        );
        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        armYaw
                )
        );
        poseStack.translate(
                -0.5D,
                0.0D,
                -0.5D
        );

        renderModel(
                ARM_MODEL,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );

        ItemStack carried =
                arm.getCarriedStack();

        if (!carried.isEmpty()) {
            ItemStack display =
                    carried.copy();
            display.setCount(1);

            poseStack.pushPose();
            poseStack.translate(
                    0.5D,
                    0.72D,
                    -0.64D
            );
            poseStack.mulPose(
                    Axis.XP.rotationDegrees(
                            90.0F
                    )
            );
            poseStack.scale(
                    0.34F,
                    0.34F,
                    0.34F
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
        poseStack.translate(
                0.5D,
                0.0D,
                0.5D
        );
        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        arm.getVisualRotationDegrees(
                                partialTicks
                        )
                )
        );
        poseStack.translate(
                -0.5D,
                0.0D,
                -0.5D
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

        Direction facing =
                Direction.NORTH;
        BlockState state =
                arm.getBlockState();

        if (state.hasProperty(
                DirectionalBlock.FACING
        )
                && state.getValue(
                DirectionalBlock.FACING
        ).getAxis().isHorizontal()) {
            facing =
                    state.getValue(
                            DirectionalBlock.FACING
                    );
        }

        poseStack.pushPose();
        rotateFromNorth(
                poseStack,
                facing
        );

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
        poseStack.translate(
                0.5D,
                0.355D,
                0.205D
        );
        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        90.0F
                )
        );
        poseStack.scale(
                0.24F,
                0.24F,
                0.24F
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
            float partialTicks) {
        Direction source =
                arm.getSourceSide();
        Direction destination =
                arm.getDestinationSide();

        if (source == null) {
            BlockState state =
                    arm.getBlockState();

            if (state.hasProperty(
                    DirectionalBlock.FACING
            )) {
                source =
                        state.getValue(
                                DirectionalBlock.FACING
                        );
            } else {
                source = Direction.NORTH;
            }
        }

        if (destination == null) {
            destination = source;
        }

        float progress =
                arm.getVisualCycleProgress(
                        partialTicks
                );

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
        rotateFromNorth(
                poseStack,
                direction
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

    private void rotateFromNorth(
            PoseStack poseStack,
            Direction direction) {
        float yaw =
                yawFor(direction);

        if (Math.abs(yaw) < 0.001F) {
            return;
        }

        poseStack.translate(
                0.5D,
                0.0D,
                0.5D
        );
        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        yaw
                )
        );
        poseStack.translate(
                -0.5D,
                0.0D,
                -0.5D
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
