package com.magneticraft2.client.model;

import com.magneticraft2.common.utils.MultiBlockProperties;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * @author JumpWatch on 04-07-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MultiBlockModel extends BakedModelWrapper<BakedModel> {
    public static final Logger LOGGER = LogManager.getLogger("MultiBlockModel");
    private final Map<String, BakedModel> modelMap; // Assuming you have a map of named models


    MultiBlockModel(BakedModel baseModel, Map<String, BakedModel> modelMap) {
        super(baseModel);
        this.modelMap = modelMap;
    }
    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        String modelName = extraData.get(MultiBlockProperties.MODEL_NAME);
        LOGGER.info("Getting quads for model {}", modelName);
        if (modelName != null) {
            LOGGER.info("Attempting to render model: {}", modelName);
            BakedModel model = modelMap.get(modelName);
            if (model != null) {
                return model.getQuads(state, side, rand, extraData, renderType);
            } else {
                LOGGER.info("No model found for name: {}", modelName);
            }
        }else{
            LOGGER.info("No model found for name: {}", extraData.get(MultiBlockProperties.MODEL_NAME));
        }
        return super.getQuads(state, side, rand, extraData, renderType);
    }
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state,side,rand,ModelData.EMPTY, null);
    }


}
