package com.jiggneshgaming.azyron.core.hud;

/**
 * AZYRON CLIENT — HUD element geometry helpers.
 *
 * <p>Pure math for clamping HUD elements to screen bounds. Unit-tested standalone.</p>
 */
public final class HudGeometry {

    private HudGeometry() {
    }

    /** Clamp value {@code v} into [min, max]; safe when min &gt; max. */
    public static int clamp(int v, int min, int max) {
        if (min > max) {
            return min;
        }
        return Math.max(min, Math.min(max, v));
    }

    /**
     * Clamp a HUD element rectangle so it stays fully on screen.
     *
     * @param x     element left position (pixels)
     * @param y     element top position (pixels)
     * @param width element width (pixels)
     * @param height element height (pixels)
     * @param screenWidth window width (pixels)
     * @param screenHeight window height (pixels)
     * @return a two-element int array {x, y} with the clamped position
     */
    public static int[] clampToScreen(int x, int y, int width, int height,
                                      int screenWidth, int screenHeight) {
        int maxX = Math.max(0, screenWidth - Math.max(1, width));
        int maxY = Math.max(0, screenHeight - Math.max(1, height));
        int[] out = new int[2];
        out[0] = clamp(x, 0, maxX);
        out[1] = clamp(y, 0, maxY);
        return out;
    }

    /** Simple AABB overlap test. */
    public static boolean overlaps(int ax, int ay, int aw, int ah,
                                   int bx, int by, int bw, int bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }
}
