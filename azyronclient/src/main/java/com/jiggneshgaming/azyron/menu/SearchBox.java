package com.jiggneshgaming.azyron.menu;

import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.util.RenderUtil;
import com.jiggneshgaming.azyron.util.Texts;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * AZYRON CLIENT — minimal custom search input.
 *
 * <p>Plain text field drawn with fills; full keyboard support comes from the
 * owning screen via {@link #charTyped} and {@link #keyPressed}.</p>
 */
public final class SearchBox {

    public String text = "";
    public boolean focused;
    public int x;
    public int y;
    public int width;
    public int height;
    public String placeholder = "Search...";

    public SearchBox(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void clear() {
        text = "";
    }

    public void charTyped(char chr) {
        if (chr >= 32 && chr != 127 && text.length() < 64) {
            text += chr;
        }
    }

    /** @return true if the key was consumed */
    public boolean keyPressed(int keyCode) {
        if (!focused) {
            return false;
        }
        if (keyCode == 259 || keyCode == 8) { // GLFW_KEY_BACKSPACE (259) or legacy 8
            if (text.length() > 0) {
                text = text.substring(0, text.length() - 1);
            }
            return true;
        }
        if (keyCode == 257 || keyCode == 335) { // GLFW_KEY_ENTER / KP_ENTER
            focused = false;
            return true;
        }
        return false;
    }

    public void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
        boolean hover = contains(mouseX, mouseY);
        int border = focused ? AzyronConstants.COLOR_ELECTRIC : (hover ? 0xFF54407C : AzyronConstants.COLOR_PANEL_BORDER);
        RenderUtil.fillRoundedRect(ctx, x, y, width, height, 5, 0xC0100A1E);
        RenderUtil.drawRoundedBorder(ctx, x, y, width, height, 5, border);

        int textY = y + (height - 9) / 2 + 1;
        // Magnifier glyph
        drawMagnifier(ctx, x + 8, y + height / 2, focused ? AzyronConstants.COLOR_ELECTRIC : 0xFF9CA3AF);

        if (text.isEmpty() && !focused) {
            Texts.draw(ctx, tr, placeholder, x + 22, textY, 0xFF6B7280, false);
        } else {
            String shown = text;
            int maxTextWidth = width - 30;
            // Trim from the left so the caret stays visible.
            while (!shown.isEmpty() && tr.getWidth(shown) > maxTextWidth) {
                shown = shown.substring(1);
            }
            Texts.draw(ctx, tr, shown, x + 22, textY, AzyronConstants.COLOR_TEXT, false);
            if (focused && (System.currentTimeMillis() / 530L) % 2L == 0L) {
                int caretX = x + 22 + tr.getWidth(shown) + 1;
                ctx.fill(caretX, y + 4, caretX + 1, y + height - 4, AzyronConstants.COLOR_ELECTRIC);
            }
        }
    }

    private static void drawMagnifier(DrawContext ctx, int cx, int cy, int color) {
        // Ring
        int r = 4;
        for (int dy = -r; dy <= r; dy++) {
            double dyC = dy - 0.5D;
            double span = Math.sqrt(Math.max(0.0D, r * (double) r - dyC * dyC));
            int outer = (int) Math.floor(span);
            double innerSpan = Math.sqrt(Math.max(0.0D, (r - 1) * (double) (r - 1) - dyC * dyC));
            int inner = (int) Math.floor(innerSpan);
            ctx.fill(cx - outer, cy + dy, cx - inner + 1, cy + dy + 1, color);
            ctx.fill(cx + inner, cy + dy, cx + outer + 1, cy + dy + 1, color);
        }
        // Handle
        ctx.fill(cx + 3, cy + 3, cx + 6, cy + 4, color);
        ctx.fill(cx + 4, cy + 4, cx + 7, cy + 5, color);
    }
}
