package com.jiggneshgaming.azyron.keybinds;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT — key binding registration.
 *
 * <p>Minecraft 1.21.9 introduced {@code KeyBinding.Category}; older versions
 * used a plain string category. Registration is attempted with the modern
 * API first and falls back to the legacy signature, so this file survives
 * both worlds without a compile-time version check.</p>
 */
public final class KeyBinds {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/keys");

    private KeyBinds() {
    }

    public static final String CATEGORY = "key.categories.azyronclient";

    public static KeyBinding MENU;
    public static KeyBinding ZOOM;
    public static KeyBinding SPRINT_TOGGLE;
    public static KeyBinding HUD_EDITOR;

    public static void register() {
        MENU = register("key.azyronclient.menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
        ZOOM = register("key.azyronclient.zoom", GLFW.GLFW_KEY_C);
        SPRINT_TOGGLE = register("key.azyronclient.sprint_toggle", GLFW.GLFW_KEY_R);
        HUD_EDITOR = register("key.azyronclient.hud_editor", GLFW.GLFW_KEY_H);
    }

    private static KeyBinding register(String translationKey, int keyCode) {
        KeyBinding binding = createModern(translationKey, keyCode);
        if (binding == null) {
            binding = createLegacy(translationKey, keyCode);
        }
        if (binding == null) {
            // No compatible constructor on this Minecraft version — disable the
            // feature instead of crashing. All poll helpers are null-safe.
            LOGGER.error("Could not create keybinding {} — feature disabled until restart.", translationKey);
            return null;
        }
        return KeyBindingHelper.registerKeyBinding(binding);
    }

    private static KeyBinding createModern(String translationKey, int keyCode) {
        try {
            // KeyBinding.Category.create(Identifier) — added in 1.21.9.
            Class<?> categoryClass = Class.forName("net.minecraft.client.option.KeyBinding$Category");
            Object category = categoryClass
                    .getMethod("create", Identifier.class)
                    .invoke(null, Identifier.of("azyronclient", "main"));
            java.lang.reflect.Constructor<KeyBinding> ctor = KeyBinding.class
                    .getDeclaredConstructor(String.class, InputUtil.Type.class, int.class, categoryClass);
            ctor.setAccessible(true);
            return ctor.newInstance(translationKey, InputUtil.Type.KEYSYM, keyCode, category);
        } catch (Throwable t) {
            LOGGER.debug("Modern KeyBinding.Category API unavailable, using legacy signature.");
            return null;
        }
    }

    private static KeyBinding createLegacy(String translationKey, int keyCode) {
        try {
            // Resolved reflectively — the string-category constructor was
            // removed in newer Minecraft versions.
            java.lang.reflect.Constructor<KeyBinding> ctor = KeyBinding.class
                    .getDeclaredConstructor(String.class, InputUtil.Type.class, int.class, String.class);
            ctor.setAccessible(true);
            return ctor.newInstance(translationKey, InputUtil.Type.KEYSYM, keyCode, CATEGORY);
        } catch (Throwable t) {
            LOGGER.debug("Legacy KeyBinding(String, Type, int, String) constructor unavailable: {}", t.toString());
            return null;
        }
    }

    /** Poll helpers used from the tick handler. */
    public static boolean menuWasPressed() {
        return MENU != null && MENU.wasPressed();
    }

    public static boolean zoomIsPressed() {
        return ZOOM != null && ZOOM.isPressed();
    }

    public static boolean sprintToggleWasPressed() {
        return SPRINT_TOGGLE != null && SPRINT_TOGGLE.wasPressed();
    }

    public static boolean hudEditorWasPressed() {
        return HUD_EDITOR != null && HUD_EDITOR.wasPressed();
    }
}
