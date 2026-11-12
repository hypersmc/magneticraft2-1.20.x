package com.magneticraft2.client.menu;

import net.minecraft.client.Minecraft;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Temporary view/simulation distance limits for the disposable menu server.
 *
 * IntegratedServer.tickServer copies the client's Options distances every tick.
 * Limiting only ServerPlayerList distances would therefore be undone immediately.
 *
 * Keep the user's values in memory and restore them before opening any regular
 * settings screen or normal game. Never call Options.save() here.
 *
 * This bounds player-view tickets, not Minecraft 1.20.1's hardcoded 441 spawn
 * chunks. The latter would require a separate isolated startup patch.
 */
final class LivingMenuChunkLimits {
    private static final Logger LOGGER = LogUtils.getLogger();
    // Minecraft 1.20.1 Options.simulationDistance is IntRange(5, 32|16).
    // Unlike render distance, values below five are rejected by OptionInstance.
    private static final int MIN_SIMULATION_DISTANCE = 5;
    private static boolean inEffect;
    private static boolean suspended;
    private static int originalRender;
    private static int originalSimulation;
    private static int maxRender;
    private static int maxSimulation;

    private LivingMenuChunkLimits() {}

    static void begin(Minecraft minecraft, LivingMenuScene scene) {
        restore(minecraft);
        originalRender = minecraft.options.renderDistance().get();
        originalSimulation = minecraft.options.simulationDistance().get();
        maxRender = scene.renderDistanceChunks;
        maxSimulation = Math.max(MIN_SIMULATION_DISTANCE, scene.simulationDistanceChunks);
        if (scene.simulationDistanceChunks < MIN_SIMULATION_DISTANCE) {
            LOGGER.info("Living Main Menu: scene '{}' requested simulation distance {}; "
                    + "using Minecraft 1.20.1 minimum {} instead",
                    scene.id, scene.simulationDistanceChunks, MIN_SIMULATION_DISTANCE);
        }
        inEffect = true;
        suspended = false;
        try {
            cap(minecraft);
        } catch (RuntimeException error) {
            restore(minecraft);
            throw error;
        }
    }

    /** Restore the actual preferences before any Options/Mods submenu can save them. */
    static void suspend(Minecraft minecraft) {
        if (!inEffect || suspended) {
            return;
        }
        minecraft.options.renderDistance().set(originalRender);
        minecraft.options.simulationDistance().set(originalSimulation);
        suspended = true;
    }

    /** Read updated preferences after leaving Options, and cap only the live scene again. */
    static void resume(Minecraft minecraft) {
        if (!inEffect || !suspended) {
            return;
        }
        originalRender = minecraft.options.renderDistance().get();
        originalSimulation = minecraft.options.simulationDistance().get();
        suspended = false;
        cap(minecraft);
    }

    /** Always restore before any real singleplayer or multiplayer session starts. */
    static void restore(Minecraft minecraft) {
        if (!inEffect) {
            return;
        }
        if (!suspended) {
            minecraft.options.renderDistance().set(originalRender);
            minecraft.options.simulationDistance().set(originalSimulation);
        }
        inEffect = false;
        suspended = false;
    }

    private static void cap(Minecraft minecraft) {
        minecraft.options.renderDistance().set(Math.min(originalRender, maxRender));
        minecraft.options.simulationDistance().set(Math.min(originalSimulation, maxSimulation));
    }
}
