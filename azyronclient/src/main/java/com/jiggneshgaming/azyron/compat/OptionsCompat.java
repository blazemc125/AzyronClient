package com.jiggneshgaming.azyron.compat;

import java.lang.reflect.Method;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.SimpleOption;

/**
 * Cross-version options shim (shared by every profile).
 *
 * <p>The graphics-quality option was renamed between releases:
 * <ul>
 *     <li>&le;1.21.10 — {@code GameOptions.getGraphicsMode()}</li>
 *     <li>&ge;1.21.11 — {@code GameOptions.getPreset()}</li>
 * </ul>
 * Both methods return the same {@code SimpleOption<GraphicsMode>} in every
 * affected release, so a single reflective lookup keeps the shared code
 * compilable and functional on both sides of the rename. Returns
 * {@code null} when neither method exists (never expected, but safe).</p>
 */
public final class OptionsCompat {

    private OptionsCompat() {
    }

    @SuppressWarnings("unchecked")
    public static SimpleOption<GraphicsMode> graphicsModeOption(GameOptions options) {
        if (options == null) {
            return null;
        }
        // Newest first: prefer getPreset() (1.21.11+), fall back to getGraphicsMode().
        for (String methodName : new String[]{"getPreset", "getGraphicsMode"}) {
            try {
                Method m = GameOptions.class.getMethod(methodName);
                Object value = m.invoke(options);
                if (value instanceof SimpleOption<?>) {
                    return (SimpleOption<GraphicsMode>) value;
                }
            } catch (Throwable ignored) {
                // Method missing or invocation failed — try the next name.
            }
        }
        return null;
    }
}
