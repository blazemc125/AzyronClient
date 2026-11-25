package com.jiggneshgaming.azyron.keybinds;

import com.jiggneshgaming.azyron.compat.KeybindCompat;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT V2 — keybinding registration.
 *
 * <p>The registration mechanics differ between Minecraft releases
 * (1.21.9+ replaced the String-category constructor with
 * {@code KeyBinding.Category}), so the actual construction lives in the
 * per-version compat module ({@code compat/KeybindCompat}). Failures are
 * logged loudly and disable only that binding — never the game.</p>
 */
public final class KeyBinds {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/keys");

    /** Translation key for the AZYRON keybinding category (pre-1.21.9 screens). */
    public static final String CATEGORY = "key.categories.azyronclient";

    private KeyBinds() {
    }

    public static KeyBinding MENU;
    public static KeyBinding ZOOM;
    public static KeyBinding SPRINT_TOGGLE;
    public static KeyBinding HUD_EDITOR;

    public static void register() {
        MENU = KeybindCompat.register("key.azyronclient.menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
        ZOOM = KeybindCompat.register("key.azyronclient.zoom", GLFW.GLFW_KEY_C);
        SPRINT_TOGGLE = KeybindCompat.register("key.azyronclient.sprint_toggle", GLFW.GLFW_KEY_R);
        HUD_EDITOR = KeybindCompat.register("key.azyronclient.hud_editor", GLFW.GLFW_KEY_H);
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
