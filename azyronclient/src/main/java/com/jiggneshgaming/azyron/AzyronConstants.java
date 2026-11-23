package com.jiggneshgaming.azyron;

/**
 * AZYRON CLIENT — global constants.
 */
public final class AzyronConstants {

    private AzyronConstants() {
    }

    public static final String NAME = "AZYRON CLIENT";
    public static final String SHORT_NAME = "AZYRON";
    public static final String TAGLINE = "PERFORMANCE. SMOOTHNESS. STYLE.";
    public static final String DEVELOPER = "JIGNESH GAMING";
    public static final String VERSION = "2.0.0";
    public static final String EDITION = "V2";
    public static final String MINECRAFT_VERSION = "1.21.11";

    /** Community Discord invite. Edit this constant to point at your own server. */
    public static final String DISCORD_INVITE = "https://discord.gg/azyronclient";
    public static final String DISCORD_LABEL = "discord.gg/azyronclient";

    /* Theme palette (ARGB). */
    public static final int COLOR_BG_TOP = 0xFF0B0714;
    public static final int COLOR_BG_BOTTOM = 0xFF1A1030;
    public static final int COLOR_PANEL = 0xE0140D24;
    public static final int COLOR_PANEL_LIGHT = 0xEE1E1436;
    public static final int COLOR_PANEL_BORDER = 0xFF3B2A63;
    public static final int COLOR_PURPLE = 0xFF7C3AED;
    public static final int COLOR_PURPLE_BRIGHT = 0xFFA78BFA;
    public static final int COLOR_ELECTRIC = 0xFF33CCFF;
    public static final int COLOR_ELECTRIC_DEEP = 0xFF0E86C9;
    public static final int COLOR_TEXT = 0xFFF1F5F9;
    public static final int COLOR_TEXT_DIM = 0xFF9CA3AF;
    public static final int COLOR_GOOD = 0xFF4ADE80;
    public static final int COLOR_WARN = 0xFFFACC15;
    public static final int COLOR_BAD = 0xFFF87171;

    /** Blend two ARGB colors. ratio 0 = a, 1 = b. */
    public static int lerpColor(int a, int b, float ratio) {
        float t = Math.max(0F, Math.min(1F, ratio));
        int aA = (a >>> 24) & 0xFF, aR = (a >>> 16) & 0xFF, aG = (a >>> 8) & 0xFF, aB = a & 0xFF;
        int bA = (b >>> 24) & 0xFF, bR = (b >>> 16) & 0xFF, bG = (b >>> 8) & 0xFF, bB = b & 0xFF;
        int alpha = Math.round(aA + (bA - aA) * t);
        int red = Math.round(aR + (bR - aR) * t);
        int green = Math.round(aG + (bG - aG) * t);
        int blue = Math.round(aB + (bB - aB) * t);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
