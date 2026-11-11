package com.magneticraft2.client.menu;

import com.magneticraft2.common.utils.Magneticraft2ConfigClient;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.network.chat.Component;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Manages a real integrated-server world behind the title-screen widgets.
 * No block or machine behavior is emulated by the menu renderer.
 */
@Mod.EventBusSubscriber(modid = "magneticraft2", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LivingMenuController {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long LOAD_TIMEOUT_NANOS = 120_000_000_000L;
    private static final long EXIT_TIMEOUT_NANOS = 30_000_000_000L;
    // Per-scene camera speed is in degrees/second. Gameplay stays at normal 20 TPS.
    private static Screen menuDuringLoading;
    private static boolean loading;
    private static boolean active;
    private static boolean failedThisSession;
    private static boolean navigatingAway;
    private static NavigationTarget requestedNavigation;
    private static NavigationTarget awaitingWorldRelease;
    private static long exitStartedAt;

    enum NavigationTarget {
        SINGLEPLAYER, MULTIPLAYER, REALMS, QUIT
    }
    private static long loadStartedAt;
    private static Path expectedRuntimePath;
    private static LivingMenuScene selectedScene;
    private static boolean sceneLookupDone;

    private LivingMenuController() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (requestedNavigation != null) {
            NavigationTarget destination = requestedNavigation;
            requestedNavigation = null;
            awaitingWorldRelease = destination;
            exitStartedAt = System.nanoTime();
            navigatingAway = true;
            active = false;
            loading = false;
            menuDuringLoading = null;
            selectedScene = null;
            LOGGER.info("Living Main Menu: {} requested; closing the disposable world", destination);
            try {
                // Match Minecraft 1.20.1's PauseScreen.onDisconnect:
                // ClientLevel.disconnect() tells the local server to stop.
                // clearLevel() alone only waits for shutdown and can hang
                // indefinitely while the integrated server keeps ticking.
                if (minecraft.level != null) {
                    minecraft.level.disconnect();
                }
                // Only now wait for the real server and release its save lock.
                // Keep a non-interactive screen during the transition.
                minecraft.clearLevel(new GenericDirtMessageScreen(
                        Component.literal("Closing Magneticraft2 menu world...")));
            } catch (Exception error) {
                // Some mods can throw during integrated-server teardown. Keep
                // the player's requested destination queued; we can proceed if
                // the world actually finishes closing despite the exception.
                LOGGER.error("Living Main Menu: error while closing the scene for {}. "
                        + "Waiting for full server and save-lock release", destination, error);
            }
            // Vanilla clearLevel usually waits for the server itself. If it has
            // already completed, open the requested screen on THIS click.
            tryFinishNavigation(minecraft);
            return;
        }
        if (awaitingWorldRelease != null) {
            if (!tryFinishNavigation(minecraft)
                    && System.nanoTime() - exitStartedAt > EXIT_TIMEOUT_NANOS) {
                NavigationTarget blocked = awaitingWorldRelease;
                boolean clientWorldPresent = minecraft.level != null;
                boolean serverPresent = minecraft.getSingleplayerServer() != null;
                boolean saveLocked = !isRuntimeSaveUnlocked(minecraft.getLevelSource().getBaseDir());
                LOGGER.error("Living Main Menu: {} could not open after shutdown; "
                                + "clientWorldPresent={}, integratedServerPresent={}, runtimeSaveLocked={}",
                        blocked, clientWorldPresent, serverPresent, saveLocked);
                awaitingWorldRelease = null;
                failedThisSession = true;
                navigatingAway = false;
                // Do not silently lose the selection and pretend the vanilla
                // title is its result. Report the blocked handoff explicitly.
                minecraft.setScreen(new AlertScreen(
                        minecraft::stop,
                        Component.literal("Living Main Menu could not close"),
                        Component.literal("The temporary world did not shut down safely. "
                                + "Check latest.log, then restart Minecraft before loading another world.")));
            }
            return;
        }
        if (loading) {
            if (minecraft.player != null && minecraft.level != null && minecraft.getSingleplayerServer() != null) {
                Path loadedWorld = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT)
                        .toAbsolutePath().normalize();
                if (expectedRuntimePath.equals(loadedWorld)) {
                    loading = false;
                    menuDuringLoading = null;
                    active = true;
                    if (selectedScene != null) {
                        selectedScene.apply(minecraft);
                    }
                    minecraft.setScreen(new LiveTitleScreen());
                    LOGGER.info("Living Main Menu running genuine scene '{}'",
                            selectedScene != null ? selectedScene.id : "unknown");
                } else {
                    loading = false;
                    menuDuringLoading = null;
                    selectedScene = null;
                    failedThisSession = true;
                    LOGGER.warn("Living Main Menu did not join its temporary world; keeping normal gameplay");
                }
            } else if (System.nanoTime() - loadStartedAt > LOAD_TIMEOUT_NANOS) {
                // Do not tear down a potentially still-loading world from a timeout.
                loading = false;
                menuDuringLoading = null;
                selectedScene = null;
                failedThisSession = true;
                LOGGER.warn("Living Main Menu world was not ready after two minutes; disabled for this session");
            }
            return;
        }
        if (active) {
            if (minecraft.level == null || minecraft.getSingleplayerServer() == null) {
                active = false;
            } else if (minecraft.screen instanceof LiveTitleScreen && minecraft.player != null) {
                // Move the real client viewpoint only. Machines still tick on the server.
                if (selectedScene != null && selectedScene.rotate) {
                    minecraft.player.setYRot(minecraft.player.getYRot()
                            + selectedScene.degreesPerSecond / 20.0F);
                }
            }
            return;
        }
        if (navigatingAway) {
            // Do not attempt another world open while Singleplayer, Multiplayer
            // or Realms menus are on screen. Once they return to a clean title,
            // re-arm the demo for the FOLLOWING tick, not this navigation tick.
            if (minecraft.level == null && minecraft.screen instanceof TitleScreen
                    && !(minecraft.screen instanceof LiveTitleScreen)) {
                navigatingAway = false;
                sceneLookupDone = false;
            }
            return;
        }
        if (failedThisSession || !Magneticraft2ConfigClient.GENERAL.livingMainMenu.get()
                || minecraft.level != null || !(minecraft.screen instanceof TitleScreen)
                || minecraft.screen instanceof LiveTitleScreen) {
            return;
        }
        // Never enumerate scene directories or parse JSON on every client tick.
        if (sceneLookupDone) {
            return;
        }
        sceneLookupDone = true;
        Path savesDirectory = minecraft.getLevelSource().getBaseDir();
        LivingMenuScene scene = LivingMenuScene.select(minecraft.gameDirectory.toPath(), savesDirectory);
        if (scene == null) {
            return;
        }
        try {
            expectedRuntimePath = LivingMenuWorldCopy.prepare(savesDirectory, scene);
            selectedScene = scene;
            menuDuringLoading = minecraft.screen;
            loading = true;
            loadStartedAt = System.nanoTime();
            minecraft.createWorldOpenFlows().loadLevel(minecraft.screen, LivingMenuWorldCopy.RUNTIME_ID);
        } catch (Exception error) {
            loading = false;
            selectedScene = null;
            menuDuringLoading = null;
            failedThisSession = true;
            LOGGER.error("Could not open the real Living Main Menu world; leaving the vanilla menu available", error);
        }
    }

    /**
     * Completes the original button action only once the genuine server and
     * its disposable save have fully closed. Returns false while still waiting.
     */
    private static boolean tryFinishNavigation(Minecraft minecraft) {
        NavigationTarget destination = awaitingWorldRelease;
        if (destination == null || minecraft.level != null
                || minecraft.getSingleplayerServer() != null
                || !isRuntimeSaveUnlocked(minecraft.getLevelSource().getBaseDir())) {
            return false;
        }

        try {
            LivingMenuWorldCopy.cleanupRuntime(minecraft.getLevelSource().getBaseDir());
        } catch (IOException error) {
            // A marked runtime may fail cleanup because of permissions, but
            // its lock has been released. Do not delete other saves.
            LOGGER.warn("Living Main Menu: could not clean up disposable world after shutdown", error);
        }

        try {
            if (destination == NavigationTarget.QUIT) {
                awaitingWorldRelease = null;
                LOGGER.info("Living Main Menu: stopped the scene, quitting");
                minecraft.stop();
                return true;
            }

            Screen parent = new TitleScreen();
            Screen next = switch (destination) {
                case SINGLEPLAYER -> new SelectWorldScreen(parent);
                case MULTIPLAYER -> new JoinMultiplayerScreen(parent);
                case REALMS -> new RealmsMainScreen(parent);
                case QUIT -> throw new IllegalStateException("Quit handled above");
            };
            minecraft.setScreen(next);
            if (minecraft.screen != next) {
                throw new IllegalStateException("Requested screen " + next.getClass().getSimpleName()
                        + " was not installed; current screen: "
                        + (minecraft.screen == null ? "null" : minecraft.screen.getClass().getSimpleName()));
            }
            awaitingWorldRelease = null;
            LOGGER.info("Living Main Menu: opened {} automatically after {} ms",
                    destination, (System.nanoTime() - exitStartedAt) / 1_000_000L);
            return true;
        } catch (RuntimeException error) {
            awaitingWorldRelease = null;
            failedThisSession = true;
            navigatingAway = false;
            LOGGER.error("Living Main Menu: the scene closed, but {} failed to open", destination, error);
            minecraft.setScreen(new AlertScreen(
                    () -> minecraft.setScreen(new TitleScreen()),
                    Component.literal("Living Main Menu navigation failed"),
                    Component.literal("Could not open " + destination
                            + ". Check latest.log for the reason.")));
            return true;
        }
    }

    static void requestNavigation(NavigationTarget destination) {
        // Called by the actual title buttons before any destination screen is
        // constructed. No world selection/connection UI may run until release.
        if (active && requestedNavigation == null && awaitingWorldRelease == null) {
            requestedNavigation = destination;
        }
    }

    private static NavigationTarget targetFor(Screen destination) {
        if (destination instanceof SelectWorldScreen) {
            return NavigationTarget.SINGLEPLAYER;
        }
        if (destination instanceof JoinMultiplayerScreen) {
            return NavigationTarget.MULTIPLAYER;
        }
        if (destination instanceof RealmsMainScreen) {
            return NavigationTarget.REALMS;
        }
        return null;
    }

    /**
     * A save can outlive the client-side level during integrated-server shutdown.
     * Only touch the disposable generated runtime lock, never a user's save.
     */
    private static boolean isRuntimeSaveUnlocked(Path savesDirectory) {
        Path lockPath = savesDirectory.resolve(LivingMenuWorldCopy.RUNTIME_ID).resolve("session.lock");
        if (!Files.isRegularFile(lockPath, LinkOption.NOFOLLOW_LINKS)) {
            return true;
        }
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.WRITE)) {
            try (FileLock lock = channel.tryLock()) {
                return lock != null;
            }
        } catch (OverlappingFileLockException exception) {
            return false;
        } catch (IOException exception) {
            LOGGER.warn("Cannot check living menu runtime session.lock yet: {}", exception.toString());
            return false;
        }
    }

    private static boolean isLiveMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        // The same disposable world may remain loaded beneath Options / Mods.
        return active && minecraft.level != null && minecraft.getSingleplayerServer() != null;
    }

    @SubscribeEvent
    public static void onGameplayHud(RenderGuiEvent.Pre event) {
        if (isLiveMenu()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        if (isLiveMenu()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerHand(RenderHandEvent event) {
        if (isLiveMenu()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        if (isLiveMenu()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLoadingScreenRender(ScreenEvent.Render.Pre event) {
        if (!loading || menuDuringLoading == null) {
            return;
        }
        Screen shown = event.getScreen();
        if (!(shown instanceof GenericDirtMessageScreen
                || shown instanceof LevelLoadingScreen
                || shown instanceof ReceivingLevelScreen
                || shown instanceof ProgressScreen)) {
            return;
        }

        // Keep Minecraft's real loading screen instance and its tick/packet logic.
        // Replace only what is drawn: the existing title stays visible until the
        // integrated-server world has actually connected and loaded.
        // TitleScreen's panorama advances with the render delta. Zero keeps
        // the original panorama still while the REAL demo world starts.
        menuDuringLoading.render(event.getGuiGraphics(), -1000, -1000, 0.0F);
        event.getGuiGraphics().fill(0, 0, shown.width, shown.height, 0x30000000);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        Screen next = event.getNewScreen();
        // The only safe time to intercept selection is BEFORE screen change:
        // otherwise Minecraft can start a real join while the menu's integrated
        // server is still alive, leaving its loading screen stuck at 100%.
        NavigationTarget target = targetFor(next);
        if (active && event.getCurrentScreen() instanceof LiveTitleScreen && target != null) {
            // Fallback for other mods opening selection screens programmatically.
            // The vanilla title buttons are intercepted earlier, before constructors.
            event.setCanceled(true);
            requestNavigation(target);
            return;
        }

        if (next instanceof LiveTitleScreen && !active) {
            event.setNewScreen(new TitleScreen());
        } else if (next instanceof TitleScreen
                && !(next instanceof LiveTitleScreen)
                && active && Minecraft.getInstance().level != null) {
            // Ordinary Options/Mods Back actions reuse the same still-running
            // world, without another integrated server startup.
            event.setNewScreen(new LiveTitleScreen());
        }
    }
}
