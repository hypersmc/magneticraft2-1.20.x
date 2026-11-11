package com.magneticraft2.client.menu;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A real save-based menu scene. The JSON only controls presentation and bounds;
 * normal Minecraft block entities, recipes and networks remain server-authoritative.
 */
final class LivingMenuScene {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SCHEMA_VERSION = 1;
    private static final float DEFAULT_DEGREES_PER_SECOND = 1.5F;

    final String id;
    final Path worldDirectory;
    final boolean rotate;
    final float degreesPerSecond;
    final Vec3 cameraPosition;
    final Float yaw;
    final Float pitch;
    final Integer timeOfDay;
    final Boolean daylightCycle;
    final Integer randomTickSpeed;
    final int maxWorldMiB;
    final int maxStoredChunks;
    final int renderDistanceChunks;
    final int simulationDistanceChunks;

    private LivingMenuScene(String id, Path worldDirectory, boolean rotate, float degreesPerSecond,
                            Vec3 cameraPosition, Float yaw, Float pitch,
                            Integer timeOfDay, Boolean daylightCycle, Integer randomTickSpeed,
                            int maxWorldMiB, int maxStoredChunks,
                            int renderDistanceChunks, int simulationDistanceChunks) {
        this.id = id;
        this.worldDirectory = worldDirectory;
        this.rotate = rotate;
        this.degreesPerSecond = degreesPerSecond;
        this.cameraPosition = cameraPosition;
        this.yaw = yaw;
        this.pitch = pitch;
        this.timeOfDay = timeOfDay;
        this.daylightCycle = daylightCycle;
        this.randomTickSpeed = randomTickSpeed;
        this.maxWorldMiB = maxWorldMiB;
        this.maxStoredChunks = maxStoredChunks;
        this.renderDistanceChunks = renderDistanceChunks;
        this.simulationDistanceChunks = simulationDistanceChunks;
    }

    /**
     * The first alphabetically enabled valid scene is used for now.
     * An existing legacy save remains supported without any migration.
     */
    static LivingMenuScene select(Path gameDirectory, Path savesDirectory) {
        Path base = gameDirectory.resolve("mgcmainmenu");
        Path root = base.resolve("scenes");
        try {
            // Create the library on first use, without traversing directory links.
            if (!Files.exists(base, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectory(base);
            }
            if (!Files.isDirectory(base, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("mgcmainmenu must be a real directory, not a symlink");
            }
            if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectory(root);
            }
            if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("mgcmainmenu/scenes must be a real directory");
            }
        } catch (IOException error) {
            LOGGER.warn("Cannot initialize mgcmainmenu scene library: {}", error.getMessage());
        }
        if (Files.isDirectory(base, LinkOption.NOFOLLOW_LINKS)
                && Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            try (Stream<Path> entries = Files.list(root)) {
                List<Path> folders = entries.sorted(Comparator.comparing(p -> p.getFileName().toString()))
                        .toList();
                for (Path folder : folders) {
                    String id = folder.getFileName().toString();
                    if (!id.matches("[a-z0-9][a-z0-9_-]{0,63}")
                            || !Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) {
                        continue;
                    }
                    try {
                        LivingMenuScene scene = parse(id, folder);
                        if (scene != null) {
                            return scene;
                        }
                    } catch (IOException | RuntimeException error) {
                        LOGGER.warn("Skipping invalid living-menu scene '{}': {}", id, error.getMessage());
                    }
                }
            } catch (IOException error) {
                LOGGER.warn("Cannot enumerate mgcmainmenu scenes: {}", error.getMessage());
            }
        }
        Path legacy = savesDirectory.resolve(LivingMenuWorldCopy.SOURCE_ID);
        if (LivingMenuWorldCopy.sourceExists(legacy)) {
            return new LivingMenuScene("legacy", legacy, true, DEFAULT_DEGREES_PER_SECOND,
                    null, null, null, null, null, null, 128, 8192, 4, 4);
        }
        return null;
    }

    private static LivingMenuScene parse(String id, Path folder) throws IOException {
        Path manifest = folder.resolve("scene.json");
        Path world = folder.resolve("world");
        if (!Files.isRegularFile(manifest, LinkOption.NOFOLLOW_LINKS)
                || !LivingMenuWorldCopy.sourceExists(world)) {
            throw new IOException("Expected a regular scene.json and a world/level.dat");
        }
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(manifest, StandardCharsets.UTF_8)) {
            JsonElement value = JsonParser.parseReader(reader);
            if (!value.isJsonObject()) {
                throw new IOException("scene.json must contain a JSON object");
            }
            root = value.getAsJsonObject();
        }
        if (number(root, "schemaVersion", -1, 1, 1) != SCHEMA_VERSION) {
            throw new IOException("Unsupported scene.json schemaVersion");
        }
        if (!bool(root, "enabled", true)) {
            return null;
        }
        JsonObject camera = object(root, "camera");
        JsonObject rotation = object(camera, "rotation");
        boolean rotate = bool(rotation, "enabled", true);
        float speed = (float) number(rotation, "degreesPerSecond", DEFAULT_DEGREES_PER_SECOND, 0, 12);
        JsonObject position = object(camera, "position");
        Vec3 cameraPosition = null;
        if (camera != null && camera.has("position")) {
            cameraPosition = new Vec3(numberRequired(position, "x", -29_999_984, 29_999_984),
                    numberRequired(position, "y", -64, 320),
                    numberRequired(position, "z", -29_999_984, 29_999_984));
        }
        Float yaw = camera != null && camera.has("yaw")
                ? (float) numberRequired(camera, "yaw", -360, 360) : null;
        Float pitch = camera != null && camera.has("pitch")
                ? (float) numberRequired(camera, "pitch", -90, 90) : null;

