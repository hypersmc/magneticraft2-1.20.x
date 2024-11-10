package com.magneticraft2.client.model;

import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.minecraftforge.client.model.geometry.UnbakedGeometryHelper;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author JumpWatch on 04-07-2024
 * @Project mgc2-1.20
 * v1.0.0
 */
public class MultiBlockGeometry implements IUnbakedGeometry<MultiBlockGeometry> {
    private final BlockModel baseModel;
    private final Map<String, BlockModel> namedModels;  // Map of model names to BlockModel

    MultiBlockGeometry(BlockModel baseModel, Map<String, BlockModel> namedModels) {
        this.baseModel = baseModel;
        this.namedModels = namedModels;
    }
    @Override
    public BakedModel bake(IGeometryBakingContext ctx, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation resourceLocation) {
        Map<String, BakedModel> bakedModels = new HashMap<>();
        namedModels.forEach((name, blockModel) -> {
            ModelState rotState = new ModelState() {
                @Override
                public Transformation getRotation() {
                    return Transformation.identity();  // Adjust rotation as necessary
                }

                @Override
                public boolean isUvLocked() {
                    return modelState.isUvLocked();
                }
            };
            BakedModel bakedModel = blockModel.bake(baker, blockModel, spriteGetter, rotState, resourceLocation, true);
            bakedModels.put(name, bakedModel);
        });
        return new MultiBlockModel(baseModel.bake(baker, baseModel, spriteGetter, modelState, resourceLocation, true), bakedModels);
    }

    private int getRotation(String modelName) {
        // You can define rotation logic based on the model name if needed
        return (modelName.hashCode() % 4) * 90;
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        baseModel.resolveParents(modelGetter);
        namedModels.values().forEach(model -> model.resolveParents(modelGetter));
    }
}