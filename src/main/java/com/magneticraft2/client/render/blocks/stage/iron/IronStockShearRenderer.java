package com.magneticraft2.client.render.blocks.stage.iron;

import com.magneticraft2.common.block.stage.iron.IronStockShearBlock;
import com.magneticraft2.common.blockentity.stage.iron.IronStockShearBlockEntity;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Only the gear-driven shear blade and workpiece are dynamic; V2 owns the frame. */
public final class IronStockShearRenderer
        implements BlockEntityRenderer<IronStockShearBlockEntity> {
    private static final ResourceLocation RAM =
            new ResourceLocation("magneticraft2", "multiblock/iron_stock_shear_blade");

    public IronStockShearRenderer(BlockEntityRendererProvider.Context ignored) {}

    @Override public int getViewDistance() { return 64; }

    @Override public void render(IronStockShearBlockEntity shear, float partial,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!shear.isFormed()) return;
        Direction facing = shear.getBlockState().getValue(IronStockShearBlock.FACING);
        float yaw = switch (facing) {
            case WEST -> -90F;
            case NORTH -> 180F;
            case EAST -> 90F;
            default -> 0F;
        };
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-.5D, 0D, -.5D);

        // One mechanically guided cam-driven vertical stroke. The model is registered
        // at resource load and remains fixed across frames; no voxel rebuild.
        pose.pushPose();
        pose.translate(0D, (16D - 6D * shear.getShearStroke(partial)) / 16D, -1D);
        BakedModel ram = Minecraft.getInstance().getModelManager().getModel(RAM);
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        dispatcher.getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.cutout()),
                BlockRegistry.IRON_STOCK_SHEAR.get().defaultBlockState(),
                ram, 1F, 1F, 1F, light, overlay);
        pose.popPose();

        ItemStack workpiece = shear.getWorkpiece();
        if (!workpiece.isEmpty()) {
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            pose.pushPose();
            // The center of the actual 3x3x2 shear table, not the controller or ports.
            pose.translate(8D/16D, 8.5D/16D, -9D/16D);
            pose.mulPose(Axis.XP.rotationDegrees(90F));
            pose.scale(.38F,.38F,.38F);
            itemRenderer.render(workpiece, ItemDisplayContext.NONE, false,
                    pose, buffers, light, overlay,
                    itemRenderer.getModel(workpiece, shear.getLevel(), null, 889));
            pose.popPose();
        }
        pose.popPose();
    }
}
