package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeMechanicalLatheBlock;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeMechanicalLatheBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Only the moving workpiece and installed cutter. Static Lathe geometry is baked into V2 parts. */
public final class BronzeMechanicalLatheRenderer
        implements BlockEntityRenderer<BronzeMechanicalLatheBlockEntity> {
    public BronzeMechanicalLatheRenderer(BlockEntityRendererProvider.Context ignored){}
    @Override public int getViewDistance(){return 64;}
    @Override public void render(BronzeMechanicalLatheBlockEntity lathe,float partial,
            PoseStack pose,MultiBufferSource source,int light,int overlay){
        if(!lathe.isFormed())return;
        BlockState state=lathe.getBlockState();
        Direction facing=state.getValue(BronzeMechanicalLatheBlock.FACING);
        float yaw=switch(facing){
            case WEST->-90F;case NORTH->180F;case EAST->90F;default->0F;
        };
        pose.pushPose();
        pose.translate(.5D,0,.5D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-.5D,0,-.5D);
        ItemStack stack=lathe.getOutput().isEmpty()?lathe.getWorkpiece():lathe.getOutput();
        if(!stack.isEmpty()){
            pose.pushPose();
            pose.translate(.5D,1.375D,-.625D);
            pose.mulPose(Axis.ZP.rotationDegrees(90F));
            if(lathe.isRunning()) pose.mulPose(Axis.YP.rotationDegrees(lathe.getSpindleRotation(partial)));
            pose.scale(.8F,.8F,.8F);
            ItemRenderer renderer=Minecraft.getInstance().getItemRenderer();
            BakedModel model=renderer.getModel(stack,lathe.getLevel(),null,883);
            renderer.render(stack,ItemDisplayContext.NONE,false,pose,source,light,overlay,model);
            pose.popPose();
        }
        if(!lathe.getCutter().isEmpty()){
            pose.pushPose();
            pose.translate(.625D,1.53D,-.22D - .09D*lathe.getProgress());
            pose.scale(.55F,.55F,.55F);
            ItemRenderer renderer=Minecraft.getInstance().getItemRenderer();
            BakedModel model=renderer.getModel(lathe.getCutter(),lathe.getLevel(),null,884);
            renderer.render(lathe.getCutter(),ItemDisplayContext.NONE,false,pose,source,light,overlay,model);
            pose.popPose();
        }
        pose.popPose();
    }
}
