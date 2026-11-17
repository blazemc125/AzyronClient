package com.jiggneshgaming.azyron.keys;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * AZYRON CLIENT — keystroke display state for the Keystrokes HUD.
 *
 * <p>Polled once per client tick from the raw window handle; no mixins.
 * Also tracks press timestamps so keys can flash on the HUD.</p>
 */
public final class KeystrokesTracker {

    public static final int KEY_W = 0;
    public static final int KEY_A = 1;
    public static final int KEY_S = 2;
    public static final int KEY_D = 3;
    public static final int KEY_SPACE = 4;
    public static final int KEY_SHIFT = 5;
    public static final int KEY_CTRL = 6;

    private static final int[] GL_KEYS = {
            GLFW.GLFW_KEY_W,
            GLFW.GLFW_KEY_A,
            GLFW.GLFW_KEY_S,
            GLFW.GLFW_KEY_D,
            GLFW.GLFW_KEY_SPACE,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            GLFW.GLFW_KEY_LEFT_CONTROL
    };

    private static final boolean[] DOWN = new boolean[GL_KEYS.length];
    private static final long[] LAST_PRESS = new long[GL_KEYS.length];

    private KeystrokesTracker() {
    }

    public static void poll() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        long handle = client.getWindow().getHandle();
        long now = System.currentTimeMillis();
        for (int i = 0; i < GL_KEYS.length; i++) {
            boolean down = GLFW.glfwGetKey(handle, GL_KEYS[i]) == GLFW.GLFW_PRESS;
            if (down && !DOWN[i]) {
                LAST_PRESS[i] = now;
            }
            DOWN[i] = down;
        }
    }

    public static boolean isDown(int key) {
        return key >= 0 && key < DOWN.length && DOWN[key];
    }

    /** Brightness flash in [0,1] — 1 right at press, fading to 0 over 220ms. */
    public static float flash(int key) {
        if (key < 0 || key >= LAST_PRESS.length) {
            return 0F;
        }
        long elapsed = System.currentTimeMillis() - LAST_PRESS[key];
        if (elapsed < 0L || elapsed > 220L) {
            return 0F;
        }
        return 1.0F - (float) elapsed / 220.0F;
    }

    public static void reset() {
        for (int i = 0; i < DOWN.length; i++) {
            DOWN[i] = false;
            LAST_PRESS[i] = 0L;
        }
    }
}
