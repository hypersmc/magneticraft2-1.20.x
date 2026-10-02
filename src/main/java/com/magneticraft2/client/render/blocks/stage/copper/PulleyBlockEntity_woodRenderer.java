package com.magneticraft2.client.render.blocks.stage.copper;

import com.magneticraft2.common.blockentity.stage.copper.PulleyBlockEntity_wood;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import static com.magneticraft2.common.block.stage.copper.PulleyBlock_wood.POWERED;
import static net.minecraft.world.level.block.DirectionalBlock.FACING;

/**
 * Dynamic wooden pulley + straight open-belt renderer.
 *
 * The belt is deliberately rendered as a compact leather band rather than as
 * placed blocks. One endpoint renders each connection to avoid duplicates.
 */
public class PulleyBlockEntity_woodRenderer implements BlockEntityRenderer<PulleyBlockEntity_wood> {
    public PulleyBlockEntity_woodRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PulleyBlockEntity_wood blockEntity,
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
                    stack,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );
            stack.popPose();
        }

        renderBelt(blockEntity, stack, bufferSource);
    }

    private void applyPulleyRotation(PulleyBlockEntity_wood blockEntity,
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

    private void renderBelt(PulleyBlockEntity_wood blockEntity,
                            PoseStack stack,
                            MultiBufferSource bufferSource) {
        BlockPos partnerPos = blockEntity.getBeltPartner();
        if (partnerPos == null || blockEntity.getLevel() == null) {
            return;
        }

        // Render once from the deterministically lower endpoint.
        if (blockEntity.getBlockPos().asLong() > partnerPos.asLong()) {
            return;
        }

        if (!(blockEntity.getLevel().getBlockEntity(partnerPos) instanceof PulleyBlockEntity_wood partner)
                || !partner.isLinkedTo(blockEntity.getBlockPos())
                || partner.getGearAxis() != blockEntity.getGearAxis()) {
            return;
        }

        Vec3 startCenter = new Vec3(0.5D, 0.5D, 0.5D);
        BlockPos delta = partnerPos.subtract(blockEntity.getBlockPos());
        Vec3 endCenter = new Vec3(delta.getX() + 0.5D, delta.getY() + 0.5D, delta.getZ() + 0.5D);

        Vec3 line = endCenter.subtract(startCenter);
        if (line.lengthSqr() < 0.0001D) {
            return;
        }

        Vec3 axis = switch (blockEntity.getGearAxis()) {
            case X -> new Vec3(1.0D, 0.0D, 0.0D);
            case Y -> new Vec3(0.0D, 1.0D, 0.0D);
            case Z -> new Vec3(0.0D, 0.0D, 1.0D);
        };

        Vec3 side = axis.cross(line.normalize());
        if (side.lengthSqr() < 0.0001D) {
            return;
        }
        side = side.normalize();

        double startRadius = blockEntity.getPulleyRadius();
        double endRadius = partner.getPulleyRadius();

        Vec3 startA = startCenter.add(side.scale(startRadius));
        Vec3 endA = endCenter.add(side.scale(endRadius));
        Vec3 startB = startCenter.subtract(side.scale(startRadius));
        Vec3 endB = endCenter.subtract(side.scale(endRadius));

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        PoseStack.Pose pose = stack.last();

        // Three close parallel lines per run give the belt readable thickness without
        // requiring a dedicated animated belt texture yet.
        for (double thickness : new double[]{-0.025D, 0.0D, 0.025D}) {
            Vec3 thicknessOffset = axis.scale(thickness);
            drawLeatherLine(consumer, pose,
                    startA.add(thicknessOffset),
                    endA.add(thicknessOffset));
            drawLeatherLine(consumer, pose,
                    startB.add(thicknessOffset),
                    endB.add(thicknessOffset));
        }
    }

    private void drawLeatherLine(VertexConsumer consumer,
                                 PoseStack.Pose pose,
                                 Vec3 from,
                                 Vec3 to) {
        float r = 0.30F;
        float g = 0.14F;
        float b = 0.055F;
        float a = 1.0F;

        consumer.vertex(pose.pose(), (float) from.x, (float) from.y, (float) from.z)
                .color(r, g, b, a)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
        consumer.vertex(pose.pose(), (float) to.x, (float) to.y, (float) to.z)
                .color(r, g, b, a)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
