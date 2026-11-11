package com.magneticraft2.client.render.blocks.stage.steam;

import com.magneticraft2.client.animation.AnimationDefinition;
import com.magneticraft2.client.animation.AnimationFrame;
import com.magneticraft2.client.animation.MachineVisualRenderer;
import com.magneticraft2.common.block.stage.steam.SteamEngineBlock;
import com.magneticraft2.common.blockentity.stage.steam.SteamEngineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import java.util.EnumSet;
import java.util.Map;

public final class SteamEngineBlockEntityRenderer
        implements BlockEntityRenderer<SteamEngineBlockEntity> {
    private static final ResourceLocation VISUAL =
            new ResourceLocation("magneticraft2","steam_engine");

    public SteamEngineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public int getViewDistance(){return 96;}

    @Override public void render(SteamEngineBlockEntity engine,float partialTicks,
            PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        BlockState state=engine.getBlockState();
        if(!state.hasProperty(SteamEngineBlock.IS_FORMED)
                || !state.getValue(SteamEngineBlock.IS_FORMED))return;

        Direction facing=state.getValue(SteamEngineBlock.FACING);
        pose.pushPose();
        try{
            float yaw=switch(facing){
                case EAST->90F;case NORTH->180F;case WEST->-90F;default->0F;
            };
            if(yaw!=0){
                pose.translate(0.5D,0,0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(yaw));
                pose.translate(-0.5D,0,-0.5D);
            }
            float angle=engine.getDriveRotationDegrees(partialTicks);
            EnumSet<AnimationDefinition.State> states=EnumSet.of(
                    engine.isRunning()?AnimationDefinition.State.RUNNING:AnimationDefinition.State.STOPPED);
            if(engine.isOutputOverloadedForAnimation())
                states.add(AnimationDefinition.State.OVERLOADED);
            AnimationFrame frame=new AnimationFrame(Map.of("steam_engine_output",angle),states);
            MachineVisualRenderer.renderScene(VISUAL,frame,state,pose,buffers,light,overlay);
        }finally{pose.popPose();}
    }
}
