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
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT — Fabric client entrypoint.
 *
 * <p>Wires the HUD hook, key bindings, tick handlers, the performance
 * profile system and the title-screen replacement. Risky integrations are
 * wrapped in defensive try/catch — a failure degrades one feature, never
 * the game.</p>
 */
public final class AzyronClientMod implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient");

    /** World-presence tracking for disconnect cleanup. */
    private static boolean wasInWorld;

    /** Set once the saved performance profile has been applied (first tick). */
    private static boolean profileApplied;

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
        //    The saved performance profile is applied on the first tick:
        //    during mod initialization the client is not ready to accept
        //    option changes (applying graphics settings at that point can
        //    dereference half-initialized state and throws).
        ClientTickEvents.END_CLIENT_TICK.register(AzyronClientMod::onEndTick);

        // 5) Replace the vanilla title screen with the AZYRON main menu.
        //    Every TitleScreen init is replaced (quit-to-title included);
        //    AzyronMainMenuScreen is not a TitleScreen, so no recursion.
        //    The swap is DEFERRED out of the init event: mutating the current
        //    screen synchronously inside AFTER_INIT (nested init/resize while
        //    the title screen is still being constructed) can leave the client
        //    in a half-initialized state where the screen is set but its
        //    widgets never receive layout or input. client.execute() re-enters
        //    on the client thread after the current event completes.
        try {
            ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
                if (!(screen instanceof TitleScreen)) {
                    return;
                }
                try {
                    client.execute(() -> {
                        try {
                            if (client.currentScreen instanceof TitleScreen) {
                                client.setScreen(new AzyronMainMenuScreen());
                            }
                        } catch (Throwable t) {
                            LOGGER.error("Could not open the AZYRON main menu — staying on the vanilla title.", t);
                        }
                    });
                } catch (Throwable t) {
                    LOGGER.error("Could not schedule the AZYRON main menu swap.", t);
                }
            });
        } catch (Throwable t) {
            LOGGER.error("Screen replacement hook unavailable — AZYRON menus reachable via the Mods Menu key.", t);
        }

        LOGGER.info("AZYRON CLIENT initialized — Right Shift opens the Mods Menu. Made by {}.",
                AzyronConstants.DEVELOPER);
    }

    private static void applySavedProfileOnce(MinecraftClient client) {
        if (profileApplied) {
            return;
        }
        profileApplied = true; // one shot — never fight the user's settings later
        try {
            PerformanceProfile.Settings settings = PerformanceProfile.forId(ProfileManager.activeProfile());
            ProfileManager.applySettings(settings);
        } catch (Throwable t) {
            LOGGER.warn("Could not apply the saved performance profile: {}", t.toString());
        }
    }

    private static void onEndTick(MinecraftClient client) {
        // 0) First tick — the client is fully constructed now.
        applySavedProfileOnce(client);

        tickSafely("CPS polling", CpsManager::poll);
        tickSafely("Keystroke tracking", KeystrokesTracker::poll);
        tickSafely("Zoom", ZoomManager::tick);
        tickSafely("Toggle sprint", ToggleSprint::tick);
        tickSafely("Dynamic FPS", DynamicFpsManager::tick);
        tickSafely("Cosmetic auras", AuraManager::tick);

        // Right Shift — toggle the Mods Menu (open in-world, close when open).
        if (KeyBinds.menuWasPressed()) {
            try {
                Screen current = currentScreen(client);
                if (current == null) {
                    client.setScreen(new AzyronModsMenuScreen(null));
                } else if (current instanceof AzyronModsMenuScreen) {
                    ((AzyronModsMenuScreen) current).goBack();
                }
            } catch (Throwable t) {
                LOGGER.warn("Could not toggle the Mods Menu: {}", t.toString());
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

        // H — HUD editor. Toggles: opens over the HUD, closes the editor when open.
        if (KeyBinds.hudEditorWasPressed()) {
            try {
                Screen current = currentScreen(client);
                if (current == null) {
                    client.setScreen(new HudEditorScreen());
                } else if (current instanceof HudEditorScreen) {
                    closeToSafeScreen(client, current);
                }
            } catch (Throwable t) {
                LOGGER.warn("Could not toggle the HUD editor: {}", t.toString());
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

    private static Screen currentScreen(MinecraftClient client) {
        return com.jiggneshgaming.azyron.util.ClientRefs.currentScreen(client);
    }

    /** Close a screen that carries no navigation target — return to the HUD or the AZYRON main menu. */
    private static void closeToSafeScreen(MinecraftClient client, Screen current) {
        if (client.world != null) {
            client.setScreen(null);
        } else if (!(current instanceof AzyronMainMenuScreen)) {
            client.setScreen(new AzyronMainMenuScreen());
        }
    }

    private static void tickSafely(String feature, Runnable tick) {
        try {
            tick.run();
        } catch (Throwable t) {
            LOGGER.warn("AZYRON feature '{}' failed this tick: {}", feature, t.toString());
        }
    }
}
