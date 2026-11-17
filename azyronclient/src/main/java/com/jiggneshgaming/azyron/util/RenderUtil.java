package com.jiggneshgaming.azyron.util;

import com.jiggneshgaming.azyron.AzyronConstants;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * AZYRON CLIENT — premium procedural rendering helpers.
 *
 * <p>Every visual is drawn with plain rectangle fills and the vanilla text
 * font, so the UI is resolution-independent, needs no shader pipeline and
 * cannot fail on PojavLauncher's GL implementations.</p>
 */
public final class RenderUtil {

    private RenderUtil() {
    }

    /* ------------------------------------------------ shapes ------- */

    /** Horizontal gradient (left → right) built from vertical slices. */
    public static void fillHorizontalGradient(DrawContext ctx, int x, int y, int width, int height,
                                             int colorLeft, int colorRight) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int slices = Math.min(48, Math.max(1, width));
        int sliceWidth = (int) Math.ceil(width / (double) slices);
        for (int i = 0; i < slices; i++) {
            int sliceX = x + i * sliceWidth;
            int sliceW = Math.min(sliceWidth, x + width - sliceX);
            if (sliceW <= 0) {
                break;
            }
            float t = slices == 1 ? 0F : (float) i / (float) (slices - 1);
            ctx.fill(sliceX, y, sliceX + sliceW, y + height,
                    AzyronConstants.lerpColor(colorLeft, colorRight, t));
        }
    }

    /** Filled rounded rectangle with a stepped anti-aliased corner. */
    public static void fillRoundedRect(DrawContext ctx, int x, int y, int width, int height,
                                       int radius, int color) {
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (r == 0) {
            ctx.fill(x, y, x + width, y + height, color);
            return;
        }
        // Center body + edge bars.
        ctx.fill(x + r, y, x + width - r, y + height, color);
        ctx.fill(x, y + r, x + r, y + height - r, color);
        ctx.fill(x + width - r, y + r, x + width, y + height - r, color);
        // Corners: horizontal scanline approximation of a quarter circle.
        for (int dy = 0; dy < r; dy++) {
            double dyCenter = r - dy - 0.5D;
            int inset = (int) Math.floor(r - Math.sqrt(Math.max(0D, r * (double) r - dyCenter * dyCenter)));
            ctx.fill(x + inset, y + dy, x + r, y + dy + 1, color);                    // top-left
            ctx.fill(x + width - r, y + dy, x + width - inset, y + dy + 1, color);    // top-right
            ctx.fill(x + inset, y + height - dy - 1, x + r, y + height - dy, color);  // bottom-left
            ctx.fill(x + width - r, y + height - dy - 1, x + width - inset, y + height - dy, color);
        }
    }

    /** Rounded rectangle outline (1px). */
    public static void drawRoundedBorder(DrawContext ctx, int x, int y, int width, int height,
                                         int radius, int color) {
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        // Straight edges.
        ctx.fill(x + r, y, x + width - r, y + 1, color);
        ctx.fill(x + r, y + height - 1, x + width - r, y + height, color);
        ctx.fill(x, y + r, x + 1, y + height - r, color);
        ctx.fill(x + width - 1, y + r, x + width, y + height - r, color);
        // Quarter-circle corners.
        for (int dy = 0; dy < r; dy++) {
            double dyCenter = r - dy - 0.5D;
            int inset = (int) Math.floor(r - Math.sqrt(Math.max(0D, r * (double) r - dyCenter * dyCenter)));
            ctx.fill(x + inset, y + dy, x + inset + 1, y + dy + 1, color);
            ctx.fill(x + width - inset - 1, y + dy, x + width - inset, y + dy + 1, color);
            ctx.fill(x + inset, y + height - dy - 1, x + inset + 1, y + height - dy, color);
            ctx.fill(x + width - inset - 1, y + height - dy - 1, x + width - inset, y + height - dy, color);
        }
    }

    /** Translucent HUD panel: fill + hairline border. */
    public static void fillHudPanel(DrawContext ctx, int x, int y, int width, int height, double opacity) {
        int alpha = (int) (Math.max(0D, Math.min(0.9D, opacity)) * 255.0D);
        int fill = (alpha << 24) | 0x0D0A1A;
        int border = 0x703B2A63;
        fillRoundedRect(ctx, x, y, width, height, 5, fill);
        drawRoundedBorder(ctx, x, y, width, height, 5, border);
    }

    /** Standard menu panel: dark fill, 1px border and a purple→electric accent line on top. */
    public static void fillMenuPanel(DrawContext ctx, int x, int y, int width, int height, int radius) {
        fillRoundedRect(ctx, x, y, width, height, radius, AzyronConstants.COLOR_PANEL);
        drawRoundedBorder(ctx, x, y, width, height, radius, AzyronConstants.COLOR_PANEL_BORDER);
        fillHorizontalGradient(ctx, x + radius, y, width - radius * 2, 2,
                AzyronConstants.COLOR_PURPLE, AzyronConstants.COLOR_ELECTRIC);
    }

    /** Fake outer glow around a rounded rect, used on hover / for the logo. */
    public static void fillGlow(DrawContext ctx, int x, int y, int width, int height, int radius, int color) {
        for (int i = 3; i >= 1; i--) {
            int alphaStep = 0x18 * (4 - i);
            int glowColor = (alphaStep << 24) | (color & 0x00FFFFFF);
            int grow = i * 2;
            drawRoundedBorder(ctx, x - grow, y - grow, width + grow * 2, height + grow * 2,
                    radius + grow, glowColor);
        }
    }

    /* ------------------------------------------------- text ------- */

    /** Draw a left-aligned string with per-glyph gradient colors. Returns the end cursor x. */
    public static int drawGradientText(DrawContext ctx, TextRenderer textRenderer, String text,
                                       int x, int y, int fromRgb, int toRgb, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return x;
        }
        int cursor = x;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            float t = len == 1 ? 0F : (float) i / (float) (len - 1);
            int color = AzyronConstants.lerpColor(fromRgb, toRgb, t);
            String ch = text.substring(i, i + 1);
            Texts.draw(ctx, textRenderer, ch, cursor, y, 0xFF000000 | (color & 0x00FFFFFF), shadow);
            cursor += textRenderer.getWidth(ch);
        }
        return cursor;
    }

    public static int textWidth(TextRenderer textRenderer, String text) {
        return textRenderer.getWidth(text == null ? "" : text);
    }
}
