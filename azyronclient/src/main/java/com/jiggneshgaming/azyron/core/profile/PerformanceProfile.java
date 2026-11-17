package com.jiggneshgaming.azyron.core.profile;

/**
 * AZYRON CLIENT — Performance profiles.
 *
 * <p>Pure, dependency-free definition of the three performance profiles.
 * The concrete Minecraft options are applied by
 * {@code com.jiggneshgaming.azyron.performance.ProfileManager} on the Fabric side;
 * this class intentionally contains zero Minecraft references so it can be
 * unit-tested standalone.</p>
 */
public final class PerformanceProfile {

    private PerformanceProfile() {
    }

    /** Identifier used in config files and the CLI. */
    public enum Id {
        POTATO,
        BALANCED,
        SMOOTH;

        public Id next() {
            Id[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static Id fromString(String raw, Id fallback) {
            if (raw == null) {
                return fallback;
            }
            try {
                return Id.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return fallback;
            }
        }
    }

    /** Graphics mode ids, matching Minecraft's Fast / Fancy / Fabulous order. */
    public static final int GRAPHICS_FAST = 0;
    public static final int GRAPHICS_FANCY = 1;
    public static final int GRAPHICS_FABULOUS = 2;

    /** Particles mode ids: All / Decreased / Minimal. */
    public static final int PARTICLES_ALL = 0;
    public static final int PARTICLES_DECREASED = 1;
    public static final int PARTICLES_MINIMAL = 2;

    /** Immutable bundle of option values for one profile. */
    public static final class Settings {
        public final int graphicsMode;
        public final int particles;
        public final int viewDistance;
        public final int simulationDistance;
        public final int maxFps;
        public final double entityDistanceScaling;
        public final int mipmapLevels;
        public final int biomeBlendRadius;
        public final boolean smoothLighting;
        public final boolean cloudsEnabled;
        public final boolean vsync;
        public final String blurb;

        public Settings(int graphicsMode, int particles, int viewDistance, int simulationDistance,
                       int maxFps, double entityDistanceScaling, int mipmapLevels, int biomeBlendRadius,
                       boolean smoothLighting, boolean cloudsEnabled, boolean vsync, String blurb) {
            this.graphicsMode = graphicsMode;
            this.particles = particles;
            this.viewDistance = viewDistance;
            this.simulationDistance = simulationDistance;
            this.maxFps = maxFps;
            this.entityDistanceScaling = entityDistanceScaling;
            this.mipmapLevels = mipmapLevels;
            this.biomeBlendRadius = biomeBlendRadius;
            this.smoothLighting = smoothLighting;
            this.cloudsEnabled = cloudsEnabled;
            this.vsync = vsync;
            this.blurb = blurb;
        }
    }

    /** Potato Mode — maximum FPS optimization; minimum particles and effects; lowest graphics. */
    public static final Settings POTATO = new Settings(
            GRAPHICS_FAST,
            PARTICLES_MINIMAL,
            /* viewDistance          */ 6,
            /* simulationDistance    */ 4,
            /* maxFps                */ 260,
            /* entityDistanceScaling */ 0.5D,
            /* mipmapLevels          */ 0,
            /* biomeBlendRadius      */ 0,
            /* smoothLighting        */ false,
            /* cloudsEnabled         */ false,
            /* vsync                 */ false,
            "Maximum FPS optimization — minimum particles and visual effects, low graphics.");

    /** Balanced Mode — a middle ground between performance and visuals. */
    public static final Settings BALANCED = new Settings(
            GRAPHICS_FANCY,
            PARTICLES_DECREASED,
            12,
            8,
            /* maxFps                */ 144,
            1.0D,
            /* mipmapLevels          */ 4,
            /* biomeBlendRadius      */ 2,
            /* smoothLighting        */ true,
            /* cloudsEnabled         */ true,
            /* vsync                 */ false,
            "Balanced performance and visuals — the recommended everyday profile.");

    /** Smooth FPS Mode — prioritize smooth, stable frame pacing. */
    public static final Settings SMOOTH = new Settings(
            GRAPHICS_FANCY,
            PARTICLES_DECREASED,
            12,
            6,
            /* maxFps                */ 120,
            /* entityDistanceScaling */ 1.25D,
            /* mipmapLevels          */ 2,
            /* biomeBlendRadius      */ 1,
            /* smoothLighting        */ true,
            /* cloudsEnabled         */ true,
            /* vsync                 */ true,
            "Prioritizes smooth, stable frame pacing over raw frame rate.");

    public static Settings forId(Id id) {
        switch (id == null ? Id.BALANCED : id) {
            case POTATO:
                return POTATO;
            case SMOOTH:
                return SMOOTH;
            case BALANCED:
            default:
                return BALANCED;
        }
    }
}
