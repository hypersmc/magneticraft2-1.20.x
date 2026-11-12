package com.magneticraft2.client.menu;

import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Bootstrap-only spawn chunk budget for the disposable Living Main Menu world.
 *
 * A Minecraft 1.20.1 server always creates a level-11 START ticket and waits
 * for 441 ticking chunks before a player can join. The accompanying client-only
 * Mixin replaces BOTH constants for the exact marked runtime save; all other
 * integrated and dedicated worlds retain vanilla startup behavior.
 *
 * One shared immutable snapshot is published from the client before loadLevel,
 * then read by the integrated server thread during prepareLevels.
 */
public final class LivingMenuSpawnBudget {
    private static volatile Budget pending;

    private LivingMenuSpawnBudget() {}

    static void arm(Path runtimePath, int radiusChunks) {
        if (radiusChunks < 2 || radiusChunks > 8) {
            throw new IllegalArgumentException("Menu spawn radius must be 2..8 chunks");
        }
        pending = new Budget(runtimePath.toAbsolutePath().normalize(), radiusChunks);
    }

    static void disarm() {
        pending = null;
    }

    /**
     * Called from Mixin in MinecraftServer.prepareLevels. The vanilla constant
     * is returned without changes for every world other than our exact marked,
     * currently armed disposable runtime.
     */
    public static int ticketLevel(MinecraftServer server, int vanillaLevel) {
        Budget budget = matchingBudget(server);
        return budget == null ? vanillaLevel : budget.radiusChunks() + 1;
    }

    public static int expectedTickingChunks(MinecraftServer server, int vanillaCount) {
        Budget budget = matchingBudget(server);
        if (budget == null) {
            return vanillaCount;
        }
        int diameter = 2 * budget.radiusChunks() + 1;
        return diameter * diameter;
    }

    private static Budget matchingBudget(MinecraftServer server) {
        Budget budget = pending;
        if (budget == null || !(server instanceof IntegratedServer)) {
            return null;
        }

        Path serverWorld = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        if (!budget.runtimePath().equals(serverWorld)) {
            return null;
        }
        // Path matching alone is insufficient. Refuse to touch a normal save
        // unless it is the generated, explicitly marked disposable runtime.
        if (!Files.isRegularFile(
                serverWorld.resolve(LivingMenuWorldCopy.RUNTIME_MARKER), LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        return budget;
    }

    private record Budget(Path runtimePath, int radiusChunks) {}
}
