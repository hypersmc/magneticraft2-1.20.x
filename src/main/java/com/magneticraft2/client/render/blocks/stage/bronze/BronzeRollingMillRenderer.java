package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeRollingMillBlock;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeRollingMillBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;

/**
 * Two identical baked rolls, rotating in opposing directions on the actual
 * horizontal Gear V2 shaft axis (local +Z). Geometry and collision remain
 * the pre-baked static multiblock model/voxel rather than animated entities.
 */
public final class BronzeRollingMillRenderer
        implements BlockEntityRenderer<BronzeRollingMillBlockEntity> {
    private static final ResourceLocation ROLLER =
            new ResourceLocation("magneticraft2", "multiblock/bronze_rolling_mill_roller");

    public BronzeRollingMillRenderer(BlockEntityRendererProvider.Context ignored) {}

    @Override public int getViewDistance() { return 64; }

    @Override public void render(BronzeRollingMillBlockEntity mill, float partial,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!mill.isFormed()) return;
        Direction facing = mill.getBlockState().getValue(BronzeRollingMillBlock.FACING);
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

        // Two distinct directions. One lower axle meets the real rear-facing
        // mechanical input; the upper is driven by the enclosed timing gears.
        // Render only TWO cached baked models, never reconstruct voxels.
        BakedModel roller = Minecraft.getInstance().getModelManager().getModel(ROLLER);
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BlockState state = BlockRegistry.BRONZE_ROLLING_MILL.get().defaultBlockState();
        float angle = mill.getRollerAngle(partial);

        renderRoller(pose, buffers, roller, dispatcher, state, angle, 0F, light, overlay);
        renderRoller(pose, buffers, roller, dispatcher, state, -angle, 14F, light, overlay);

        // Copper plates sit on the left feed tray; a future Studio visual
        // can replace this with an in/out travel animation without changing
        // gameplay inventory or adding item entities.
        ItemStack workpiece = mill.getWorkpiece();
        if (!workpiece.isEmpty()) {
            ItemRenderer items = Minecraft.getInstance().getItemRenderer();
            pose.pushPose();
            pose.translate(-.5D, 17.4D / 16D, -.5D);
            pose.mulPose(Axis.XP.rotationDegrees(90F));
            pose.scale(.37F, .37F, .37F);
            items.render(workpiece, ItemDisplayContext.NONE, false,
                    pose, buffers, light, overlay,
                    items.getModel(workpiece, mill.getLevel(), null, 994));
            pose.popPose();
        }
        pose.popPose();
    }

    private static void renderRoller(PoseStack pose, MultiBufferSource buffers,
            BakedModel roller, BlockRenderDispatcher dispatcher, BlockState state,
            float angle, float verticalOffsetPixels, int light, int overlay) {
        pose.pushPose();
        // Local shaft centers: x=8px, y=8/22px, z=-8px. The roller model
        // itself is in one 16px local cell, with its axis along local +Z.
        pose.translate(.5D, (8D + verticalOffsetPixels) / 16D, -.5D);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-.5D, -.5D, -.5D);
        dispatcher.getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.cutout()), state, roller,
                1F, 1F, 1F, light, overlay);
        pose.popPose();
    }
}
