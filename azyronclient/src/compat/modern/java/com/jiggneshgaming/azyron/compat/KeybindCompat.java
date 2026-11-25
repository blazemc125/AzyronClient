package com.jiggneshgaming.azyron.compat;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT V2 — keybinding registration for Minecraft 1.21.9+.
 *
 * <p>In 1.21.9 the {@code String}-category KeyBinding constructor was removed
 * in favour of {@link KeyBinding.Category}. A broken category must never
 * take the bindings down — we fall back to the MISC category, then to a
 * disabled binding (logged loudly), never to a crash.</p>
 */
public final class KeybindCompat {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/keys");

    private static final Identifier CATEGORY_ID = Identifier.of("azyronclient", "main");

    private KeybindCompat() {
    }

    public static KeyBinding register(String translationKey, int keyCode) {
        KeyBinding.Category category = createCategory();
        try {
            KeyBinding binding = new KeyBinding(translationKey,
                    net.minecraft.client.util.InputUtil.Type.KEYSYM, keyCode, category);
            return KeyBindingHelper.registerKeyBinding(binding);
        } catch (Throwable t) {
            LOGGER.error("Could not register keybinding {} — feature disabled until restart.",
                    translationKey, t);
            return null;
        }
    }

    private static KeyBinding.Category createCategory() {
        try {
            return KeyBinding.Category.create(CATEGORY_ID);
        } catch (Throwable t) {
            LOGGER.error("Could not create the AZYRON keybinding category — falling back to the MISC category.", t);
            return KeyBinding.Category.MISC;
        }
    }
}
