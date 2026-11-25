package com.jiggneshgaming.azyron.compat;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT V2 — keybinding registration for Minecraft 1.21 through
 * 1.21.8. The KeyBinding constructor takes a raw {@code String} category;
 * the matching translation key ({@code key.categories.azyronclient}) is
 * registered in {@code en_us.json}.
 */
public final class KeybindCompat {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/keys");

    /** Translation key for the AZYRON keybinding category. */
    public static final String CATEGORY = "key.categories.azyronclient";

    private KeybindCompat() {
    }

    public static KeyBinding register(String translationKey, int keyCode) {
        try {
            KeyBinding binding = new KeyBinding(translationKey,
                    net.minecraft.client.util.InputUtil.Type.KEYSYM, keyCode, CATEGORY);
            return KeyBindingHelper.registerKeyBinding(binding);
        } catch (Throwable t) {
            LOGGER.error("Could not register keybinding {} — feature disabled until restart.",
                    translationKey, t);
            return null;
        }
    }
}
