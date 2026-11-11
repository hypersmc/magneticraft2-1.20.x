package com.magneticraft2.client.menu;

import com.magneticraft2.common.utils.Magneticraft2ConfigClient;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsMainScreen;
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
    // 1.5 degrees/second at 20 client ticks per second: four-minute full turn.
    private static final float CAMERA_TURN_DEGREES_PER_TICK = 0.075F;
    private static Screen menuDuringLoading;
    private static boolean loading;
    private static boolean active;
    private static boolean failedThisSession;
    private static boolean navigatingAway;
    private static Screen pendingUnloadDestination;
    private static long loadStartedAt;
    private static Path expectedRuntimePath;

    private LivingMenuController() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (pendingUnloadDestination != null) {
            // A title button has already opened its destination. Finish the current
            // input dispatch before tearing down the integrated server; doing this
            // synchronously from TitleScreen.mouseClicked caused navigation lockups.
            Screen destination = pendingUnloadDestination;
            pendingUnloadDestination = null;
            try {
                if (minecraft.level != null) {
                    minecraft.clearLevel(destination);
                }
            } catch (Exception error) {
                failedThisSession = true;
                LOGGER.error("Failed to close the living menu's integrated world", error);
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
                    minecraft.setScreen(new LiveTitleScreen());
                    LOGGER.info("Living Main Menu is running a real integrated-server world");
                } else {
                    loading = false;
                    menuDuringLoading = null;
                    failedThisSession = true;
                    LOGGER.warn("Living Main Menu did not join its temporary world; keeping normal gameplay");
                }
            } else if (System.nanoTime() - loadStartedAt > LOAD_TIMEOUT_NANOS) {
                // Do not tear down a potentially still-loading world from a timeout.
                loading = false;
                menuDuringLoading = null;
                failedThisSession = true;
                LOGGER.warn("Living Main Menu world was not ready after two minutes; disabled for this session");
            }
            return;
        }
        if (active) {
            if (minecraft.level == null || minecraft.getSingleplayerServer() == null) {
                active = false;
            } else if (minecraft.screen instanceof LiveTitleScreen && minecraft.player != null) {
                // Turn the real client viewpoint in the disposable demonstration world.
                minecraft.player.setYRot(minecraft.player.getYRot() + CAMERA_TURN_DEGREES_PER_TICK);
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
            }
            return;
        }
        if (failedThisSession || !Magneticraft2ConfigClient.GENERAL.livingMainMenu.get()
                || minecraft.level != null || !(minecraft.screen instanceof TitleScreen)
                || minecraft.screen instanceof LiveTitleScreen) {
            return;
        }
        Path savesDirectory = minecraft.getLevelSource().getBaseDir();
        if (!LivingMenuWorldCopy.sourceExists(savesDirectory)) {
            return;
        }
        try {
            expectedRuntimePath = LivingMenuWorldCopy.prepare(savesDirectory);
            menuDuringLoading = minecraft.screen;
            loading = true;
            loadStartedAt = System.nanoTime();
            minecraft.createWorldOpenFlows().loadLevel(minecraft.screen, LivingMenuWorldCopy.RUNTIME_ID);
        } catch (Exception error) {
            loading = false;
            menuDuringLoading = null;
            failedThisSession = true;
            LOGGER.error("Could not open the real Living Main Menu world; leaving the vanilla menu available", error);
        }
    }

    /** Called AFTER a vanilla TitleScreen button has opened its destination. */
    static void leaveMenuFor(Screen destination) {
        if (!active || destination == null) {
            return;
        }

        // Options, Mods and other non-gameplay submenus can reuse the live
        // integrated server. Their normal Back button returns immediately to
        // the living menu without another shutdown/copy/world load.
        if (!(destination instanceof SelectWorldScreen
                || destination instanceof JoinMultiplayerScreen
                || destination instanceof RealmsMainScreen)) {
            return;
        }

        // The actual world/network selection flows must never run while our
        // temporary integrated server is still connected. Unload on next tick
        // instead of re-entering setScreen()/clearLevel() during button dispatch.
        navigatingAway = true;
        active = false;
        pendingUnloadDestination = destination;
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
        // A vanilla menu page can keep its original TitleScreen as its parent.
        // Never display an empty live-background screen after leaving the demo world.
        if (event.getNewScreen() instanceof LiveTitleScreen && !active) {
            event.setNewScreen(new TitleScreen());
        } else if (event.getNewScreen() instanceof TitleScreen
                && !(event.getNewScreen() instanceof LiveTitleScreen)
                && active && Minecraft.getInstance().level != null) {
            // A submenu might construct a new vanilla title rather than retain
            // its original parent. Reuse the still-loaded menu world either way.
            event.setNewScreen(new LiveTitleScreen());
        }
    }
}
