package com.magneticraft2.client.menu;

import com.magneticraft2.common.utils.Magneticraft2ConfigClient;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
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
    private static boolean loading;
    private static boolean active;
    private static boolean failedThisSession;
    private static boolean navigatingAway;
    private static long loadStartedAt;
    private static Path expectedRuntimePath;

    private LivingMenuController() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (loading) {
            if (minecraft.player != null && minecraft.level != null && minecraft.getSingleplayerServer() != null) {
                Path loadedWorld = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT)
                        .toAbsolutePath().normalize();
                if (expectedRuntimePath.equals(loadedWorld)) {
                    loading = false;
                    active = true;
                    minecraft.setScreen(new LiveTitleScreen());
                    LOGGER.info("Living Main Menu is running a real integrated-server world");
                } else {
                    loading = false;
                    failedThisSession = true;
                    LOGGER.warn("Living Main Menu did not join its temporary world; keeping normal gameplay");
                }
            } else if (System.nanoTime() - loadStartedAt > LOAD_TIMEOUT_NANOS) {
                // Do not tear down a potentially still-loading world from a timeout.
                loading = false;
                failedThisSession = true;
                LOGGER.warn("Living Main Menu world was not ready after two minutes; disabled for this session");
            }
            return;
        }
        if (active) {
            if (minecraft.level == null || minecraft.getSingleplayerServer() == null) {
                active = false;
            }
            return;
        }
        if (navigatingAway) {
            if (minecraft.screen != null && !(minecraft.screen instanceof TitleScreen)) {
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
            loading = true;
            loadStartedAt = System.nanoTime();
            minecraft.createWorldOpenFlows().loadLevel(minecraft.screen, LivingMenuWorldCopy.RUNTIME_ID);
        } catch (Exception error) {
            loading = false;
            failedThisSession = true;
            LOGGER.error("Could not open the real Living Main Menu world; leaving the vanilla menu available", error);
        }
    }

    /** Called after a vanilla TitleScreen button has opened its destination. */
    static void leaveMenuFor(Screen destination) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active || destination == null) {
            return;
        }
        navigatingAway = true;
        active = false;
        // This stops the actual integrated server before showing a normal menu page.
        minecraft.clearLevel(destination);
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        // A vanilla menu page can keep its original TitleScreen as its parent.
        // Never display an empty live-background screen after leaving the demo world.
        if (event.getNewScreen() instanceof LiveTitleScreen && !active) {
            event.setNewScreen(new TitleScreen());
        }
    }
}
