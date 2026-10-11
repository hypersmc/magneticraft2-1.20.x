package com.magneticraft2.common.systems.Multiblocking.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Voxel V2: data-pack-defined collision/selection/interaction shapes.
 *
 * One SOUTH-oriented source in model pixels is compiled/cached for the four
 * horizontal directions when the server data packs reload. On formation
 * MultiblockRenderParts writes block-local snapshots to the controller and
 * filler BEs. Clients receive these exact snapshots in normal BE packets,
 * including when their resource packs differ from the server data pack.
 *
 * The outline is NOT rebuilt during per-tick collision checks.
 */
public final class MultiblockVoxelProfiles extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogManager.getLogger("Magneticraft2Voxels");
    private static final Direction[] HORIZONTAL = {
            Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST
    };
    private static volatile Map<ResourceLocation, Profile> PROFILES = Map.of();

    public MultiblockVoxelProfiles() {
        super(new Gson(), "multiblock_voxels");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources,
                         ProfilerFiller profiler) {
        Map<ResourceLocation, Profile> compiled = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : json.entrySet()) {
            try {
                compiled.put(e.getKey(), decode(e.getValue().getAsJsonObject()));
            } catch (RuntimeException invalid) {
                LOGGER.error("Ignoring invalid multiblock voxel profile {}: {}",
                        e.getKey(), invalid.getMessage());
            }
        }
        PROFILES = Map.copyOf(compiled);
        LOGGER.info("Loaded {} Voxel V2 multiblock profiles", compiled.size());
    }

    public static boolean exists(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key != null && PROFILES.containsKey(key);
    }

    @Nullable
    public static VoxelShape collision(String id, Direction facing) {
        Profile p = profile(id);
        return p == null ? null : p.collision.get(facing);
    }

    @Nullable
    public static VoxelShape selection(String id, Direction facing) {
        Profile p = profile(id);
        return p == null ? null : p.selection.get(facing);
    }

    /**
     * Finds a named interaction region, e.g. firebox, tap or crucible. The
     * hit is in world units, not BlockHitResult's clicked block-local offset.
     * The first matching zone wins, preserving authored priority.
     */
    @Nullable
    public static String hitZone(String id, Direction facing, BlockPos controller, Vec3 hit) {
        Profile p = profile(id);
        if (p == null) return null;
        Map<String, List<AABB>> zones = p.zones.get(facing);
        if (zones == null) return null;
        Vec3 local = hit.subtract(controller.getX(), controller.getY(), controller.getZ());
        for (Map.Entry<String, List<AABB>> entry : zones.entrySet()) {
            for (AABB box : entry.getValue()) {
                if (box.inflate(0.002D).contains(local)) return entry.getKey();
            }
        }
        return null;
    }

    @Nullable
    private static Profile profile(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? null : PROFILES.get(key);
    }

    private static Profile decode(JsonObject json) {
        if (!json.has("format") || json.get("format").getAsInt() != 1
                || !json.has("unit") || !"model_pixels".equals(json.get("unit").getAsString())) {
            throw new JsonParseException("Expected format=1 and unit=model_pixels");
        }
        List<AABB> collision = boxes(json.getAsJsonArray("collision"), "collision", false);
        List<AABB> selection = json.has("selection")
                ? boxes(json.getAsJsonArray("selection"), "selection", false)
                : collision;
        Map<String, List<AABB>> zones = new LinkedHashMap<>();
        if (json.has("zones")) {
            JsonObject zoneData = json.getAsJsonObject("zones");
            if (zoneData.size() > 32) throw new JsonParseException("Too many interaction zones");
            for (Map.Entry<String, JsonElement> entry : zoneData.entrySet()) {
                if (!entry.getKey().matches("[a-z0-9_]+")) {
                    throw new JsonParseException("Invalid interaction zone: " + entry.getKey());
                }
                zones.put(entry.getKey(), boxes(entry.getValue().getAsJsonArray(),
                        "zones." + entry.getKey(), true));
            }
        }

        EnumMap<Direction, VoxelShape> collisions = new EnumMap<>(Direction.class);
        EnumMap<Direction, VoxelShape> selections = new EnumMap<>(Direction.class);
        EnumMap<Direction, Map<String, List<AABB>>> worldZones = new EnumMap<>(Direction.class);
        for (Direction dir : HORIZONTAL) {
            collisions.put(dir, compose(collision, dir));
            selections.put(dir, compose(selection, dir));
            Map<String, List<AABB>> rotatedZones = new LinkedHashMap<>();
            for (Map.Entry<String, List<AABB>> entry : zones.entrySet()) {
                rotatedZones.put(entry.getKey(), entry.getValue().stream()
                        .map(b -> rotate(b, dir)).toList());
            }
            worldZones.put(dir, Map.copyOf(rotatedZones));
        }
        return new Profile(collisions, selections, worldZones);
    }

    private static List<AABB> boxes(@Nullable JsonArray elements, String key, boolean allowEmpty) {
        if (elements == null || elements.size() > 512 || (!allowEmpty && elements.isEmpty())) {
            throw new JsonParseException("Invalid box count for " + key);
        }
        List<AABB> boxes = new ArrayList<>(elements.size());
        for (JsonElement element : elements) {
            JsonArray coords = element.getAsJsonArray();
            if (coords.size() != 6) throw new JsonParseException(key + ": expected six coordinates");
            double[] v = new double[6];
            for (int i = 0; i < 6; i++) v[i] = coords.get(i).getAsDouble() / 16D;
            for (double n : v) {
                if (!Double.isFinite(n) || Math.abs(n) > 1024D) {
                    throw new JsonParseException(key + ": invalid/out-of-range coordinate");
                }
            }
            if (!(v[3] > v[0] && v[4] > v[1] && v[5] > v[2])) {
                throw new JsonParseException(key + ": inverted or empty box");
            }
            boxes.add(new AABB(v[0], v[1], v[2], v[3], v[4], v[5]));
        }
        return List.copyOf(boxes);
    }

    private static VoxelShape compose(List<AABB> boxes, Direction facing) {
        VoxelShape joined = Shapes.empty();
        for (AABB box : boxes) {
            joined = Shapes.joinUnoptimized(joined, Shapes.create(rotate(box, facing)),
                    BooleanOp.OR);
        }
        return joined.optimize();
    }

    /** Same 8px-center XZ transformation as the V2 model exporter. */
    private static AABB rotate(AABB b, Direction facing) {
        return switch (facing) {
            case SOUTH -> b;
            case WEST -> new AABB(1D - b.maxZ, b.minY, b.minX,
                    1D - b.minZ, b.maxY, b.maxX);
            case NORTH -> new AABB(1D - b.maxX, b.minY, 1D - b.maxZ,
                    1D - b.minX, b.maxY, 1D - b.minZ);
            case EAST -> new AABB(b.minZ, b.minY, 1D - b.maxX,
                    b.maxZ, b.maxY, 1D - b.minX);
            default -> throw new IllegalArgumentException("Multiblock voxel facing must be horizontal");
        };
    }

    private record Profile(EnumMap<Direction, VoxelShape> collision,
                           EnumMap<Direction, VoxelShape> selection,
                           EnumMap<Direction, Map<String, List<AABB>>> zones) { }
}
