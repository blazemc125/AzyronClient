package com.jiggneshgaming.azyron.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * AZYRON CLIENT — resilient screen navigation.
 *
 * <p>Vanilla menu screen constructor signatures shift between Minecraft
 * versions. This helper tries a list of candidate constructors reflectively
 * and logs a clear warning when none match, instead of crashing the game.</p>
 */
public final class ScreenNav {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/nav");

    private ScreenNav() {
    }

    public static void openSingleplayer(MinecraftClient client, Screen parent) {
        Screen screen = tryCreate("net.minecraft.client.gui.screen.world.SelectWorldScreen", parent);
        if (screen != null) {
            client.setScreen(screen);
        } else {
            LOGGER.warn("Could not open the singleplayer screen — vanilla screen class not found.");
        }
    }

    public static void openMultiplayer(MinecraftClient client, Screen parent) {
        Screen screen = tryCreate("net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen", parent);
        if (screen != null) {
            client.setScreen(screen);
        } else {
            LOGGER.warn("Could not open the multiplayer screen — vanilla screen class not found.");
        }
    }

    private static final Class<?>[][] CTOR_SHAPES = {
            {Screen.class},   // new SelectWorldScreen(parentScreen)
            {}                // new SelectWorldScreen() — older shapes
    };

    private static Screen tryCreate(String className, Screen parent) {
        try {
            Class<?> raw = Class.forName(className);
            for (Class<?>[] shape : CTOR_SHAPES) {
                try {
                    java.lang.reflect.Constructor<?> ctor = raw.getDeclaredConstructor(shape);
                    ctor.setAccessible(true);
                    Object[] args = shape.length == 1 ? new Object[]{parent} : new Object[0];
                    Object created = ctor.newInstance(args);
                    if (created instanceof Screen) {
                        return (Screen) created;
                    }
                } catch (NoSuchMethodException | SecurityException e) {
                    // try the next constructor shape
                } catch (Throwable t) {
                    LOGGER.warn("Failed to instantiate {} ({} arg ctor): {}",
                            className, shape.length, t.toString());
                    return null;
                }
            }
            LOGGER.warn("No compatible constructor found for {} (tried {} and no-arg).",
                    className, "screen-parent");
            return null;
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Vanilla screen class {} is unavailable on this Minecraft version.", className);
            return null;
        }
    }
}
