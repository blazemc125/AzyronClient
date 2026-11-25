package com.jiggneshgaming.azyron.performance;

import com.jiggneshgaming.azyron.config.AzyronConfig;
import com.jiggneshgaming.azyron.core.profile.PerformanceProfile;
import com.jiggneshgaming.azyron.util.TextHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.SimpleOption;
import com.jiggneshgaming.azyron.compat.ParticlesCompat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT — applies the selected performance profile to the game.
 *
 * <p>Direct, yarn-1.21.11-stable typed accessors are used throughout;
 * every write is individually guarded so an unexpected option failure can
 * never crash the game — it is logged and skipped.</p>
 */
public final class ProfileManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/profiles");

    private ProfileManager() {
    }

    public static PerformanceProfile.Id activeProfile() {
        return PerformanceProfile.Id.fromString(AzyronConfig.get().profile, PerformanceProfile.Id.BALANCED);
    }

    public static PerformanceProfile.Settings activeSettings() {
        return PerformanceProfile.forId(activeProfile());
    }

    /** Apply the currently configured profile. Safe to call at any time. */
    public static void applyActiveProfile() {
        apply(activeProfile());
    }

    /** Apply a profile by id and persist the choice. */
    public static void apply(PerformanceProfile.Id id) {
        PerformanceProfile.Settings s = PerformanceProfile.forId(id);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            LOGGER.warn("Options not available yet — profile {} will apply on the next world load.", id);
            return;
        }
        applySettings(s);
        AzyronConfig.get().profile = id.name();
        AzyronConfig.save();
        notifyPlayer(client, "AZYRON CLIENT: " + id.name() + " profile applied");
        LOGGER.info("Applied performance profile {}", id);
    }

    /** Apply settings without changing the persisted selection (used on world join). */
    public static void applySettings(PerformanceProfile.Settings s) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            LOGGER.warn("Options not available yet — profile will apply on the next tick.");
            return;
        }
        net.minecraft.client.option.GameOptions options = client.options;

        // --- direct, yarn-1.21.11 stable option writes (each guarded) ---
        setGraphicsMode(options, s.graphicsMode);
        setParticles(options, s.particles);
        setOption(options.getViewDistance(), s.viewDistance, "view distance");
        setOption(options.getSimulationDistance(), s.simulationDistance, "simulation distance");
        setOption(options.getMaxFps(), s.maxFps, "max framerate");
        setOption(options.getMipmapLevels(), s.mipmapLevels, "mipmap levels");
        setOption(options.getEntityDistanceScaling(), s.entityDistanceScaling, "entity distance scaling");
        setOption(options.getBiomeBlendRadius(), s.biomeBlendRadius, "biome blend radius");
        setOption(options.getEnableVsync(), s.vsync, "vsync");
        setOption(options.getAo(), s.smoothLighting, "smooth lighting");
        setCloudOption(options, s.cloudsEnabled);
    }

    /* ---------------------------------------------------------- */

    private static void setGraphicsMode(net.minecraft.client.option.GameOptions options, int mode) {
        try {
            GraphicsMode[] values = GraphicsMode.values();
            GraphicsMode target = values[Math.max(0, Math.min(values.length - 1, mode))];
            net.minecraft.client.option.SimpleOption<GraphicsMode> preset =
                    com.jiggneshgaming.azyron.compat.OptionsCompat.graphicsModeOption(options);
            if (preset == null) {
                // Option not constructed yet (early startup) — skip, never NPE.
                LOGGER.warn("The graphics preset option is not available yet — skipping graphics mode {}.", mode);
                return;
            }
            preset.setValue(target);
        } catch (Throwable t) {
            LOGGER.warn("Could not apply graphics mode {}: {}", mode, t.toString());
        }
    }

    private static void setParticles(net.minecraft.client.option.GameOptions options, int particles) {
        try {
            // ParticlesMode moved packages in 1.21.2 — resolved reflectively.
            // yarn order is All, Decreased, Minimal — matching PARTICLES_* constants.
            if (!ParticlesCompat.setLevel(options, particles)) {
                LOGGER.warn("Could not apply particles mode {}: option not resolvable on this version.", particles);
            }
        } catch (Throwable t) {
            LOGGER.warn("Could not apply particles mode {}: {}", particles, t.toString());
        }
    }

    private static <T> void setOption(SimpleOption<T> option, T value, String label) {
        if (option == null) {
            LOGGER.debug("Option {} is not available yet — skipped.", label);
            return;
        }
        try {
            option.setValue(value);
        } catch (Throwable t) {
            LOGGER.debug("Could not apply option {}: {}", label, t.toString());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setCloudOption(net.minecraft.client.option.GameOptions options, boolean enabled) {
        try {
            SimpleOption option = options.getCloudRenderMode();
            if (option == null) {
                return;
            }
            Object current = option.getValue();
            if (!(current instanceof Enum)) {
                return;
            }
            Object off = null;
            Object fast = null;
            for (Object constant : current.getClass().getEnumConstants()) {
                String name = ((Enum<?>) constant).name().toUpperCase(java.util.Locale.ROOT);
                if ("OFF".equals(name)) {
                    off = constant;
                } else if ("FAST".equals(name) || "FANCY".equals(name)) {
                    if (fast == null || "FAST".equals(name)) {
                        fast = constant; // prefer FAST when both are available
                    }
                }
            }
            Object target = enabled ? fast : off;
            if (target != null) {
                option.setValue(target);
            }
        } catch (Throwable t) {
            LOGGER.debug("Cloud option unavailable: {}", t.toString());
        }
    }

    private static void notifyPlayer(MinecraftClient client, String message) {
        try {
            if (client.player != null) {
                client.player.sendMessage(TextHelper.literal(message), true);
            }
        } catch (Throwable ignored) {
        }
    }
}