        JsonObject worldSettings = object(root, "world");
        Integer time = has(worldSettings, "timeOfDay")
                ? integer(worldSettings, "timeOfDay", 0, 23999) : null;
        Boolean dayCycle = has(worldSettings, "daylightCycle")
                ? bool(worldSettings, "daylightCycle", false) : null;
        Integer randomTick = has(worldSettings, "randomTickSpeed")
                ? integer(worldSettings, "randomTickSpeed", 0, 4096) : null;

        JsonObject limits = object(root, "limits");
        int worldMiB = integer(limits, "maxWorldMiB", 1, 128, 64);
        int storedChunks = integer(limits, "maxStoredChunks", 1, 8192, 512);
        // Forge/Minecraft 1.20.1 requires a radius of at least two chunks.
        // These cap the *player* view and simulation radii, not spawn preparation.
        int renderChunks = integer(limits, "renderDistanceChunks", 2, 8, 4);
        int simulationChunks = integer(limits, "simulationDistanceChunks", 2, 8, 4);
        if (simulationChunks > renderChunks) {
            throw new IOException("simulationDistanceChunks cannot exceed renderDistanceChunks");
        }
        return new LivingMenuScene(id, world, rotate, speed, cameraPosition,
                yaw, pitch, time, dayCycle, randomTick, worldMiB, storedChunks,
                renderChunks, simulationChunks);
    }

    /** Apply the scene to the disposable integrated server, never the player's saves. */
    void apply(Minecraft minecraft) {
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.player == null) {
            return;
        }
        java.util.UUID playerId = minecraft.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                return;
            }
            if (timeOfDay != null || daylightCycle != null || randomTickSpeed != null) {
                for (ServerLevel level : server.getAllLevels()) {
                    if (timeOfDay != null) {
                        level.setDayTime(timeOfDay);
                    }
                }
                if (daylightCycle != null) {
                    server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylightCycle, server);
                }
                if (randomTickSpeed != null) {
                    server.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(randomTickSpeed, server);
                }
            }
            if (cameraPosition != null || yaw != null || pitch != null) {
                Vec3 at = cameraPosition != null ? cameraPosition : player.position();
                player.teleportTo(player.serverLevel(), at.x, at.y, at.z,
                        yaw != null ? yaw : player.getYRot(),
                        pitch != null ? pitch : player.getXRot());
            }
        });
    }

    private static boolean has(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull();
    }

    private static JsonObject object(JsonObject root, String key) throws IOException {
        if (!has(root, key)) {
            return null;
        }
        if (!root.get(key).isJsonObject()) {
            throw new IOException(key + " must be a JSON object");
        }
        return root.getAsJsonObject(key);
    }

    private static boolean bool(JsonObject obj, String key, boolean defaultValue) throws IOException {
        if (!has(obj, key)) {
            return defaultValue;
        }
        JsonElement element = obj.get(key);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw new IOException(key + " must be a JSON boolean");
        }
        return element.getAsBoolean();
    }

    private static int integer(JsonObject obj, String key, int min, int max) throws IOException {
        return integer(obj, key, min, max, -1);
    }

    private static int integer(JsonObject obj, String key, int min, int max, int defaultValue) throws IOException {
        double value = number(obj, key, defaultValue, min, max);
        if (value != Math.rint(value)) {
            throw new IOException(key + " must be an integer");
        }
        return (int) value;
    }

    private static double numberRequired(JsonObject obj, String key, double min, double max) throws IOException {
        if (!has(obj, key)) {
            throw new IOException("Missing required numeric field " + key);
        }
        return number(obj, key, 0, min, max);
    }

    private static double number(JsonObject obj, String key, double fallback, double min, double max)
            throws IOException {
        if (!has(obj, key)) {
            return fallback;
        }
        JsonElement element = obj.get(key);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IOException(key + " must be numeric");
        }
        double value = element.getAsDouble();
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new IOException(key + " outside the supported range " + min + ".." + max);
        }
        return value;
    }
}
