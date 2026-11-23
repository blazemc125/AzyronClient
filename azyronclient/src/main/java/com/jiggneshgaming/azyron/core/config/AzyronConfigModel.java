package com.jiggneshgaming.azyron.core.config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AZYRON CLIENT — serializable configuration model.
 *
 * <p>This POJO is deliberately free of Minecraft/Fabric types so it can be
 * (de)serialized with any JSON binding and unit-tested standalone.
 * Unknown enum-like strings are tolerated and clamped by {@link #validate()}.</p>
 */
public final class AzyronConfigModel {

    /** Crosshair styles offered by the custom crosshair module. */
    public enum CrosshairStyle {
        VANILLA,
        CLASSIC,
        PLUS,
        DOT,
        CIRCLE,
        T_SHAPE;

        public static CrosshairStyle fromString(String raw) {
            if (raw == null) {
                return CLASSIC;
            }
            try {
                return CrosshairStyle.valueOf(raw.trim().toUpperCase().replace('-', '_'));
            } catch (IllegalArgumentException e) {
                return CLASSIC;
            }
        }
    }

    /** Cosmetic aura effects rendered client-side around the player. */
    public enum Aura {
        NONE,
        END_SPARKLE,
        SOUL_FLAME,
        EMBER,
        SNOWFLAKE,
        VOID_AURA,
        LIGHTNING_AURA;

        public static Aura fromString(String raw) {
            if (raw == null) {
                return NONE;
            }
            try {
                return Aura.valueOf(raw.trim().toUpperCase().replace('-', '_'));
            } catch (IllegalArgumentException e) {
                return NONE;
            }
        }
    }

    /** Position of a HUD element, in pixels relative to the scaled window. */
    public static final class HudPosition {
        public int x;
        public int y;
        /** Per-element scale (0.6 - 2.0). Old configs deserialize as 0.0 -> normalized to 1.0. */
        public double scale = 1.0D;

        public HudPosition() {
            this(4, 4);
        }

        public HudPosition(int x, int y) {
            this.x = x;
            this.y = y;
            this.scale = 1.0D;
        }

        public HudPosition copy() {
            HudPosition p = new HudPosition(x, y);
            p.scale = scale;
            return p;
        }

        /** Effective per-element scale, normalizing legacy 0.0 values. */
        public double effectiveScale() {
            return scale <= 0.0D ? 1.0D : clampDouble(scale, 0.6D, 2.0D);
        }
    }

    /* -------------------------------------------------- profile --- */
    public String profile = "BALANCED";

    /* ------------------------------------------------------ HUD --- */
    /** Per-element visibility, keyed by HUD element id. */
    public Map<String, Boolean> hud = defaultHud();
    /** Per-element positions, keyed by HUD element id. */
    public Map<String, HudPosition> positions = defaultPositions();
    public double hudScale = 1.0D;
    public double backgroundOpacity = 0.55D;

    /* -------------------------------------------------- modules --- */
    /** Open extension point for module toggles persisted in JSON. */
    public Map<String, Boolean> modules = new HashMap<String, Boolean>();
    public boolean toggleSprint = true;
    public boolean dynamicFps = true;
    /** FPS cap applied when the game window is unfocused (Dynamic FPS). */
    public int unfocusedFps = 15;

    /* ---------------------------------------------------- zoom --- */
    public boolean zoomEnabled = true;
    public double zoomLevel = 4.0D;
    public boolean smoothZoom = true;

    /* ----------------------------------------------- crosshair --- */
    public String crosshairStyle = "CLASSIC";
    public int crosshairColor = 0xFF33CCFF;
    public double crosshairSize = 1.0D;
    public boolean hideVanillaCrosshair = true;

    /* ------------------------------------------------ cosmetics --- */
    public String aura = "NONE";

    /* --------------------------------------------------- cosmetics/UI --- */
    public boolean animations = true;
    /** Animation quality preset for AZYRON visuals: LOW / MEDIUM / HIGH. */
    public String animationQuality = "MEDIUM";
    /** Particle aura spawn multiplier (0.5x - 2.5x). */
    public double auraIntensity = 1.0D;
    /** Halves aura particle spawning for weaker devices. */
    public boolean lowParticleMode = false;

    /* -------------------------------------------------------- V2 HUD --- */
    /** Accent color used by HUD panels, keystrokes and borders (ARGB). */
    public int hudAccent = 0xFF33CCFF;

    /* -------------------------------------------------------- V2 zoom -- */
    /** Zoom transition speed multiplier (smoothness vs snappiness). */
    public double zoomSensitivity = 1.0D;
    /** true = hold key to zoom; false = press to toggle zoom on/off. */
    public boolean zoomHoldMode = true;
    /** Zoom key as a GLFW key code from the supported preset list. */
    public int zoomKey = 67; // GLFW_KEY_C

    /* --------------------------------------------------- V2 crosshair -- */
    /** Thickness multiplier applied to every crosshair line. */
    public double crosshairThickness = 1.0D;
    /** Gap multiplier (distance between crosshair and center). */
    public double crosshairGap = 1.0D;
    /** Draw a dark outline behind crosshair strokes. */
    public boolean crosshairOutline = true;
    /** Crosshair spreads/contracts with movement. */
    public boolean crosshairDynamic = false;

    /* ------------------------------------------------------ misc --- */
    public boolean firstRun = true;

    /* ---------------------------------------------------------- */
    /* Defaults                                                    */
    /* ---------------------------------------------------------- */

    public static Map<String, Boolean> defaultHud() {
        Map<String, Boolean> map = new LinkedHashMap<String, Boolean>();
        map.put("fps", Boolean.TRUE);
        map.put("cps", Boolean.TRUE);
        map.put("ping", Boolean.TRUE);
        map.put("coordinates", Boolean.TRUE);
        map.put("keystrokes", Boolean.TRUE);
        map.put("armor", Boolean.TRUE);
        map.put("potions", Boolean.TRUE);
        map.put("sprint_indicator", Boolean.TRUE);
        map.put("zoom_indicator", Boolean.TRUE);
        map.put("direction", Boolean.TRUE);
        map.put("clock", Boolean.TRUE);
        map.put("biome", Boolean.TRUE);
        map.put("player_status", Boolean.TRUE);
        return map;
    }

    public static Map<String, HudPosition> defaultPositions() {
        Map<String, HudPosition> map = new LinkedHashMap<String, HudPosition>();
        map.put("fps", new HudPosition(6, 6));
        map.put("cps", new HudPosition(6, 30));
        map.put("ping", new HudPosition(6, 54));
        map.put("coordinates", new HudPosition(6, 78));
        map.put("keystrokes", new HudPosition(6, 120));
        map.put("armor", new HudPosition(6, 280));
        map.put("potions", new HudPosition(6, 340));
        map.put("sprint_indicator", new HudPosition(6, 420));
        map.put("zoom_indicator", new HudPosition(6, 444));
        map.put("direction", new HudPosition(6, 468));
        map.put("clock", new HudPosition(6, 492));
        map.put("biome", new HudPosition(6, 516));
        map.put("player_status", new HudPosition(6, 540));
        return map;
    }

    /** Visibility of a HUD element; unknown ids default to visible. */
    public boolean isHudVisible(String id) {
        Boolean value = hud.get(id);
        return value == null || value.booleanValue();
    }

    public void setHudVisible(String id, boolean visible) {
        if (id != null) {
            hud.put(id, Boolean.valueOf(visible));
        }
    }

    public HudPosition positionOf(String id) {
        HudPosition pos = positions.get(id);
        if (pos == null) {
            pos = new HudPosition(6, 6);
            positions.put(id, pos);
        }
        return pos;
    }

    public void resetPositions() {
        positions = defaultPositions();
    }

    /**
     * Clamp every value into a legal range and fill in defaults for
     * anything missing. Called after load and before save.
     */
    public void validate() {
        if (profile == null) {
            profile = "BALANCED";
        }
        java.util.List<String> legal = java.util.Arrays.asList("POTATO", "BALANCED", "SMOOTH");
        if (!legal.contains(profile.toUpperCase(java.util.Locale.ROOT))) {
            profile = "BALANCED";
        } else {
            profile = profile.toUpperCase(java.util.Locale.ROOT);
        }

        if (hud == null) {
            hud = defaultHud();
        } else {
            Map<String, Boolean> defaults = defaultHud();
            for (Map.Entry<String, Boolean> e : defaults.entrySet()) {
                if (!hud.containsKey(e.getKey())) {
                    hud.put(e.getKey(), e.getValue());
                }
            }
        }

        if (positions == null) {
            positions = defaultPositions();
        } else {
            Map<String, HudPosition> defaults = defaultPositions();
            for (Map.Entry<String, HudPosition> e : defaults.entrySet()) {
                if (!positions.containsKey(e.getKey()) || positions.get(e.getKey()) == null) {
                    positions.put(e.getKey(), e.getValue());
                }
            }
            for (Map.Entry<String, HudPosition> e : positions.entrySet()) {
                HudPosition p = e.getValue();
                if (p != null) {
                    p.x = clampInt(p.x, -4096, 8192);
                    p.y = clampInt(p.y, -4096, 8192);
                    p.scale = (p.scale <= 0.0D) ? 1.0D : clampDouble(p.scale, 0.6D, 2.0D);
                }
            }
        }

        hudScale = clampDouble(hudScale, 0.5D, 2.0D);
        backgroundOpacity = clampDouble(backgroundOpacity, 0.0D, 0.9D);

        unfocusedFps = clampInt(unfocusedFps, 1, 120);

        zoomLevel = clampDouble(zoomLevel, 1.0D, 12.0D);

        crosshairSize = clampDouble(crosshairSize, 0.25D, 4.0D);
        crosshairStyle = CrosshairStyle.fromString(crosshairStyle).name();

        int rgb = crosshairColor & 0x00FFFFFF;
        crosshairColor = 0xFF000000 | rgb;

        aura = Aura.fromString(aura).name();

        /* ---- V2 fields ---- */
        hudAccent = 0xFF000000 | (hudAccent & 0x00FFFFFF);

        zoomSensitivity = clampDouble(zoomSensitivity, 0.25D, 2.0D);
        int[] zoomKeys = {67, 86, 90, 88, 70}; // C, V, Z, X, F
        boolean zoomKeyOk = false;
        for (int key : zoomKeys) {
            if (key == zoomKey) {
                zoomKeyOk = true;
                break;
            }
        }
        if (!zoomKeyOk) {
            zoomKey = 67;
        }

        crosshairThickness = clampDouble(crosshairThickness, 0.5D, 3.0D);
        crosshairGap = clampDouble(crosshairGap, 0.25D, 3.0D);
        auraIntensity = clampDouble(auraIntensity, 0.5D, 2.5D);

        if (animationQuality == null) {
            animationQuality = "MEDIUM";
        } else {
            animationQuality = animationQuality.trim().toUpperCase(java.util.Locale.ROOT);
            if (!animationQuality.equals("LOW") && !animationQuality.equals("MEDIUM")
                    && !animationQuality.equals("HIGH")) {
                animationQuality = "MEDIUM";
            }
        }

        if (modules == null) {
            modules = new HashMap<String, Boolean>();
        }
    }

    public boolean isModuleEnabled(String id, boolean defaultValue) {
        Boolean value = modules.get(id);
        return value == null ? defaultValue : value.booleanValue();
    }

    public void setModuleEnabled(String id, boolean enabled) {
        if (id != null) {
            modules.put(id, Boolean.valueOf(enabled));
        }
    }

    /* ---------------------------------------------------------- */

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clampDouble(double v, double min, double max) {
        if (Double.isNaN(v)) {
            return (min + max) / 2.0D;
        }
        return Math.max(min, Math.min(max, v));
    }
}
