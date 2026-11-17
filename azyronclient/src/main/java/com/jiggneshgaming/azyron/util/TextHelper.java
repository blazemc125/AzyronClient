package com.jiggneshgaming.azyron.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * AZYRON CLIENT — text literal helpers.
 */
public final class TextHelper {

    private TextHelper() {
    }

    public static MutableText literal(String value) {
        return Text.literal(value == null ? "" : value);
    }

    public static Text translatable(String key, Object... args) {
        return Text.translatable(key, args);
    }
}
