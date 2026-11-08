package com.magneticraft2.client.render.blocks.stage.iron;

import com.magneticraft2.client.render.blocks.stage.copper.LargeGearBlock_woodRenderer;
import com.magneticraft2.common.blockentity.stage.iron.WroughtIronLargeGearBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Reuses existing 16-tooth large gear phase and powered rendering unchanged. */
public final class WroughtIronLargeGearRenderer
        implements BlockEntityRenderer<WroughtIronLargeGearBlockEntity> {
    private final LargeGearBlock_woodRenderer shared;
    public WroughtIronLargeGearRenderer(BlockEntityRendererProvider.Context context) {
        shared = new LargeGearBlock_woodRenderer(context);
    }
    @Override public void render(WroughtIronLargeGearBlockEntity entity, float partial,
            PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        shared.render(entity, partial, pose, buffer, light, overlay);
    }
}
