package com.jiggneshgaming.azyron.performance;

import com.jiggneshgaming.azyron.config.AzyronConfig;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT — Dynamic FPS (unfocused window).
 *
 * <p>When the Minecraft window loses focus, the max-FPS cap is dropped to
 * the configured unfocused value; when it regains focus, the previous cap is
 * restored. Cooperates with performance profiles by saving/restoring rather
 * than hard-setting values.</p>
 */
public final class DynamicFpsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/dynamicfps");

    private static boolean limited;
    private static int savedMaxFps;
    private static int tickCounter;

    private DynamicFpsManager() {
    }

    public static void tick() {
        // Cheap path — only run every 10 ticks.
        if (++tickCounter < 10) {
            return;
        }
        tickCounter = 0;

        if (!AzyronConfig.get().dynamicFps) {
            restoreIfNeeded();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null || client.options == null) {
            return;
        }

        boolean focused = org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(
                client.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_FOCUSED) == 1;
        if (!focused && !limited) {
            try {
                net.minecraft.client.option.SimpleOption<Integer> maxFps = client.options.getMaxFps();
                savedMaxFps = maxFps.getValue().intValue();
                int cap = Math.max(1, AzyronConfig.get().unfocusedFps);
                if (savedMaxFps > cap) {
                    maxFps.setValue(Integer.valueOf(cap));
                    limited = true;
                    LOGGER.debug("Window unfocused — capped FPS to {} (was {}).", cap, savedMaxFps);
                }
            } catch (Throwable t) {
                LOGGER.debug("Could not apply unfocused FPS cap: {}", t.toString());
            }
        } else if (focused && limited) {
            restoreIfNeeded();
        }
    }

    /** Restore the saved FPS cap (also called when the module is disabled). */
    public static void restoreIfNeeded() {
        if (!limited) {
            return;
        }
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return;
            }
            client.options.getMaxFps().setValue(Integer.valueOf(savedMaxFps));
            LOGGER.debug("Window focused — restored FPS cap to {}.", savedMaxFps);
        } catch (Throwable t) {
            LOGGER.debug("Could not restore FPS cap: {}", t.toString());
        } finally {
            limited = false;
        }
    }
}
