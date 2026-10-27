package com.magneticraft2.client.hologram;

import com.magneticraft2.client.animation.AnimationDefinition;
import com.magneticraft2.client.animation.AnimationResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Derives a conservative world-space AABB from the actual baked models used by
 * a recorded JSON scene, including its model variants and rotating parts.
 *
 * <p>Computed only when the tooltip needs the values, cached until resources
 * reload. Never scans geometry on every rendering frame.
 *
 * <p>For arbitrary animated parent transforms and slider-cranks, the bounds
 * are conservative rather than mathematically exact. The player-facing
 * inscription always rounds UP to whole blocks.
 */
public final class HologramSceneBounds {
    private static final Map<ResourceLocation, Dimensions> CACHE = new ConcurrentHashMap<>();
    public record Dimensions(int width, int height, int depth) {
        public String engraving() {
            return width + " x " + height + " x " + depth;
        }
    }

    private HologramSceneBounds() {}

    public static void clearCache() {
        CACHE.clear();
    }

    public static Dimensions get(ResourceLocation animation) {
        AnimationDefinition definition = AnimationResourceLoader.get(animation);
        if (definition == null) return null;
        return CACHE.computeIfAbsent(animation, ignored -> calculate(definition));
    }

    private static Dimensions calculate(AnimationDefinition definition) {
        ModelManager manager = Minecraft.getInstance().getModelManager();
        double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY};
        double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY};
        boolean seen = false;
        for (String name : definition.componentNames()) {
            AnimationDefinition.Component component = definition.component(name);
            Set<ResourceLocation> models = new HashSet<>();
            if (component.model() != null) models.add(component.model());
            if (component.modelVariants() != null) {
                models.addAll(component.modelVariants().models().values());
            }
            if (models.isEmpty()) continue;

            Vec3 offset = absoluteOffset(definition, name);
            boolean animatedY = hasRotationY(definition, name);
            boolean rotatesOtherAxis = hasRotationOtherAxis(definition, name)
                    || component.staticRotation() != null;

            for (ResourceLocation modelId : models) {
                BakedModel model = manager.getModel(modelId);
                if (model == null || model == manager.getMissingModel()) continue;
                double[] localMin = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                        Double.POSITIVE_INFINITY};
                double[] localMax = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                        Double.NEGATIVE_INFINITY};
                for (Direction face : Direction.values()) {
                    includeQuads(model.getQuads(Blocks.STONE.defaultBlockState(),
                            face, RandomSource.create(42L)), localMin, localMax);
                }
                includeQuads(model.getQuads(Blocks.STONE.defaultBlockState(),
                        null, RandomSource.create(42L)), localMin, localMax);
                if (!Double.isFinite(localMin[0])) continue;

                if (rotatesOtherAxis) {
                    // Any arbitrary rotation around a pivot stays within this
                    // sphere, including rotations inherited from a parent.
                    Vec3 p = component.pivot();
                    double radius = 0D;
                    for (int x = 0; x < 2; x++)
                        for (int y = 0; y < 2; y++)
                            for (int z = 0; z < 2; z++) {
                                double dx = (x == 0 ? localMin[0] : localMax[0]) - p.x;
                                double dy = (y == 0 ? localMin[1] : localMax[1]) - p.y;
                                double dz = (z == 0 ? localMin[2] : localMax[2]) - p.z;
                                radius = Math.max(radius, Math.sqrt(dx * dx + dy * dy + dz * dz));
                            }
                    include(min, max, offset.x + p.x - radius, offset.y + p.y - radius,
                            offset.z + p.z - radius);
                    include(min, max, offset.x + p.x + radius, offset.y + p.y + radius,
                            offset.z + p.z + radius);
                } else if (animatedY) {
                    Vec3 p = component.pivot();
                    double radius = 0D;
                    for (int x = 0; x < 2; x++)
                        for (int z = 0; z < 2; z++) {
                            double dx = (x == 0 ? localMin[0] : localMax[0]) - p.x;
                            double dz = (z == 0 ? localMin[2] : localMax[2]) - p.z;
                            radius = Math.max(radius, Math.hypot(dx, dz));
                        }
                    include(min, max, offset.x + p.x - radius,
                            offset.y + localMin[1], offset.z + p.z - radius);
                    include(min, max, offset.x + p.x + radius,
                            offset.y + localMax[1], offset.z + p.z + radius);
                } else {
                    include(min, max, localMin[0] + offset.x, localMin[1] + offset.y,
                            localMin[2] + offset.z);
                    include(min, max, localMax[0] + offset.x, localMax[1] + offset.y,
                            localMax[2] + offset.z);
                }
                seen = true;
            }
        }
        if (!seen) return new Dimensions(0, 0, 0);
        return new Dimensions(ceilBlocks(max[0] - min[0]),
                ceilBlocks(max[1] - min[1]), ceilBlocks(max[2] - min[2]));
    }

    private static Vec3 absoluteOffset(AnimationDefinition definition, String name) {
        Vec3 result = Vec3.ZERO;
        Set<String> visited = new HashSet<>();
        while (name != null && visited.add(name)) {
            AnimationDefinition.Component component = definition.component(name);
            if (component == null) break;
            result = result.add(component.modelOffset());
            name = component.parent();
        }
        return result;
    }

    private static boolean hasRotationY(AnimationDefinition def, String name) {
        return hasRotation(def, name, Direction.Axis.Y);
    }

    private static boolean hasRotationOtherAxis(AnimationDefinition def, String name) {
        return hasRotation(def, name, Direction.Axis.X)
                || hasRotation(def, name, Direction.Axis.Z);
    }

    private static boolean hasRotation(AnimationDefinition def, String name,
                                       Direction.Axis axis) {
        for (AnimationDefinition.State state : AnimationDefinition.State.values()) {
            AnimationDefinition.RotationTrack track =
                    def.rotation(name, Set.of(state));
            if (track != null && track.axis() == axis) return true;
        }
        return false;
    }

    private static void includeQuads(java.util.List<BakedQuad> quads,
                                     double[] min, double[] max) {
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices();
            int stride = vertices.length / 4;
            if (stride < 3) continue;
            for (int i = 0; i < 4; i++) {
                int start = i * stride;
                double x = Float.intBitsToFloat(vertices[start]);
                double y = Float.intBitsToFloat(vertices[start + 1]);
                double z = Float.intBitsToFloat(vertices[start + 2]);
                if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
                    include(min, max, x, y, z);
                }
            }
        }
    }

    private static void include(double[] min, double[] max,
                                double x, double y, double z) {
        min[0] = Math.min(min[0], x);
        min[1] = Math.min(min[1], y);
        min[2] = Math.min(min[2], z);
        max[0] = Math.max(max[0], x);
        max[1] = Math.max(max[1], y);
        max[2] = Math.max(max[2], z);
    }

    private static int ceilBlocks(double amount) {
        return Math.max(1, Math.min(128, (int) Math.ceil(amount - 1.0E-5D)));
    }
}
