package com.jiggneshgaming.azyron.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT — screen navigation helpers.
 *
 * <p>Constructs the vanilla screens directly against the yarn-mapped API
 * (verified for 1.21.11). A failure is logged with a clear message instead
 * of silently doing nothing when the user presses a menu button.</p>
 */
public final class ScreenNav {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/nav");

    private ScreenNav() {
    }

    public static void openSingleplayer(MinecraftClient client, Screen parent) {
        try {
            client.setScreen(new SelectWorldScreen(parent));
        } catch (Throwable t) {
            LOGGER.error("Could not open the singleplayer screen", t);
        }
    }

    public static void openMultiplayer(MinecraftClient client, Screen parent) {
        try {
            client.setScreen(new MultiplayerScreen(parent));
        } catch (Throwable t) {
            LOGGER.error("Could not open the multiplayer screen", t);
        }
    }
}
