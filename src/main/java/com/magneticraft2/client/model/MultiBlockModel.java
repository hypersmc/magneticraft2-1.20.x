package com.magneticraft2.client.model;

import com.magneticraft2.common.utils.MultiBlockProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Existing custom multiblock geometry selector, now also used for V2
 * filler chunks. A missing part is invisible, never a purple/black cube.
 * The actual part geometry is vanilla-baked once by Minecraft's ModelManager.
 */
public class MultiBlockModel extends BakedModelWrapper<BakedModel> {
    private final Map<String, BakedModel> modelMap;

    MultiBlockModel(BakedModel baseModel, Map<String, BakedModel> modelMap) {
        super(baseModel);
        this.modelMap = modelMap;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state,
                                             @Nullable Direction side,
                                             @NotNull RandomSource random,
                                             @NotNull ModelData extraData,
                                             @Nullable RenderType renderType) {
        String modelKey = extraData.get(MultiBlockProperties.MODEL_NAME);
        if (modelKey == null || modelKey.isBlank()) {
            return super.getQuads(state, side, random, extraData, renderType);
        }

        String path = modelKey.startsWith("multiblock/")
                ? modelKey : "multiblock/" + modelKey;
        ResourceLocation location = ResourceLocation.tryParse("magneticraft2:" + path);
        if (location == null) return List.of();

        var manager = Minecraft.getInstance().getModelManager();
        BakedModel part = manager.getModel(location);
        if (part == null || part == manager.getMissingModel() || part == this) {
            return List.of();
        }
        return part.getQuads(state, side, random, extraData, renderType);
    }
}
