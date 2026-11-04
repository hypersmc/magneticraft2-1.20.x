package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeMechanicalSawmillBlock;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeMechanicalSawmillBlockEntity;
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

/** Cached blade mesh and a single mounted log; no dynamic model allocation. */
public final class BronzeMechanicalSawmillRenderer
        implements BlockEntityRenderer<BronzeMechanicalSawmillBlockEntity> {
    private static final ResourceLocation COUPLING =
            new ResourceLocation("magneticraft2", "multiblock/bronze_mechanical_sawmill_coupling");
    private static final ResourceLocation BLADE =
            new ResourceLocation("magneticraft2", "multiblock/bronze_mechanical_sawmill_blade");
    public BronzeMechanicalSawmillRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public int getViewDistance() { return 64; }
    @Override public void render(BronzeMechanicalSawmillBlockEntity sawmill, float partial,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!sawmill.isFormed()) return;
        Direction facing = sawmill.getBlockState().getValue(BronzeMechanicalSawmillBlock.FACING);
        float yaw = switch(facing) { case WEST -> -90F; case NORTH -> 180F;
            case EAST -> 90F; default -> 0F; };
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-.5D, 0D, -.5D);

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher blocks = mc.getBlockRenderer();
        BakedModel blade = mc.getModelManager().getModel(BLADE);
        BakedModel coupling = mc.getModelManager().getModel(COUPLING);
        BlockState state = BlockRegistry.BRONZE_MECHANICAL_SAWMILL.get().defaultBlockState();
        pose.pushPose();
        // Real rear input is replaced when formed: show a short supported coupling.
        pose.translate(.5D, 8D/16D, -24D/16D);
        pose.mulPose(Axis.ZP.rotationDegrees(sawmill.getSawingSpindleAngle(partial)));
        pose.translate(-.5D,-.5D,-.5D);
        blocks.getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()),
                state,coupling,1F,1F,1F,light,overlay);
        pose.popPose();

        pose.pushPose();
        // Saw disc is vertical in the XY plane, on a supported Z-axis arbor.
        // Local y=24, z=-4; the feed sled travels left-to-right through it.
        pose.translate(.5D, 24D/16D, -4D/16D);
        pose.mulPose(Axis.ZP.rotationDegrees(sawmill.getSawingSpindleAngle(partial)));
        pose.translate(-.5D,-.5D,-.5D);
        blocks.getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()),
                state,blade,1F,1F,1F,light,overlay);
        pose.popPose();

        ItemStack timber = sawmill.getWorkpiece();
        if (!timber.isEmpty()) {
            ItemRenderer items = mc.getItemRenderer();
            pose.pushPose();
            // Workpiece rests directly on the solid feed sled, not in midair.
            pose.translate(.5D,21D/16D,-4D/16D);
            pose.mulPose(Axis.ZP.rotationDegrees(90F));
            pose.scale(.9F,.9F,.9F);
            items.render(timber,ItemDisplayContext.NONE,false,pose,buffers,light,overlay,
                    items.getModel(timber,sawmill.getLevel(),null,728));
            pose.popPose();
        }
        pose.popPose();
    }
}
