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
    private static final ResourceLocation WORN_HAMMER =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_hammer_worn");
    private static final ResourceLocation MODULE =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_input_frame");
    private static final ResourceLocation CAM =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_cam");

    // Render-only powertrain, mechanically driven by the formed input module.
    // X shafts and Z input each have correctly aligned baked geometry.
    private static final ResourceLocation DRIVE_SHAFT =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_shaft");
    private static final ResourceLocation SHAFT_TIP =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_shaft_tip");
    private static final ResourceLocation INPUT_SHAFT =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_input_shaft");
    private static final ResourceLocation BEVEL_Z =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_bevel");
    private static final ResourceLocation BEVEL_X =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_bevel_x");
    private static final ResourceLocation PULLEY =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_pulley");
    private static final ResourceLocation BELT_STRAIGHT =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_belt_straight");
    private static final ResourceLocation BELT_UPPER =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_belt_upper");
    private static final ResourceLocation BELT_WRAP =
            new ResourceLocation("magneticraft2","multiblock/bronze_stamp_mill_drive_belt_wrap");

    private static void part(PoseStack pose,MultiBufferSource buffers,int light,int overlay,
                             ResourceLocation model,double x,double y,double z,
                             Axis axis,float degrees) {
        pose.pushPose();
        pose.translate(x,y,z);
        if(axis!=null)pose.mulPose(axis.rotationDegrees(degrees));
        pose.translate(-.5D,-.5D,-.5D);
        renderModel(pose,buffers,light,overlay,
                Minecraft.getInstance().getModelManager().getModel(model));
        pose.popPose();
    }

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
        // Model space is in controller-relative pixels. The Z input comes
        // from the real Gear V2 module (x=8,y=8,z=-24). Its visible shaft
        // runs along Z, through a bevel pair centered under the tray.
        // A true X-axis shaft then reaches the OUTSIDE of the right frame
        // at x=47.2px, so the belt never cuts through the ore trough.
        final double inputX=8D/16D, inputY=8D/16D;
        final double lowerZ=-10D/16D, upperY=38.5D/16D;
        final double pulleyX=47.2D/16D;
        // Opposing bevel rotation, then OPEN belt: both pulleys and the
        // crankshaft share the same positive phase as all three cams.
        final float inputAngle=-shaftAngle;
        part(pose,buffers,light,overlay,INPUT_SHAFT,
                inputX,inputY,-18D/16D,Axis.ZP,inputAngle);
        part(pose,buffers,light,overlay,BEVEL_Z,
                inputX,inputY,-12.5D/16D,Axis.ZP,inputAngle);
        part(pose,buffers,light,overlay,BEVEL_X,
                11.5D/16D,inputY,lowerZ,Axis.XP,shaftAngle);

        // The lower crossshaft is below the structural girder (Y=16px).
        // Segment endpoints meet exactly: [8,24], [24,40], [40,47.2].
        for(int x=16;x<=32;x+=16){
            part(pose,buffers,light,overlay,DRIVE_SHAFT,
                    x/16D,inputY,lowerZ,Axis.XP,shaftAngle);
        }
        part(pose,buffers,light,overlay,SHAFT_TIP,
                40D/16D,inputY,lowerZ,Axis.XP,shaftAngle);

        // Pulleys share an X axis and turn the SAME way as the top cams.
        // Straight belt flights and both half-wraps are stationary geometry,
        // not independently spinning rings. Heights join with no overlap:
        // 8..24px + 24..38.5px, and the pulley arcs cap the ends.
        part(pose,buffers,light,overlay,PULLEY,
                pulleyX,inputY,lowerZ,Axis.XP,shaftAngle);
        part(pose,buffers,light,overlay,PULLEY,
                pulleyX,upperY,lowerZ,Axis.XP,shaftAngle);
        part(pose,buffers,light,overlay,BELT_STRAIGHT,
                pulleyX,16D/16D,lowerZ,null,0F);
        part(pose,buffers,light,overlay,BELT_UPPER,
                pulleyX,32D/16D,lowerZ,null,0F);
        // The lower half-round uses the model's upper semicircle flipped.
        part(pose,buffers,light,overlay,BELT_WRAP,
                pulleyX,inputY,lowerZ,Axis.XP,180F);
        part(pose,buffers,light,overlay,BELT_WRAP,
                pulleyX,upperY,lowerZ,null,0F);

        // One continuous drive axis through all four fixed crank pillows.
        // Four 16px lengths [-24..40] and a short [40..47.2] end spindle.
        // Critically, this rotates +shaftAngle just like every eccentric;
        // the former -shaftAngle rendering was visibly backwards.
        for(int x=-16;x<=32;x+=16){
            part(pose,buffers,light,overlay,DRIVE_SHAFT,
                    x/16D,upperY,lowerZ,Axis.XP,shaftAngle);
        }
        part(pose,buffers,light,overlay,SHAFT_TIP,
                40D/16D,upperY,lowerZ,Axis.XP,shaftAngle);

        BakedModel stamp=Minecraft.getInstance().getModelManager().getModel(
                mill.getPartWearStage()>=2?WORN_HAMMER:HAMMER);
        BakedModel cam=Minecraft.getInstance().getModelManager().getModel(CAM);
        // Each hammer is centered over one complete 1x1 I/O module pocket.
        int[] stations={-8,8,24};
        for(int i=0;i<stations.length;i++) {
            // The narrow shoe fits BETWEEN the bronze trough lips and
            // rides inside both vertically anchored guide rails.
            pose.pushPose();
            pose.translate((stations[i]-8D)/16D,
                    24D/16D+mill.getHammerLift(partial,i),
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
            // Display incoming ore on top of the actual crushing plate,
            // not below the tray inside the support frame.
            pose.translate(.5D,24D/16D,-10D/16D);
            pose.scale(.55F,.55F,.55F);
            BakedModel model=items.getModel(feed,mill.getLevel(),null,721);
            items.render(feed,ItemDisplayContext.GROUND,false,pose,buffers,light,overlay,model);
            pose.popPose();
        }
        pose.popPose();
    }
}
