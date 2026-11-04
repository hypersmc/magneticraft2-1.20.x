package com.magneticraft2.client.hologram;

import com.magneticraft2.client.animation.AnimationDefinition;
import com.magneticraft2.client.animation.AnimationResourceLoader;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Diagnostic for real baked models used by client-only holographic recordings.
 * Runs ONCE per animation/resource reload, never per frame or per component
 * render. This catches a material whose geometry bakes successfully but whose
 * UVs point at Minecraft's missing-texture atlas sprite (magenta checker).
 */
public final class HologramModelDiagnostics {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<ResourceLocation> CHECKED_SCENES =
            ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> REPORTED_MODELS =
            ConcurrentHashMap.newKeySet();

    private HologramModelDiagnostics() {}

    public static void clearCache() {
        CHECKED_SCENES.clear();
        REPORTED_MODELS.clear();
    }

    public static void check(ResourceLocation animation) {
        if (animation == null || !CHECKED_SCENES.add(animation)) return;
        AnimationDefinition definition = AnimationResourceLoader.get(animation);
        if (definition == null) return;

        ModelManager manager = Minecraft.getInstance().getModelManager();
        if (manager == null) return;
        Set<ResourceLocation> modelIds = new HashSet<>();
        for (AnimationDefinition.Component component : definition.components()) {
            if (component.model() != null) modelIds.add(component.model());
            if (component.modelVariants() != null) {
                modelIds.addAll(component.modelVariants().models().values());
            }
        }

        for (ResourceLocation id : modelIds) {
            try {
                BakedModel model = manager.getModel(id);
                if (model == null || model == manager.getMissingModel()) {
                    if (REPORTED_MODELS.add(id)) {
                        LOGGER.warn("Hologram scene {} has an unbaked/missing model {}",
                                animation, id);
                    }
                    continue;
                }
                boolean missingSprite = false;
                // Some baked models place quads in the null (unculled) face;
                // others have per-direction quads. Check both, not just null.
                for (int i = -1; i < Direction.values().length; i++) {
                    Direction face = i < 0 ? null : Direction.values()[i];
                    for (BakedQuad quad : model.getQuads(
                            Blocks.STONE.defaultBlockState(), face,
                            RandomSource.create(42L))) {
                        if (quad.getSprite() != null
                                && MissingTextureAtlasSprite.getLocation().equals(
                                        quad.getSprite().contents().name())) {
                            missingSprite = true;
                            break;
                        }
                    }
                    if (missingSprite) break;
                }
                if (missingSprite && REPORTED_MODELS.add(id)) {
                    LOGGER.warn("Hologram scene {} model {} bakes with the "
                                    + "missing-texture checkerboard sprite; "
                                    + "verify the model's parent and texture IDs",
                            animation, id);
                }
            } catch (RuntimeException ex) {
                if (REPORTED_MODELS.add(id)) {
                    LOGGER.warn("Cannot inspect hologram model {} in scene {}: {}",
                            id, animation, ex.getMessage());
                }
            }
        }
    }
}
