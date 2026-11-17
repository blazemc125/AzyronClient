package com.jiggneshgaming.azyron.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT — typed accessors for Minecraft internals.
 *
 * <p>Every accessor is a direct, compile-time call. Private members are made
 * visible to the mod by the {@code azyronclient.accesswidener} (remapped by
 * Loom for production) — literal-name reflection is never used, because at
 * runtime the field names are intermediary and reflection silently returns
 * nothing (the root cause of the "TextRenderer cannot be resolved" failure).</p>
 */
public final class ClientRefs {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/refs");

    private ClientRefs() {
    }

    /** The client's text renderer — always available after client startup. */
    public static TextRenderer textRenderer(MinecraftClient client) {
        try {
            return client != null ? client.textRenderer : null;
        } catch (Throwable t) {
            LOGGER.warn("TextRenderer unavailable this session: {}", t.toString());
            return null;
        }
    }

    /** Current screen or null (public field on MinecraftClient). */
    public static Screen currentScreen(MinecraftClient client) {
        return client != null ? client.currentScreen : null;
    }

    /** Scaled window width in pixels (HUD coordinate space). */
    public static int scaledWidth(MinecraftClient client) {
        try {
            return client.getWindow().getScaledWidth();
        } catch (Throwable t) {
            LOGGER.debug("scaledWidth lookup failed, defaulting to 1280: {}", t.toString());
            return 1280;
        }
    }

    /** Scaled window height in pixels (HUD coordinate space). */
    public static int scaledHeight(MinecraftClient client) {
        try {
            return client.getWindow().getScaledHeight();
        } catch (Throwable t) {
            LOGGER.debug("scaledHeight lookup failed, defaulting to 720: {}", t.toString());
            return 720;
        }
    }
}
