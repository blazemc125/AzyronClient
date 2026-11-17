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
 * <p>Minecraft 1.21.9 introduced {@code KeyBinding.Category}; the category is
 * created through the public {@code KeyBinding.Category.create(Identifier)}
 * factory and the bindings are constructed directly against the stable
 * yarn-mapped API. A failure is logged loudly and disables only that
 * binding — the game never crashes over a keybind.</p>
 */
public final class KeyBinds {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/keys");

    private KeyBinds() {
    }

    public static final String CATEGORY = "key.categories.azyronclient";

    private static final Identifier CATEGORY_ID = Identifier.of("azyronclient", "main");

    /** AZYRON keybinding category, shared by every AZYRON binding. */
    public static final KeyBinding.Category AZYRON_CATEGORY = createCategory();

    public static KeyBinding MENU;
    public static KeyBinding ZOOM;
    public static KeyBinding SPRINT_TOGGLE;
    public static KeyBinding HUD_EDITOR;

    private static KeyBinding.Category createCategory() {
        try {
            return KeyBinding.Category.create(CATEGORY_ID);
        } catch (Throwable t) {
            // A broken category must not take the bindings (or the game) down.
            LOGGER.error("Could not create the AZYRON keybinding category — falling back to the Misc category.", t);
            return KeyBinding.Category.MISC;
        }
    }

    public static void register() {
        MENU = register("key.azyronclient.menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
        ZOOM = register("key.azyronclient.zoom", GLFW.GLFW_KEY_C);
        SPRINT_TOGGLE = register("key.azyronclient.sprint_toggle", GLFW.GLFW_KEY_R);
        HUD_EDITOR = register("key.azyronclient.hud_editor", GLFW.GLFW_KEY_H);
    }

    private static KeyBinding register(String translationKey, int keyCode) {
        try {
            KeyBinding binding = new KeyBinding(translationKey, InputUtil.Type.KEYSYM, keyCode, AZYRON_CATEGORY);
            return KeyBindingHelper.registerKeyBinding(binding);
        } catch (Throwable t) {
            // Feature degrades to disabled instead of crashing the game.
            LOGGER.error("Could not register keybinding {} — feature disabled until restart.", translationKey, t);
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
