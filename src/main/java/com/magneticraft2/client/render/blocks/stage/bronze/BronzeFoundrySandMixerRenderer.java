package com.magneticraft2.client.render.blocks.stage.bronze;

import com.magneticraft2.common.block.stage.bronze.BronzeFoundrySandMixerBlock;
import com.magneticraft2.common.blockentity.stage.bronze.BronzeFoundrySandMixerBlockEntity;
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

/** Cached mixing paddles and coupling; one feed item, no per-frame geometry. */
public final class BronzeFoundrySandMixerRenderer
        implements BlockEntityRenderer<BronzeFoundrySandMixerBlockEntity> {
    private static final ResourceLocation COUPLING =
            new ResourceLocation("magneticraft2", "multiblock/bronze_foundry_sand_mixer_coupling");
    private static final ResourceLocation PADDLES =
            new ResourceLocation("magneticraft2", "multiblock/bronze_foundry_sand_mixer_paddles");
    public BronzeFoundrySandMixerRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public int getViewDistance() { return 64; }
    @Override public void render(BronzeFoundrySandMixerBlockEntity mixer, float partial,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!mixer.isFormed()) return;
        Direction facing = mixer.getBlockState().getValue(BronzeFoundrySandMixerBlock.FACING);
        float yaw = switch(facing) { case WEST -> -90F; case NORTH -> 180F;
            case EAST -> 90F; default -> 0F; };
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-.5D, 0D, -.5D);

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher blocks = mc.getBlockRenderer();
        BakedModel paddles = mc.getModelManager().getModel(PADDLES);
        BakedModel coupling = mc.getModelManager().getModel(COUPLING);
        BlockState state = BlockRegistry.BRONZE_FOUNDRY_SAND_MIXER.get().defaultBlockState();
        pose.pushPose();
        // Real rear input is replaced when formed: show a short supported coupling.
        pose.translate(.5D, 8D/16D, -24D/16D);
        pose.mulPose(Axis.ZP.rotationDegrees(mixer.getMixingSpindleAngle(partial)));
        pose.translate(-.5D,-.5D,-.5D);
        blocks.getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()),
                state,coupling,1F,1F,1F,light,overlay);
        pose.popPose();

        pose.pushPose();
        // Paddles turn about Z on a supported shaft inside the trough.
        // Local y=24, z=-4; the paddle turns inside a supported trough.
        pose.translate(.5D, 24D/16D, -4D/16D);
        pose.mulPose(Axis.ZP.rotationDegrees(mixer.getMixingSpindleAngle(partial)));
        pose.translate(-.5D,-.5D,-.5D);
        blocks.getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()),
                state,paddles,1F,1F,1F,light,overlay);
        pose.popPose();

        ItemStack feedBlend = mixer.getWorkpiece();
        if (!feedBlend.isEmpty()) {
            ItemRenderer items = mc.getItemRenderer();
            pose.pushPose();
            // Feed blend visibly rests just above the mixing-trough floor (y=20px).
            pose.translate(.5D,21D/16D,-4D/16D);
            pose.mulPose(Axis.XP.rotationDegrees(90F));
            pose.scale(.9F,.9F,.9F);
            items.render(feedBlend,ItemDisplayContext.NONE,false,pose,buffers,light,overlay,
                    items.getModel(feedBlend,mixer.getLevel(),null,728));
            pose.popPose();
        }
        pose.popPose();
    }
}
