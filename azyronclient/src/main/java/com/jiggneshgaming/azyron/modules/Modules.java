package com.jiggneshgaming.azyron.modules;

import com.jiggneshgaming.azyron.config.AzyronConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AZYRON CLIENT — the module registry backing the in-game Mods Menu.
 *
 * <p>HUD modules mirror the config's hud map; feature modules mirror
 * dedicated config flags. Everything is persisted through
 * {@link AzyronConfig}, so toggles take effect live.</p>
 */
public final class Modules {

    private Modules() {
    }

    private static final List<AzyronModule> MODULES = buildModules();

    private static List<AzyronModule> buildModules() {
        List<AzyronModule> list = new ArrayList<AzyronModule>();

        /* ------------------------------------------------ performance -- */
        list.add(new AzyronModule(AzyronModule.ID_DYNAMIC_FPS, "Dynamic FPS",
                "Drops the FPS cap while the game window is unfocused to save power.",
                AzyronModule.Category.PERFORMANCE, true, defaultSupplier(true)));
        list.add(new AzyronModule("sodium_recommended", "Sodium (modpack)",
                "Install Sodium from the AZYRON modpack for 2-3x FPS on desktop.",
                AzyronModule.Category.PERFORMANCE, false, defaultSupplier(true)));
        list.add(new AzyronModule("lithium_recommended", "Lithium (modpack)",
                "Game-logic optimizations bundled in the AZYRON modpack.",
                AzyronModule.Category.PERFORMANCE, false, defaultSupplier(true)));
        list.add(new AzyronModule("ferritecore_recommended", "FerriteCore (modpack)",
                "Memory optimizations bundled in the AZYRON modpack.",
                AzyronModule.Category.PERFORMANCE, false, defaultSupplier(true)));

        /* ------------------------------------------------------- HUD -- */
        list.add(new AzyronModule("hud_fps", "FPS Counter",
                "Live FPS with traffic-light coloring.",
                AzyronModule.Category.HUD, true, hudDefault("fps")));
        list.add(new AzyronModule("hud_cps", "CPS Counter",
                "Left/right clicks per second, rolling 1s window.",
                AzyronModule.Category.HUD, true, hudDefault("cps")));
        list.add(new AzyronModule("hud_ping", "Ping Display",
                "Your latency to the current server in milliseconds.",
                AzyronModule.Category.HUD, true, hudDefault("ping")));
        list.add(new AzyronModule("hud_coordinates", "Coordinates HUD",
                "Your block coordinates, updated live.",
                AzyronModule.Category.HUD, true, hudDefault("coordinates")));
        list.add(new AzyronModule("hud_keystrokes", "Keystrokes HUD",
                "WASD, space and mouse buttons with press feedback.",
                AzyronModule.Category.HUD, true, hudDefault("keystrokes")));
        list.add(new AzyronModule("hud_armor", "Armor HUD",
                "Armor pieces with durability percentage.",
                AzyronModule.Category.HUD, true, hudDefault("armor")));
        list.add(new AzyronModule("hud_potions", "Potion Effects HUD",
                "Active status effects with level and remaining time.",
                AzyronModule.Category.HUD, true, hudDefault("potions")));
        list.add(new AzyronModule("hud_sprint_indicator", "Sprint Indicator",
                "Shows whether sprint is toggled on.",
                AzyronModule.Category.HUD, false, hudDefault("sprint_indicator")));
        list.add(new AzyronModule("hud_zoom_indicator", "Zoom Indicator",
                "Shows the active zoom multiplier while zooming.",
                AzyronModule.Category.HUD, false, hudDefault("zoom_indicator")));
        list.add(new AzyronModule("hud_direction", "Direction Indicator",
                "Cardinal direction you are facing (N/NE/E/SE/S/SW/W/NW).",
                AzyronModule.Category.HUD, false, hudDefault("direction")));
        list.add(new AzyronModule("hud_clock", "Clock",
                "In-game day and 24-hour time, updated live.",
                AzyronModule.Category.HUD, false, hudDefault("clock")));
        list.add(new AzyronModule("hud_biome", "Biome Display",
                "The biome you are standing in, refreshed live.",
                AzyronModule.Category.HUD, false, hudDefault("biome")));
        list.add(new AzyronModule("hud_player_status", "Player Status",
                "Health and hunger at a glance.",
                AzyronModule.Category.HUD, false, hudDefault("player_status")));

        /* --------------------------------------------------- gameplay -- */
        list.add(new AzyronModule(AzyronModule.ID_TOGGLE_SPRINT, "Toggle Sprint",
                "Always sprint without holding Ctrl. Persists and works in-world.",
                AzyronModule.Category.GAMEPLAY, true, defaultSupplier(true)));
        list.add(new AzyronModule(AzyronModule.ID_ZOOM, "Zoom",
                "Hold C (default) to zoom. Strength adjustable in settings.",
                AzyronModule.Category.GAMEPLAY, true, defaultSupplier(true)));
        list.add(new AzyronModule(AzyronModule.ID_SMOOTH_ZOOM, "Smooth Zoom",
                "Ease the zoom transition instead of snapping.",
                AzyronModule.Category.GAMEPLAY, true, defaultSupplier(true)));

        /* ------------------------------------------------- appearance -- */
        list.add(new AzyronModule(AzyronModule.ID_CUSTOM_CROSSHAIR, "Custom Crosshair",
                "Clean custom crosshair with color and size options; hides the vanilla one.",
                AzyronModule.Category.APPEARANCE, true, defaultSupplier(true)));
        list.add(new AzyronModule(AzyronModule.ID_COSMETIC_AURA, "Cosmetic Aura",
                "Client-side particle cosmetics around your player (cosmetics menu).",
                AzyronModule.Category.APPEARANCE, true, defaultSupplier(true)));

        return Collections.unmodifiableList(list);
    }

