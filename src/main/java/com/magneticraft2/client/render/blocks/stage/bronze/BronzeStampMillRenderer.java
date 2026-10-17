package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeStampMillBlock;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeStampMillBlockEntity;
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
 * Three eccentric stamp heads, one shared Gear V2 phase, no per-tick baked
 * V2 geometry regeneration. World/guide frame is rendered by V2 fillers.
 */
public final class BronzeStampMillRenderer implements BlockEntityRenderer<BronzeStampMillBlockEntity> {
    private static final ResourceLocation HAMMER =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_hammer");
    private static final ResourceLocation MODULE =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_input_frame");
    private static final ResourceLocation CAM =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_cam");

    public BronzeStampMillRenderer(BlockEntityRendererProvider.Context context) {}
    private static void renderModel(PoseStack pose,MultiBufferSource buffers,int light,int overlay,BakedModel model) {
        BlockRenderDispatcher blocks=Minecraft.getInstance().getBlockRenderer();
        blocks.getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.cutout()),
                BlockRegistry.BRONZE_STAMP_MILL.get().defaultBlockState(),model,
                1F,1F,1F,light,overlay);
    }
    @Override public int getViewDistance(){return 64;}
    @Override public void render(BronzeStampMillBlockEntity mill,float partial,PoseStack pose,
                                 MultiBufferSource buffers,int light,int overlay) {
        if(!mill.isFormed())return;
        BlockState state=mill.getBlockState();
        Direction facing=state.getValue(BronzeStampMillBlock.FACING);
        float yaw=switch(facing) {
            case WEST -> -90F;case NORTH -> 180F;case EAST -> 90F;default -> 0F;
        };
        pose.pushPose();
        pose.translate(.5D,0D,.5D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-.5D,0D,-.5D);

        // The actual formed input module at (0,0,-2) is not a filler.
        // Render a compact bearing casing right over its hidden block state.
        pose.pushPose();
        pose.translate(0D,0D,-2D);
        renderModel(pose,buffers,light,overlay,
                Minecraft.getInstance().getModelManager().getModel(MODULE));
        pose.popPose();

        float shaftAngle=mill.getDriveRotationDegrees(partial);
        BakedModel stamp=Minecraft.getInstance().getModelManager().getModel(HAMMER);
        BakedModel cam=Minecraft.getInstance().getModelManager().getModel(CAM);
        int[] stations={-14,8,30};
        for(int i=0;i<stations.length;i++) {
            // Each stamp head slides vertically between two fixed guide rails.
            // Its shoe reaches the trough but cannot go through the floor.
            pose.pushPose();
            pose.translate((stations[i]-8D)/16D,
                    14D/16D+mill.getHammerLift(partial,i),
                    (-10D-8D)/16D);
            renderModel(pose,buffers,light,overlay,stamp);
            pose.popPose();

            // One crank per stamp station; phase stagger matches hammer travel.
            pose.pushPose();
            pose.translate(stations[i]/16D,38.5D/16D,-10D/16D);
            pose.mulPose(Axis.XP.rotationDegrees(shaftAngle+120F*i));
            pose.translate(-.5D,-.5D,-.5D);
            renderModel(pose,buffers,light,overlay,cam);
            pose.popPose();
        }
        ItemRenderer items=Minecraft.getInstance().getItemRenderer();
        ItemStack feed=mill.getInputStack();
        if(!feed.isEmpty()){
            pose.pushPose();
            pose.translate(.5D,18D/16D,-10D/16D);
            pose.scale(.55F,.55F,.55F);
            BakedModel model=items.getModel(feed,mill.getLevel(),null,721);
            items.render(feed,ItemDisplayContext.GROUND,false,pose,buffers,light,overlay,model);
            pose.popPose();
        }
        pose.popPose();
    }
}
