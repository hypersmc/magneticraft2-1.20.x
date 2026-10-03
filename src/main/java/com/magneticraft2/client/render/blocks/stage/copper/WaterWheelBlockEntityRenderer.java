package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.block.stage.copper.WaterWheelBlock;
import com.magneticraft2.common.block.stage.copper.WaterWheelFillerBlock;
import com.magneticraft2.common.blockentity.stage.copper.WaterWheelBlockEntity;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Rotates the complete water-wheel assembly from Gear V2's authoritative angular state.
 *
 * Small wheels render one normal block model. Large wheels are assembled from the center
 * controller plus eight normal-sized filler models. All nine pieces are rendered under
 * one shared rotation transform, avoiding oversized JSON elements/UV atlas bleeding.
 */
public class WaterWheelBlockEntityRenderer implements BlockEntityRenderer<WaterWheelBlockEntity> {
    public WaterWheelBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WaterWheelBlockEntity blockEntity,
                       float partialTicks,
                       PoseStack stack,
                       MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {
        if (!blockEntity.getBlockState().getValue(WaterWheelBlock.ACTIVE)) {
            return;
        }

        stack.pushPose();
        stack.translate(0.5D, 0.5D, 0.5D);

        // Gear V2's source direction is correct for the output network, but the wheel
        // model's positive visual rotation is the opposite convention. Invert only the
        // rendered wheel; do not alter the mechanical source direction.
        float rotation = -blockEntity.getSmoothWheelVisualRotationDegrees(partialTicks);
        Direction.Axis axis = blockEntity.getGearAxis();

        if (axis == Direction.Axis.X) {
            stack.mulPose(Axis.XP.rotationDegrees(rotation));
        } else {
            stack.mulPose(Axis.ZP.rotationDegrees(rotation));
        }

        stack.translate(-0.5D, -0.5D, -0.5D);

        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();

        blockRenderer.renderSingleBlock(
                blockEntity.getBlockState().setValue(WaterWheelBlock.ACTIVE, false),
                stack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        if (blockEntity.isLarge() && blockEntity.getLevel() != null) {
            BlockPos center = blockEntity.getBlockPos();

            for (BlockPos fillerPos : WaterWheelBlock.largeWheelFillerPositions(center, axis)) {
                BlockState fillerState = blockEntity.getLevel().getBlockState(fillerPos);
                if (!fillerState.is(BlockRegistry.WATER_WHEEL_FILLER.get())) {
                    continue;
                }

                stack.pushPose();
                stack.translate(
                        fillerPos.getX() - center.getX(),
                        fillerPos.getY() - center.getY(),
                        fillerPos.getZ() - center.getZ()
                );

                blockRenderer.renderSingleBlock(
                        fillerState.setValue(WaterWheelFillerBlock.ACTIVE, false),
                        stack,
                        bufferSource,
                        packedLight,
                        packedOverlay
                );

                stack.popPose();
            }
        }

        stack.popPose();
    }
}
