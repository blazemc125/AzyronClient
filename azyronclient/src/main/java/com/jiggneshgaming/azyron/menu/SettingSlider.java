package com.jiggneshgaming.azyron.menu;

import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.util.RenderUtil;
import com.jiggneshgaming.azyron.util.Texts;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * AZYRON CLIENT — compact settings slider (label drawn by owner screen).
 */
public final class SettingSlider {

    public final String id;
    public final double min;
    public final double max;
    public final double step;
    public int x;
    public int y;
    public int width;

    public SettingSlider(String id, double min, double max, double step, int x, int y, int width) {
        this.id = id;
        this.min = min;
        this.max = max;
        this.step = step;
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public int height() {
        return 14;
    }

    public double snap(double value) {
        double clamped = Math.max(min, Math.min(max, value));
        return Math.round(clamped / step) * step;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x - 2 && mouseX < x + width + 2 && mouseY >= y - 4 && mouseY < y + height() + 4;
    }

    /** Value for mouse x position along the track. */
    public double valueForMouse(double mouseX) {
        float t = (float) ((mouseX - x) / Math.max(1, width));
        return snap(min + (max - min) * clamp01(t));
    }

    private static float clamp01(float v) {
        return Math.max(0F, Math.min(1F, v));
    }

    public void render(DrawContext ctx, TextRenderer tr, double value, String valueLabel) {
        float t = (float) ((snap(value) - min) / Math.max(0.0001D, max - min));
        // Track
        RenderUtil.fillRoundedRect(ctx, x, y + 4, width, 6, 3, 0xFF241A3E);
        int fillWidth = Math.round(width * t);
        if (fillWidth > 0) {
            RenderUtil.fillHorizontalGradient(ctx, x, y + 4, fillWidth, 6,
                    AzyronConstants.COLOR_PURPLE, AzyronConstants.COLOR_ELECTRIC);
        }
        // Knob
        int knobX = x + fillWidth;
        RenderUtil.fillRoundedRect(ctx, knobX - 4, y + 1, 8, 12, 4, 0xFFF8FAFC);
        RenderUtil.drawRoundedBorder(ctx, knobX - 4, y + 1, 8, 12, 4, AzyronConstants.COLOR_ELECTRIC);
        // Value label (right of knob area)
        if (valueLabel != null) {
            Texts.draw(ctx, tr, valueLabel, x + width + 6, y + 3, AzyronConstants.COLOR_ELECTRIC, false);
        }
    }
}
