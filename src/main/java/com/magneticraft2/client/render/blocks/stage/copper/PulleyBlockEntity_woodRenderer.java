package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.PulleyBlockEntity_wood;
import com.magneticraft2.common.systems.GEAR.BeltConnectionManager;
import com.magneticraft2.common.systems.GEAR.BeltPath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import static com.magneticraft2.common.block.stage.copper.PulleyBlock_wood.POWERED;
import static net.minecraft.world.level.block.DirectionalBlock.FACING;

public class PulleyBlockEntity_woodRenderer
        implements BlockEntityRenderer<PulleyBlockEntity_wood> {

    public PulleyBlockEntity_woodRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            PulleyBlockEntity_wood blockEntity,
            float partialTicks,
            PoseStack stack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        if (blockEntity.getBlockState().getValue(POWERED)) {
            stack.pushPose();
            stack.translate(0.5D, 0.5D, 0.5D);
            applyPulleyRotation(blockEntity, partialTicks, stack);
            stack.translate(-0.5D, -0.5D, -0.5D);

            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    blockEntity.getBlockState().setValue(POWERED, false),
                    stack, bufferSource, packedLight, packedOverlay
            );
            stack.popPose();
        }

        renderBelt(blockEntity, partialTicks, stack, bufferSource, packedLight);
    }

    private void applyPulleyRotation(
            PulleyBlockEntity_wood blockEntity,
            float partialTicks,
            PoseStack stack) {
        float rotation = blockEntity.getVisualRotationDegrees(partialTicks);
        Direction facing = blockEntity.getBlockState().getValue(FACING);

        if (facing.getAxis() == Direction.Axis.Y) {
            stack.mulPose(Axis.YP.rotationDegrees(rotation));
        } else if (facing.getAxis() == Direction.Axis.X) {
            stack.mulPose(Axis.XP.rotationDegrees(rotation));
        } else {
            stack.mulPose(Axis.ZP.rotationDegrees(rotation));
        }
    }

    private void renderBelt(
            PulleyBlockEntity_wood blockEntity,
            float partialTicks,
            PoseStack stack,
            MultiBufferSource bufferSource,
            int packedLight) {
        BlockPos partnerPos = blockEntity.getBeltPartner();
        if (partnerPos == null || blockEntity.getLevel() == null) {
            return;
        }

        BeltConnectionManager.ensureRegistered(blockEntity);
        BeltPath path = BeltConnectionManager.getPath(
                blockEntity.getLevel(),
                blockEntity.getBlockPos(),
                partnerPos
        );

        if (path == null
                || !path.startPulley().equals(blockEntity.getBlockPos())) {
            return;
        }

        Vec3 renderOrigin = new Vec3(
                blockEntity.getBlockPos().getX(),
                blockEntity.getBlockPos().getY(),
                blockEntity.getBlockPos().getZ()
        );

        LeatherBeltRenderHelper.renderSegments(
                path.segments(),
                stack,
                bufferSource,
                packedLight,
                renderOrigin,
                -blockEntity.getVisualBeltTravelDistance(partialTicks)
        );
    }
}