    private static java.util.function.Supplier<Boolean> defaultSupplier(final boolean value) {
        return new java.util.function.Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return Boolean.valueOf(value);
            }
        };
    }

    private static java.util.function.Supplier<Boolean> hudDefault(final String hudId) {
        return new java.util.function.Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return Boolean.valueOf(AzyronConfig.get().isHudVisible(hudId));
            }
        };
    }

    public static List<AzyronModule> all() {
        return MODULES;
    }

    public static List<AzyronModule> forCategory(AzyronModule.Category category) {
        if (category == AzyronModule.Category.ALL) {
            return MODULES;
        }
        List<AzyronModule> filtered = new ArrayList<AzyronModule>();
        for (AzyronModule module : MODULES) {
            if (module.category == category) {
                filtered.add(module);
            }
        }
        return filtered;
    }

    /** Whether a module is currently enabled (HUD modules read the hud map). */
    public static boolean isEnabled(AzyronModule module) {
        if (module == null) {
            return false;
        }
        com.jiggneshgaming.azyron.core.config.AzyronConfigModel config = AzyronConfig.get();
        if (module.id.startsWith("hud_")) {
            return config.isHudVisible(module.id.substring("hud_".length()));
        }
        switch (module.id) {
            case AzyronModule.ID_TOGGLE_SPRINT:
                return config.toggleSprint;
            case AzyronModule.ID_DYNAMIC_FPS:
                return config.dynamicFps;
            case AzyronModule.ID_ZOOM:
                return config.zoomEnabled;
            case AzyronModule.ID_SMOOTH_ZOOM:
                return config.smoothZoom;
            case AzyronModule.ID_CUSTOM_CROSSHAIR:
                return !"VANILLA".equals(config.crosshairStyle);
            case AzyronModule.ID_COSMETIC_AURA:
                return !"NONE".equals(config.aura);
            default:
                return config.isModuleEnabled(module.id, module.defaultEnabled());
        }
    }

    /** Enable/disable a module live. */
    public static void setEnabled(AzyronModule module, boolean enabled) {
        if (module == null) {
            return;
        }
        com.jiggneshgaming.azyron.core.config.AzyronConfigModel config = AzyronConfig.get();
        if (module.id.startsWith("hud_")) {
            config.setHudVisible(module.id.substring("hud_".length()), enabled);
        } else {
            switch (module.id) {
                case AzyronModule.ID_TOGGLE_SPRINT:
                    config.toggleSprint = enabled;
                    break;
                case AzyronModule.ID_DYNAMIC_FPS:
                    config.dynamicFps = enabled;
                    break;
                case AzyronModule.ID_ZOOM:
                    config.zoomEnabled = enabled;
                    break;
                case AzyronModule.ID_SMOOTH_ZOOM:
                    config.smoothZoom = enabled;
                    break;
                case AzyronModule.ID_CUSTOM_CROSSHAIR:
                    config.crosshairStyle = enabled
                            ? ("VANILLA".equals(config.crosshairStyle) ? "CLASSIC" : config.crosshairStyle)
                            : "VANILLA";
                    break;
                case AzyronModule.ID_COSMETIC_AURA:
                    if (enabled && "NONE".equals(config.aura)) {
                        config.aura = "END_SPARKLE";
                    } else if (!enabled) {
                        config.aura = "NONE";
                    }
                    break;
                default:
                    config.setModuleEnabled(module.id, enabled);
                    break;
            }
        }
        AzyronConfig.save();
    }
}
