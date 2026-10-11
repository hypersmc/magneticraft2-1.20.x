package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeMechanicalLatheBlock;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeMechanicalLatheBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import com.magneticraft2.common.registry.registers.BlockRegistry;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Static power train is V2 baked; carriage, drive wheel and workpiece move together
 * with the actual Gear V2 machine state. */
public final class BronzeMechanicalLatheRenderer
        implements BlockEntityRenderer<BronzeMechanicalLatheBlockEntity> {
    private static final ResourceLocation CARRIAGE =
            new ResourceLocation("magneticraft2", "multiblock/bronze_mechanical_lathe_carriage");
    private static final ResourceLocation CHUCK =
            new ResourceLocation("magneticraft2", "multiblock/bronze_mechanical_lathe_chuck");
    public BronzeMechanicalLatheRenderer(BlockEntityRendererProvider.Context ignored){}

    private static void model(PoseStack pose,MultiBufferSource source,int light,int overlay,
                              BakedModel model) {
        BlockRenderDispatcher blocks=Minecraft.getInstance().getBlockRenderer();
        blocks.getModelRenderer().renderModel(pose.last(),source.getBuffer(RenderType.cutout()),
                BlockRegistry.BRONZE_MECHANICAL_LATHE.get().defaultBlockState(),
                model,1F,1F,1F,light,overlay);
    }
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
        float angle=lathe.getSpindleRotation(partial);
        ItemRenderer items=Minecraft.getInstance().getItemRenderer();

        // The rear shaft leads into the LEFT headstock. This wheel stays
        // mounted even when idle, and rotates only with the Gear V2 input.
        pose.pushPose();
        pose.translate(-23D/16D,18.5D/16D,-24.3D/16D);
        pose.mulPose(Axis.XP.rotationDegrees(angle));
        pose.scale(.65F,.65F,.65F);
        ItemStack driveGear=new ItemStack(BlockRegistry.GEAR_MEDIUM_WOOD.get());
        BakedModel driveModel=items.getModel(driveGear,lathe.getLevel(),null,991);
        items.render(driveGear,ItemDisplayContext.NONE,false,pose,source,light,overlay,driveModel);
        pose.popPose();

        // Spindle/chuck rotates as ONE assembly about the headstock X axis.
        // The chuck is no longer duplicated in the baked V2 structure.
        pose.pushPose();
        pose.translate(-4.5D/16D,22D/16D,-10D/16D);
        pose.mulPose(Axis.XP.rotationDegrees(angle));
        pose.translate(-.5D,-.5D,-.5D);
        model(pose,source,light,overlay,
                Minecraft.getInstance().getModelManager().getModel(CHUCK));
        pose.popPose();

        ItemStack mounted=lathe.getOutput().isEmpty()
                ?lathe.getWorkpiece():lathe.getOutput();
        if(!mounted.isEmpty()){
            pose.pushPose();
            pose.translate(3D/16D,22D/16D,-10D/16D);
            pose.mulPose(Axis.XP.rotationDegrees(angle));
            pose.mulPose(Axis.ZP.rotationDegrees(90F));
            pose.scale(.52F,.52F,.52F);
            BakedModel workModel=items.getModel(mounted,lathe.getLevel(),null,883);
            items.render(mounted,ItemDisplayContext.NONE,false,pose,source,light,overlay,workModel);
            pose.popPose();
        }

        // One transform drives BOTH carriage and installed cutter. The saddle
        // first feeds radially toward the spindle, then traverses left, and
        // retracts without leaving a separate floating tool behind.
        float feed=lathe.getToolFeed();
        float traverse=lathe.getCutTraverse();
        pose.pushPose();
        pose.translate((2D-6D*traverse)/16D,17D/16D,
                (-6D-3D*feed)/16D);
        BakedModel carriage=Minecraft.getInstance().getModelManager().getModel(CARRIAGE);
        model(pose,source,light,overlay,carriage);
        ItemStack bit=lathe.getCutter();
        if(!bit.isEmpty()){
            pose.pushPose();
            // Bit protrudes from the carriage holder toward the chuck axis;
            // never independently moves relative to its own metal clamp.
            pose.translate(7D/16D,7D/16D,-3D/16D);
            pose.mulPose(Axis.XP.rotationDegrees(-90F));
            pose.scale(.28F,.28F,.28F);
            BakedModel bitModel=items.getModel(bit,lathe.getLevel(),null,884);
            items.render(bit,ItemDisplayContext.NONE,false,
                    pose,source,light,overlay,bitModel);
            pose.popPose();
        }
        pose.popPose();
        pose.popPose();
    }
}
