package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeIndustrialAirBlowerBlock;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeIndustrialAirBlowerBlockEntity;
import com.magneticraft2.common.registry.registers.BlockRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Only two baked models are rotated. Static housings are rendered by V2
 * block meshes, avoiding per-frame mesh generation and fake item entities.
 */
public final class BronzeIndustrialAirBlowerRenderer
        implements BlockEntityRenderer<BronzeIndustrialAirBlowerBlockEntity> {
    private static final ResourceLocation ROTOR =
            new ResourceLocation("magneticraft2", "multiblock/bronze_industrial_air_blower_rotor");
    private static final ResourceLocation COUPLING =
            new ResourceLocation("magneticraft2", "multiblock/bronze_industrial_air_blower_coupling");
    public BronzeIndustrialAirBlowerRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public int getViewDistance() { return 64; }
    @Override public void render(BronzeIndustrialAirBlowerBlockEntity blower, float partial,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!blower.isFormed()) return;
        Direction facing = blower.getBlockState().getValue(BronzeIndustrialAirBlowerBlock.FACING);
        float yaw = switch(facing) { case WEST -> -90F; case NORTH -> 180F;
            case EAST -> 90F; default -> 0F; };
        pose.pushPose();
        pose.translate(.5D,0D,.5D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-.5D,0D,-.5D);

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher blocks = mc.getBlockRenderer();
        BlockState state = BlockRegistry.BRONZE_INDUSTRIAL_AIR_BLOWER.get().defaultBlockState();
        float angle = blower.getRotorAngle(partial);
        draw(blocks, mc.getModelManager().getModel(COUPLING), state, pose, buffers,
                light, overlay, angle, 8D/16D, -24D/16D);
        draw(blocks, mc.getModelManager().getModel(ROTOR), state, pose, buffers,
                light, overlay, angle, 24D/16D, -16D/16D);
        pose.popPose();
    }
    private static void draw(BlockRenderDispatcher blocks,BakedModel model,BlockState state,
            PoseStack pose,MultiBufferSource buffers,int light,int overlay,float angle,
            double y,double z) {
        pose.pushPose();
        pose.translate(.5D,y,z);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-.5D,-.5D,-.5D);
        blocks.getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.cutout()),
                state,model,1F,1F,1F,light,overlay);
        pose.popPose();
    }
}
