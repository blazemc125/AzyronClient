package com.jiggneshgaming.azyron.modules;

/**
 * AZYRON CLIENT — a toggleable client module shown in the Mods Menu.
 */
public final class AzyronModule {

    public enum Category {
        ALL("All"),
        PERFORMANCE("Performance"),
        HUD("HUD"),
        GAMEPLAY("Gameplay"),
        APPEARANCE("Appearance");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    /** Module ids used as keys in the config's module map. */
    public static final String ID_TOGGLE_SPRINT = "toggle_sprint";
    public static final String ID_DYNAMIC_FPS = "dynamic_fps";
    public static final String ID_ZOOM = "zoom";
    public static final String ID_SMOOTH_ZOOM = "smooth_zoom";
    public static final String ID_CUSTOM_CROSSHAIR = "custom_crosshair";
    public static final String ID_COSMETIC_AURA = "cosmetic_aura";

    public final String id;
    public final String name;
    public final String description;
    public final Category category;
    /** True for modules that also show a gear button opening the settings screen. */
    public final boolean hasSettings;
    private final java.util.function.Supplier<Boolean> defaultSupplier;

    public AzyronModule(String id, String name, String description, Category category,
                        boolean hasSettings, java.util.function.Supplier<Boolean> defaultSupplier) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.hasSettings = hasSettings;
        this.defaultSupplier = defaultSupplier;
    }

    public boolean defaultEnabled() {
        return defaultSupplier != null && Boolean.TRUE.equals(defaultSupplier.get());
    }
}
