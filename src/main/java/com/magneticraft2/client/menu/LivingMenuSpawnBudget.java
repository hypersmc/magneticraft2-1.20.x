package com.magneticraft2.client.menu;

import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Bootstrap-only spawn chunk budget for the disposable Living Main Menu world.
 *
 * A Minecraft 1.20.1 server always requests a radius-11 START ticket and waits
 * for 441 ticking chunks before a player can join. The accompanying client-only
 * Mixin replaces BOTH constants for the exact marked runtime save; all other
 * integrated and dedicated worlds retain vanilla startup behavior.
 *
 * One shared immutable snapshot is published from the client before loadLevel,
 * then read by the integrated server thread during prepareLevels.
 */
public final class LivingMenuSpawnBudget {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile Budget pending;

    private LivingMenuSpawnBudget() {}

    static void arm(Path runtimePath, int radiusChunks) {
        if (radiusChunks < 2 || radiusChunks > 8) {
            throw new IllegalArgumentException("Menu spawn radius must be 2..8 chunks");
        }
        pending = new Budget(runtimePath.toAbsolutePath().normalize(), radiusChunks,
                new AtomicBoolean(), new AtomicBoolean());
    }

    static void disarm() {
        pending = null;
    }

    /**
     * Called from Mixin in MinecraftServer.prepareLevels. The vanilla constant
     * is returned without changes for every world other than our exact marked,
     * currently armed disposable runtime.
     */
    public static int regionRadius(MinecraftServer server, int vanillaRadius) {
        Budget budget = matchingBudget(server);
        if (budget == null) {
            return vanillaRadius;
        }
        int effective = budget.radiusChunks() + 1;
        if (budget.ticketLogged().compareAndSet(false, true)) {
            LOGGER.info("Living Main Menu: spawn region-ticket Mixin ACTIVE; radius {} -> {}",
                    vanillaRadius, effective);
        }
        return effective;
    }

    public static int expectedTickingChunks(MinecraftServer server, int vanillaCount) {
        Budget budget = matchingBudget(server);
        if (budget == null) {
            return vanillaCount;
        }
        int diameter = 2 * budget.radiusChunks() + 1;
        int effective = diameter * diameter;
        if (budget.targetLogged().compareAndSet(false, true)) {
            LOGGER.info("Living Main Menu: spawn completion-target Mixin ACTIVE; "
                    + "required ticking chunks {} -> {}", vanillaCount, effective);
        }
        return effective;
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

    private record Budget(Path runtimePath, int radiusChunks,
                          AtomicBoolean ticketLogged, AtomicBoolean targetLogged) {}
}
