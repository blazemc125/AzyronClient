package com.jiggneshgaming.azyron.menu;

import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.util.RenderUtil;
import com.jiggneshgaming.azyron.util.Texts;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * AZYRON CLIENT — custom premium button widget.
 *
 * <p>Rounded panel button with hover glow and electric-blue accent.
 * Hit detection is manual so buttons can be laid out freely.</p>
 */
public final class AzyronButton {

    public String label;
    public int x;
    public int y;
    public int width;
    public int height;
    public boolean enabled = true;
    /** Optional small "chip" text drawn on the left (e.g. hotkey hint). */
    public String leftHint;
    /** Click action — invoked by the owning screen. */
    public Runnable action;

    public AzyronButton(String label, int x, int y, int width, int height) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
        boolean hover = enabled && contains(mouseX, mouseY);
        int fillTop = enabled ? (hover ? 0xF03A2459 : 0xE01E1436) : 0xD0140D24;
        int fillBottom = enabled ? (hover ? 0xF02A1A45 : 0xE0160F2C) : 0xD0100A1E;
        int border = hover ? AzyronConstants.COLOR_ELECTRIC : AzyronConstants.COLOR_PANEL_BORDER;
        int textColor = enabled ? AzyronConstants.COLOR_TEXT : AzyronConstants.COLOR_TEXT_DIM;

        // Body
        RenderUtil.fillRoundedRect(ctx, x, y, width, height, 6, fillBottom);
        ctx.fill(x + 6, y, x + width - 6, y + Math.max(2, height / 3), fillTop);
        RenderUtil.drawRoundedBorder(ctx, x, y, width, height, 6, border);

        if (hover) {
            RenderUtil.fillGlow(ctx, x, y, width, height, 6, AzyronConstants.COLOR_ELECTRIC);
        }

        // Left hint chip
        if (leftHint != null && !leftHint.isEmpty()) {
            int chipWidth = tr.getWidth(leftHint) + 10;
            int chipY = y + (height - 12) / 2;
            RenderUtil.fillRoundedRect(ctx, x + 6, chipY, chipWidth, 12, 3, 0x5533CCFF);
            Texts.draw(ctx, tr, leftHint, x + 11, chipY + 2, AzyronConstants.COLOR_ELECTRIC, false);
        }

        // Label (centered, ignoring the hint chip reservation for balance)
        Texts.drawCentered(ctx, tr, label, x + width / 2, y + (height - 9) / 2 + 1, textColor, true);

        // Chevron
        if (enabled) {
            String chevron = "›";
            Texts.draw(ctx, tr, chevron, x + width - tr.getWidth(chevron) - 8,
                    y + (height - 9) / 2 + 1,
                    hover ? AzyronConstants.COLOR_ELECTRIC : 0xFF4B3B76, true);
        }
    }
}
