package com.jiggneshgaming.azyron;

import com.jiggneshgaming.azyron.config.AzyronConfig;
import com.jiggneshgaming.azyron.core.profile.PerformanceProfile;
import com.jiggneshgaming.azyron.crosshair.Crosshairs;
import com.jiggneshgaming.azyron.cps.CpsManager;
import com.jiggneshgaming.azyron.cosmetics.AuraManager;
import com.jiggneshgaming.azyron.hud.HudEditorScreen;
import com.jiggneshgaming.azyron.hud.HudRenderer;
import com.jiggneshgaming.azyron.keybinds.KeyBinds;
import com.jiggneshgaming.azyron.keys.KeystrokesTracker;
import com.jiggneshgaming.azyron.menu.AzyronMainMenuScreen;
import com.jiggneshgaming.azyron.menu.AzyronModsMenuScreen;
import com.jiggneshgaming.azyron.performance.DynamicFpsManager;
import com.jiggneshgaming.azyron.performance.ProfileManager;
import com.jiggneshgaming.azyron.sprint.ToggleSprint;
import com.jiggneshgaming.azyron.zoom.ZoomManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT — Fabric client entrypoint.
 *
 * <p>Wires the HUD hook, key bindings, tick handlers, the performance
 * profile system and the title-screen replacement. All risky integrations
 * are wrapped in defensive try/catch — a failure degrades one feature,
 * never the game.</p>
 */
public final class AzyronClientMod implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient");

    /** True once the AZYRON main menu has replaced the vanilla title screen. */
    private static boolean titleReplaced;

    /** World-presence tracking for disconnect cleanup. */
    private static boolean wasInWorld;

    @Override
    public void onInitializeClient() {
        LOGGER.info("AZYRON CLIENT {} — starting initialization (MC {})",
                AzyronConstants.VERSION, AzyronConstants.MINECRAFT_VERSION);

        // 1) Config — must load before anything reads settings.
        AzyronConfig.load();

        // 2) Key bindings (Right Shift opens the Mods Menu by default).
        KeyBinds.register();

        // 3) HUD hook — draws every enabled AZYRON element after vanilla HUD.
        try {
            HudElementRegistry.addLast(Identifier.of("azyronclient", "hud"),
                    (context, tickCounter) -> {
                        try {
                            HudRenderer.renderHud(context);
                            Crosshairs.render(context);
                        } catch (Throwable t) {
                            LOGGER.error("AZYRON HUD render failed", t);
                        }
                    });
        } catch (Throwable t) {
            LOGGER.error("Could not register the AZYRON HUD hook. HUD will be unavailable this session.", t);
        }

        // 4) Tick handler — input polling, sprint, zoom, auras, dynamic FPS.
        ClientTickEvents.END_CLIENT_TICK.register(AzyronClientMod::onEndTick);

        // 5) Replace the vanilla title screen with the AZYRON main menu.
        try {
            ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
                try {
                    if (titleReplaced || !(screen instanceof TitleScreen)) {
                        return;
                    }
                    titleReplaced = true;
                    client.setScreen(new AzyronMainMenuScreen());
                } catch (Throwable t) {
                    LOGGER.error("Could not open the AZYRON main menu — staying on the vanilla title.", t);
                }
            });
        } catch (Throwable t) {
            LOGGER.error("Screen replacement hook unavailable — AZYRON menus reachable via the Mods Menu key.", t);
        }

        // 6) Re-apply the persisted performance profile (options survive restarts,
        //    but a profile switch on another install must win here too).
        try {
            PerformanceProfile.Settings settings = PerformanceProfile.forId(ProfileManager.activeProfile());
            ProfileManager.applySettings(settings);
        } catch (Throwable t) {
            LOGGER.warn("Could not apply the saved performance profile at startup: {}", t.toString());
        }

        LOGGER.info("AZYRON CLIENT initialized — Right Shift opens the Mods Menu. Made by {}.",
                AzyronConstants.DEVELOPER);
    }

    private static void onEndTick(MinecraftClient client) {
        CpsManager.poll();
        KeystrokesTracker.poll();
        ZoomManager.tick();
        ToggleSprint.tick();
        DynamicFpsManager.tick();
        AuraManager.tick();

        // Right Shift — toggle the Mods Menu while in-world (no screen open).
        if (KeyBinds.menuWasPressed()) {
            try {
                net.minecraft.client.gui.screen.Screen current = com.jiggneshgaming.azyron.util.ClientRefs.currentScreen(client);
                if (current == null) {
                    client.setScreen(new AzyronModsMenuScreen(null));
                }
            } catch (Throwable t) {
                LOGGER.warn("Could not open the Mods Menu: {}", t.toString());
            }
        }

        // R — toggle sprint.
        if (KeyBinds.sprintToggleWasPressed()) {
            try {
                com.jiggneshgaming.azyron.core.config.AzyronConfigModel config = AzyronConfig.get();
                config.toggleSprint = !config.toggleSprint;
                AzyronConfig.save();
                if (client.player != null) {
                    client.player.sendMessage(
                            com.jiggneshgaming.azyron.util.TextHelper.literal(
                                    "AZYRON CLIENT: Toggle sprint " + (config.toggleSprint ? "ON" : "OFF")),
                            true);
                }
            } catch (Throwable t) {
                LOGGER.warn("Toggle sprint keybind failed: {}", t.toString());
            }
        }

        // H — HUD editor.
        if (KeyBinds.hudEditorWasPressed()) {
            try {
                client.setScreen(new HudEditorScreen());
            } catch (Throwable t) {
                LOGGER.warn("Could not open the HUD editor: {}", t.toString());
            }
        }

        // Reset polling state when leaving a world.
        boolean inWorld = client.player != null && client.world != null;
        if (!inWorld && wasInWorld) {
            CpsManager.reset();
            KeystrokesTracker.reset();
            AuraManager.reset();
        }
        wasInWorld = inWorld;
    }
}
