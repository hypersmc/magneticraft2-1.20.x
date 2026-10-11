package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeMoldTableBlock;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeMoldTableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Render actual inventory items on the wood-and-sand table, without a GUI. */
public final class BronzeMoldTableRenderer
        implements BlockEntityRenderer<BronzeMoldTableBlockEntity> {
    public BronzeMoldTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BronzeMoldTableBlockEntity table, float partialTicks,
                       PoseStack pose, MultiBufferSource buffer, int light,
                       int overlay) {
        Direction facing = table.getBlockState().getValue(BronzeMoldTableBlock.FACING);
        float yaw = switch (facing) {
            case EAST -> 90F;
            case NORTH -> 180F;
            case WEST -> -90F;
            default -> 0F;
        };
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-0.5, 0, -0.5);

        ItemStack result = table.getDisplayStack(3);
        if (!result.isEmpty()) {
            item(table, result, 0.50, 0.86, 0.50, 0.64F, 410,
                    pose, buffer, light, overlay);
        } else {
            item(table, table.getDisplayStack(0), 0.34, 0.85, 0.41,
                    0.42F, 411, pose, buffer, light, overlay);
            item(table, table.getDisplayStack(1), 0.66, 0.85, 0.41,
                    0.40F, 412, pose, buffer, light, overlay);
            item(table, table.getDisplayStack(2), 0.50, 0.85, 0.67,
                    0.34F, 413, pose, buffer, light, overlay);
        }
        pose.popPose();
    }

    private void item(BronzeMoldTableBlockEntity table, ItemStack stack,
                      double x, double y, double z, float scale, int seed,
                      PoseStack pose, MultiBufferSource buffer, int light,
                      int overlay) {
        if (stack.isEmpty()) return;
        ItemStack display = stack.copy();
        display.setCount(1);
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.XP.rotationDegrees(90F));
        pose.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                display, ItemDisplayContext.NONE, light, overlay,
                pose, buffer, table.getLevel(), seed);
        pose.popPose();
    }
}
