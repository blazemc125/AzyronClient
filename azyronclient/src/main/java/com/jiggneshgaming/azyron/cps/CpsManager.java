package com.jiggneshgaming.azyron.cps;

import com.jiggneshgaming.azyron.core.cps.CpsTracker;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * AZYRON CLIENT — polls raw mouse buttons each tick and feeds the
 * rolling CPS tracker. No mixins required.
 */
public final class CpsManager {

    private static final CpsTracker TRACKER = new CpsTracker();

    private static boolean leftDown;
    private static boolean rightDown;
    private static boolean middleDown;

    private CpsManager() {
    }

    /** Call once per client tick. */
    public static void poll() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        long handle = client.getWindow().getHandle();
        long now = System.currentTimeMillis();

        boolean left = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean middle = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;

        if (left && !leftDown) {
            TRACKER.press(CpsTracker.LEFT, now);
        }
        if (right && !rightDown) {
            TRACKER.press(CpsTracker.RIGHT, now);
        }
        if (middle && !middleDown) {
            TRACKER.press(CpsTracker.MIDDLE, now);
        }

        leftDown = left;
        rightDown = right;
        middleDown = middle;
    }

    public static int leftCps() {
        return TRACKER.cps(CpsTracker.LEFT, System.currentTimeMillis());
    }

    public static int rightCps() {
        return TRACKER.cps(CpsTracker.RIGHT, System.currentTimeMillis());
    }

    public static int middleCps() {
        return TRACKER.cps(CpsTracker.MIDDLE, System.currentTimeMillis());
    }

    public static boolean isLeftDown() {
        return leftDown;
    }

    public static boolean isRightDown() {
        return rightDown;
    }

    public static void reset() {
        TRACKER.clear();
        leftDown = false;
        rightDown = false;
        middleDown = false;
    }
}
