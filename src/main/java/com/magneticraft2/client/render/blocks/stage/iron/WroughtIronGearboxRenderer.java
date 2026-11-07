package com.magneticraft2.client.render.blocks.stage.iron;

import com.magneticraft2.client.render.blocks.stage.copper.GearboxBlockEntity_woodRenderer;
import com.magneticraft2.common.blockentity.stage.iron.WroughtIronGearboxBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Identical established bevel/coupling geometry and animation, with iron
 * metal and bronze supports instead of the wooden gearbox's oak texture.
 */
public final class WroughtIronGearboxRenderer
        implements BlockEntityRenderer<WroughtIronGearboxBlockEntity> {
    private static final ResourceLocation IRON =
            new ResourceLocation("magneticraft2", "textures/block/material/dark_work_iron.png");
    private static final ResourceLocation BRONZE =
            new ResourceLocation("magneticraft2", "textures/block/material/bronze_machined.png");
    private final GearboxBlockEntity_woodRenderer shared;

    public WroughtIronGearboxRenderer(BlockEntityRendererProvider.Context context) {
        shared = new GearboxBlockEntity_woodRenderer(context, IRON, IRON, BRONZE);
    }

    @Override
    public void render(WroughtIronGearboxBlockEntity entity, float partialTicks,
            PoseStack stack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        shared.render(entity, partialTicks, stack, buffer, packedLight, packedOverlay);
    }
}
