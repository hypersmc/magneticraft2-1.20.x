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
        ItemRenderer renderer=Minecraft.getInstance().getItemRenderer();
        ItemStack mounted=lathe.getOutput().isEmpty()?lathe.getWorkpiece():lathe.getOutput();
        // The arbor and static chuck clamp this casting at y=22, z=-10
        // in the canonical model. Its center is aligned to the spindle X axis.
        if(!mounted.isEmpty()){
            pose.pushPose();
            // Chuck jaws end at x=-1 model pixel; part center sits 3 pixels
            // beyond the jaw face, and stays on the real (y=22,z=-10) axis.
            pose.translate(3D/16D,22D/16D,-10D/16D);
            // Item model is cast horizontally; stand it up on the lathe's X axis.
            pose.mulPose(Axis.ZP.rotationDegrees(90F));
            if(lathe.isRunning())pose.mulPose(Axis.YP.rotationDegrees(lathe.getSpindleRotation(partial)));
            pose.scale(.52F,.52F,.52F);
            BakedModel model=renderer.getModel(mounted,lathe.getLevel(),null,883);
            renderer.render(mounted,ItemDisplayContext.NONE,false,pose,source,light,overlay,model);
            pose.popPose();
        }
        // Cutting insert mounted on the static tool post: a short approach
        // from the front, controlled traverse while touching the bronze,
        // then a visible retraction. Idle and stalled machines never advance.
        ItemStack bit=lathe.getCutter();
        if(!bit.isEmpty()){
            float feed=lathe.getToolFeed();
            float traverse=lathe.getCutTraverse();
            pose.pushPose();
            // The tool starts at the saddle (x=10,z=-2), moves towards
            // the spindle radially, then traverses LEFT along the mounted
            // casting. It must never travel toward the tailstock instead.
            double x=(10D - 6D*traverse)/16D;
            double z=(-2D - 6D*feed)/16D;
            pose.translate(x,24D/16D,z);
            pose.mulPose(Axis.XP.rotationDegrees(-90F));
            pose.scale(.28F,.28F,.28F);
            BakedModel model=renderer.getModel(bit,lathe.getLevel(),null,884);
            renderer.render(bit,ItemDisplayContext.NONE,false,pose,source,light,overlay,model);
            pose.popPose();
        }
        pose.popPose();
    }
}
