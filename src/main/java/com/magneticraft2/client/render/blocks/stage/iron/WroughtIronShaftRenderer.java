package com.magneticraft2.client.render.blocks.stage.iron;

import com.magneticraft2.client.render.blocks.stage.copper.ShaftBlockEntity_woodRenderer;
import com.magneticraft2.common.blockentity.stage.iron.WroughtIronShaftBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Same rotation and baked-block geometry path as the wooden shaft. */
public final class WroughtIronShaftRenderer
        implements BlockEntityRenderer<WroughtIronShaftBlockEntity> {
    private final ShaftBlockEntity_woodRenderer shared;

    public WroughtIronShaftRenderer(BlockEntityRendererProvider.Context context) {
        shared = new ShaftBlockEntity_woodRenderer(context);
    }

    @Override
    public void render(WroughtIronShaftBlockEntity entity, float partialTicks,
            PoseStack stack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        shared.render(entity, partialTicks, stack, buffer, packedLight, packedOverlay);
    }
}
