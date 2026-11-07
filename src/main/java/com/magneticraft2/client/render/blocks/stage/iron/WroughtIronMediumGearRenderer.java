package com.magneticraft2.client.render.blocks.stage.iron;

import com.magneticraft2.client.render.blocks.stage.copper.MediumGearBlockEntity_woodRenderer;
import com.magneticraft2.common.blockentity.stage.iron.WroughtIronMediumGearBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Reuses the already-tested medium-gear rotation and rendering path. */
public final class WroughtIronMediumGearRenderer
        implements BlockEntityRenderer<WroughtIronMediumGearBlockEntity> {
    private final MediumGearBlockEntity_woodRenderer shared;

    public WroughtIronMediumGearRenderer(BlockEntityRendererProvider.Context context) {
        shared = new MediumGearBlockEntity_woodRenderer(context);
    }

    @Override public void render(WroughtIronMediumGearBlockEntity entity, float partialTicks,
            PoseStack stack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        shared.render(entity, partialTicks, stack, bufferSource, packedLight, packedOverlay);
    }
}
