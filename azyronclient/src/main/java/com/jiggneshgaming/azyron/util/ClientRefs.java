package com.jiggneshgaming.azyron.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT — typed accessors for Minecraft internals whose yarn
 * names drift between versions.
 *
 * <p>Direct typed access is used wherever the field/method visibility and
 * name are stable (currentScreen, window scaled size). TextRenderer and
 * Session access stay reflection-based because those members are
 * package-private (textRenderer) / private (session) in 1.21.11; the helper
 * degrades gracefully instead of crashing the HUD.</p>
 */
public final class ClientRefs {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/refs");
    private static boolean warnedTextRenderer;

    private ClientRefs() {
    }

    /** TextRenderer regardless of yarn naming (getTextRenderer() vs textRenderer field). */
    public static TextRenderer textRenderer(MinecraftClient client) {
        if (client == null) {
            return null;
        }
        Object viaMethod = ReflectionUtil.invokeNoArg(client, "getTextRenderer");
        if (viaMethod instanceof TextRenderer) {
            return (TextRenderer) viaMethod;
        }
        Object viaField = ReflectionUtil.getField(client, "textRenderer");
        if (viaField instanceof TextRenderer) {
            return (TextRenderer) viaField;
        }
        if (!warnedTextRenderer) {
            warnedTextRenderer = true;
            LOGGER.warn("Could not resolve the TextRenderer — text-based HUD elements will be skipped.");
        }
        return null;
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
