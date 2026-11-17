package com.jiggneshgaming.azyron.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * AZYRON CLIENT — single point of contact for text drawing.
 *
 * <p>All text rendering goes through this class so that if a Minecraft text
 * drawing signature changes, exactly one file needs to be touched.</p>
 */
public final class Texts {

    private Texts() {
    }

    public static void draw(DrawContext ctx, TextRenderer textRenderer, String text,
                            int x, int y, int color, boolean shadow) {
        ctx.drawText(textRenderer, Text.literal(text == null ? "" : text), x, y, color, shadow);
    }

    public static void draw(DrawContext ctx, TextRenderer textRenderer, Text text,
                            int x, int y, int color, boolean shadow) {
        ctx.drawText(textRenderer, text, x, y, color, shadow);
    }

    public static void drawCentered(DrawContext ctx, TextRenderer textRenderer, String text,
                                    int centerX, int y, int color, boolean shadow) {
        String value = text == null ? "" : text;
        draw(ctx, textRenderer, value, centerX - width(textRenderer, value) / 2, y, color, shadow);
    }

    public static int width(TextRenderer textRenderer, String text) {
        return textRenderer.getWidth(text == null ? "" : text);
    }
}
