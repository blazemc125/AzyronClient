package com.jiggneshgaming.azyron.performance;

import com.jiggneshgaming.azyron.config.AzyronConfig;
import com.jiggneshgaming.azyron.core.profile.PerformanceProfile;
import com.jiggneshgaming.azyron.util.ReflectionUtil;
import com.jiggneshgaming.azyron.util.TextHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT — applies the selected performance profile to the game.
 *
 * <p>Direct, stable getters are used where yarn names are known-stable;
 * everything optional goes through reflection so a renamed getter can never
 * crash the game — it just gets skipped with a log line.</p>
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
        net.minecraft.client.option.GameOptions options = client.options;

        // --- direct, stable option writes (each guarded) ---
        setGraphicsMode(options, s.graphicsMode);
        setParticles(options, s.particles);
        setIntOption(options, "getViewDistance", s.viewDistance, "view distance");
        setIntOption(options, "getSimulationDistance", s.simulationDistance, "simulation distance");
        setIntOption(options, "getMaxFps", s.maxFps, "max framerate");
        setIntOption(options, "getMipmapLevels", s.mipmapLevels, "mipmap levels");
        setDoubleOption(options, "getEntityDistanceScaling", s.entityDistanceScaling, "entity distance scaling");

        // --- best-effort extras via reflection (never fatal) ---
        setBooleanishOption(options, "getVsync", s.vsync, "vsync");
        setIntOption(options, "getBiomeBlendRadius", s.biomeBlendRadius, "biome blend radius");
        setBooleanishOption(options, "getAo", s.smoothLighting, "smooth lighting");
        setCloudOption(options, s.cloudsEnabled);
    }

    /* ---------------------------------------------------------- */

    private static void setGraphicsMode(net.minecraft.client.option.GameOptions options, int mode) {
        try {
            GraphicsMode[] values = GraphicsMode.values();
            GraphicsMode target = values[Math.max(0, Math.min(values.length - 1, mode))];
            options.getPreset().setValue(target);
        } catch (Throwable t) {
            LOGGER.warn("Could not apply graphics mode {}: {}", mode, t.toString());
        }
    }

    private static void setParticles(net.minecraft.client.option.GameOptions options, int particles) {
        try {
            ParticlesMode[] values = ParticlesMode.values();
            // yarn order is All, Decreased, Minimal — matching PARTICLES_* constants.
            ParticlesMode target = values[Math.max(0, Math.min(values.length - 1, particles))];
            options.getParticles().setValue(target);
        } catch (Throwable t) {
            LOGGER.warn("Could not apply particles mode {}: {}", particles, t.toString());
        }
    }

    private static void setIntOption(net.minecraft.client.option.GameOptions options, String getter,
                                     int value, String label) {
        try {
            Object option = ReflectionUtil.invokeNoArg(options, getter);
            if (option == null) {
                return;
            }
            java.lang.reflect.Method setValue = option.getClass().getMethod("setValue", Object.class);
            setValue.invoke(option, Integer.valueOf(value));
        } catch (Throwable t) {
            LOGGER.debug("Option {} unavailable: {}", label, t.toString());
        }
    }

    private static void setDoubleOption(net.minecraft.client.option.GameOptions options, String getter,
                                        double value, String label) {
        try {
            Object option = ReflectionUtil.invokeNoArg(options, getter);
            if (option == null) {
                return;
            }
            java.lang.reflect.Method setValue = option.getClass().getMethod("setValue", Object.class);
            setValue.invoke(option, Double.valueOf(value));
        } catch (Throwable t) {
            LOGGER.debug("Option {} unavailable: {}", label, t.toString());
        }
    }

    private static void setBooleanishOption(net.minecraft.client.option.GameOptions options, String getter,
                                            boolean value, String label) {
        try {
            Object option = ReflectionUtil.invokeNoArg(options, getter);
            if (option == null) {
                return;
            }
            // SimpleOption<Boolean>
            try {
                java.lang.reflect.Method setValue = option.getClass().getMethod("setValue", Object.class);
                setValue.invoke(option, Boolean.valueOf(value));
                return;
            } catch (NoSuchMethodException direct) {
                // older/other shapes — try primitive boolean overload
                java.lang.reflect.Method setValue = option.getClass().getMethod("setValue", boolean.class);
                setValue.invoke(option, Boolean.valueOf(value));
            }
        } catch (Throwable t) {
            LOGGER.debug("Option {} unavailable: {}", label, t.toString());
        }
    }

    private static void setCloudOption(net.minecraft.client.option.GameOptions options, boolean enabled) {
        try {
            Object option = ReflectionUtil.invokeNoArg(options, "getCloudRenderMode", "getClouds");
            if (option == null) {
                return;
            }
            Object current = option.getClass().getMethod("getValue").invoke(option);
            if (current == null) {
                return;
            }
            Object value = null;
            for (Object constant : current.getClass().getEnumConstants()) {
                String name = ((Enum<?>) constant).name().toUpperCase(java.util.Locale.ROOT);
                if (enabled ? !"OFF".equals(name) : "OFF".equals(name)) {
                    if (enabled && ("FAST".equals(name) || "FANCY".equals(name))) {
                        value = constant;
                        if ("FAST".equals(name)) {
                            break;
                        }
                    } else if (!enabled && "OFF".equals(name)) {
                        value = constant;
                        break;
                    }
                }
            }
            if (value != null) {
                java.lang.reflect.Method setValue = option.getClass().getMethod("setValue", Object.class);
                setValue.invoke(option, value);
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
