package com.jiggneshgaming.azyron.core.format;

/**
 * AZYRON CLIENT — HUD formatting helpers.
 *
 * <p>Pure, dependency-free formatting and classification logic used by the
 * Fabric HUD layer. Unit-tested standalone.</p>
 */
public final class FormatUtils {

    private FormatUtils() {
    }

    /** Connectivity quality derived from a ping value in milliseconds. */
    public enum PingQuality {
        GOOD,
        MEDIUM,
        BAD,
        OFFLINE;

        /** ARGB colors (0xAARRGGBB) tuned for the dark AZYRON theme. */
        public int color() {
            switch (this) {
                case GOOD:
                    return 0xFF4ADE80; // emerald
                case MEDIUM:
                    return 0xFFFACC15; // amber
                case BAD:
                    return 0xFFF87171; // red
                case OFFLINE:
                default:
                    return 0xFF94A3B8; // slate
            }
        }
    }

    public static PingQuality pingQuality(int pingMs) {
        if (pingMs < 0) {
            return PingQuality.OFFLINE;
        }
        if (pingMs < 80) {
            return PingQuality.GOOD;
        }
        if (pingMs < 180) {
            return PingQuality.MEDIUM;
        }
        return PingQuality.BAD;
    }

    /** {@code "— ms"} when unknown/negative, otherwise {@code "NNN ms"}. */
    public static String formatPing(int pingMs) {
        return pingMs < 0 ? "— ms" : pingMs + " ms";
    }

    /** Traffic-light color for FPS values. */
    public static int fpsColor(int fps) {
        if (fps >= 100) {
            return 0xFF4ADE80;
        }
        if (fps >= 60) {
            return 0xFFA3E635;
        }
        if (fps >= 30) {
            return 0xFFFACC15;
        }
        return 0xFFF87171;
    }

    public static String formatFps(int fps) {
        return "FPS: " + fps;
    }

    /** Block-coordinate style: {@code "XYZ: 128 / 64 / -230"}. */
    public static String formatCoordsInt(double x, double y, double z) {
        long bx = Math.round(Math.floor(x));
        long by = Math.round(Math.floor(y));
        long bz = Math.round(Math.floor(z));
        return "XYZ: " + bx + " / " + by + " / " + bz;
    }

    /** Decimal-coordinate style with one decimal of precision. */
    public static String formatCoordsDecimal(double x, double y, double z) {
        return String.format(java.util.Locale.ROOT, "XYZ: %.1f / %.1f / %.1f", x, y, z);
    }

    /** Formats milliseconds as {@code m:ss} (potion durations). */
    public static String formatDurationMs(long durationMs) {
        if (durationMs < 0L) {
            return "0:00";
        }
        long totalSeconds = durationMs / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes + ":" + (seconds < 10L ? "0" : "") + seconds;
    }

    /** Formats ticks as {@code m:ss} (20 ticks per second). */
    public static String formatDurationTicks(int ticks) {
        if (ticks <= 0) {
            return "0:00";
        }
        return formatDurationMs((long) ticks * 50L);
    }

    /** Roman numeral for amplifier levels 0-9 (I, II, III ... X). */
    public static String roman(int value) {
        switch (value) {
            case 0:
                return "I";
            case 1:
                return "II";
            case 2:
                return "III";
            case 3:
                return "IV";
            case 4:
                return "V";
            case 5:
                return "VI";
            case 6:
                return "VII";
            case 7:
                return "VIII";
            case 8:
                return "IX";
            case 9:
                return "X";
            default:
                return Integer.toString(value + 1);
        }
    }
}
