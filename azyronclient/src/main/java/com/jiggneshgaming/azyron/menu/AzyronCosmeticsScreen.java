package com.jiggneshgaming.azyron.menu;

import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.config.AzyronConfig;
import com.jiggneshgaming.azyron.util.ClientRefs;
import com.jiggneshgaming.azyron.util.RenderUtil;
import com.jiggneshgaming.azyron.core.config.AzyronConfigModel;
import com.jiggneshgaming.azyron.util.Texts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

/**
 * AZYRON CLIENT — cosmetics.
 *
 * <p>Client-side particle auras rendered around your player. Fully
 * functional offline; visible only to you (server sync is a future
 * feature). Selecting a card equips it instantly.</p>
 */
public final class AzyronCosmeticsScreen extends BaseAzyronScreen {

    private static final class Cosmetic {
        final String id;
        final String name;
        final String description;

        Cosmetic(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }
    }

    private static final Cosmetic[] COSMETICS = {
            new Cosmetic("NONE", "None", "No aura — clean look."),
            new Cosmetic("END_SPARKLE", "End Sparkle", "Violet-white ender motes orbit you."),
            new Cosmetic("SOUL_FLAME", "Soul Flame", "Teal soul fire swirls around you."),
            new Cosmetic("EMBER", "Ember Trail", "Warm floating embers on the move."),
            new Cosmetic("SNOWFLAKE", "Snowflake", "Soft frosty flakes drifting by."),
            new Cosmetic("VOID_AURA", "Void Aura", "Dark portal motes collapse into you."),
            new Cosmetic("LIGHTNING_AURA", "Lightning Aura", "Crackling electric arcs surround you.")
    };

    private final SettingSlider intensitySlider =
            new SettingSlider("auraIntensity", 0.5D, 2.5D, 0.25D, 120, 0, 150);
    private boolean draggingIntensity;

    public AzyronCosmeticsScreen(Screen backTarget) {
        super("AZYRON Cosmetics", backTarget);
    }

    @Override
    protected void buildButtons() {
        int x = Math.max(24, (int) (this.width * 0.06D));
        addButton("Back", x, this.height - 40, 120, 22, this::goBack);
    }

    @Override
    protected void renderContent(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY, float delta) {
        Texts.draw(ctx, tr, "AZYRON COSMETICS", Math.max(24, (int) (this.width * 0.06D)), 16,
                AzyronConstants.COLOR_TEXT, true);
        Texts.draw(ctx, tr, "Particle auras — apply instantly, visible to you",
                Math.max(24, (int) (this.width * 0.06D)), 28, AzyronConstants.COLOR_TEXT_DIM, false);

        int ctlY = this.height - 76;
        int ctlX = Math.max(24, (int) (this.width * 0.06D));
        AzyronConfigModel config = AzyronConfig.get();
        Texts.draw(ctx, tr, "PARTICLE INTENSITY", ctlX, ctlY + 3, AzyronConstants.COLOR_TEXT_DIM, true);
        intensitySlider.x = ctlX + 138;
        intensitySlider.y = ctlY;
        intensitySlider.width = 130;
        intensitySlider.render(ctx, tr, config.auraIntensity,
                trimZero(String.format(java.util.Locale.ROOT, "%.2fx", config.auraIntensity)));
        // Low particle mode toggle
        int toggleX = intensitySlider.x + intensitySlider.width + 60;
        drawMiniToggle(ctx, toggleX, ctlY, 28, 13, config.lowParticleMode);
        Texts.draw(ctx, tr, "LOW PARTICLES", toggleX + 36, ctlY + 3,
                config.lowParticleMode ? AzyronConstants.COLOR_ELECTRIC : AzyronConstants.COLOR_TEXT_DIM, true);

        int cardW = 132;
        int cardH = 122;
        int gap = 12;
        int cols = Math.max(1, Math.min(5, (this.width - 48) / (cardW + gap)));
        int startX = Math.max(24, (int) (this.width * 0.06D));
        int startY = 54;

        String equipped = AzyronConfig.get().aura;
        for (int i = 0; i < COSMETICS.length; i++) {
            Cosmetic cosmetic = COSMETICS[i];
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (cardW + gap);
            int y = startY + row * (cardH + gap);
            boolean selected = cosmetic.id.equals(equipped);
            boolean hover = mouseX >= x && mouseX < x + cardW && mouseY >= y && mouseY < y + cardH;
            renderCard(ctx, tr, cosmetic, x, y, cardW, cardH, selected, hover);
        }
    }

    private void renderCard(DrawContext ctx, TextRenderer tr, Cosmetic cosmetic,
                            int x, int y, int w, int h, boolean selected, boolean hover) {
        int fill = selected ? 0xE0281E45 : (hover ? 0xD0241838 : 0xB0181028);
        int border = selected ? AzyronConstants.COLOR_ELECTRIC
                : (hover ? AzyronConstants.COLOR_PURPLE_BRIGHT : AzyronConstants.COLOR_PANEL_BORDER);
        RenderUtil.fillRoundedRect(ctx, x, y, w, h, 8, fill);
        RenderUtil.drawRoundedBorder(ctx, x, y, w, h, 8, border);
        if (selected) {
            RenderUtil.fillGlow(ctx, x, y, w, h, 8, AzyronConstants.COLOR_ELECTRIC);
        }

        // Icon area
        int iconCy = y + 40;
        int iconCx = x + w / 2;
        drawAuraIcon(ctx, cosmetic.id, iconCx, iconCy);

        // Name + description
        Texts.drawCentered(ctx, tr, cosmetic.name, iconCx, y + 68,
                selected ? AzyronConstants.COLOR_ELECTRIC : AzyronConstants.COLOR_TEXT, true);
        wrapCentered(ctx, tr, cosmetic.description, iconCx, y + 80, w - 12, 9,
                AzyronConstants.COLOR_TEXT_DIM, 2);

        // Equipped badge
        if (selected) {
            String badge = "EQUIPPED";
            int bw = tr.getWidth(badge) + 10;
            RenderUtil.fillRoundedRect(ctx, x + w / 2 - bw / 2, y - 5, bw, 11, 4, AzyronConstants.COLOR_ELECTRIC);
            Texts.drawCentered(ctx, tr, badge, x + w / 2, y - 3, 0xFF0B0714, false);
        }
    }

    private void drawAuraIcon(DrawContext ctx, String id, int cx, int cy) {
        switch (id) {
            case "END_SPARKLE":
                drawSparkle(ctx, cx, cy, 9, AzyronConstants.COLOR_ELECTRIC);
                drawSparkle(ctx, cx - 12, cy + 8, 5, 0xFFA78BFA);
                drawSparkle(ctx, cx + 12, cy + 6, 4, 0xFFA78BFA);
                break;
            case "SOUL_FLAME":
                drawFlame(ctx, cx, cy, 0xFF2DD4BF);
                break;
            case "EMBER":
                drawFlame(ctx, cx, cy, 0xFFFB923C);
                break;
            case "SNOWFLAKE":
                drawSnowflake(ctx, cx, cy, 9, 0xFFE0F2FE);
                break;
            case "VOID_AURA":
                drawVoid(ctx, cx, cy);
                break;
            case "LIGHTNING_AURA":
                drawBolt(ctx, cx, cy);
                break;
            default:
                drawNone(ctx, cx, cy, 0xFF475569);
                break;
        }
    }

    private static void drawSparkle(DrawContext ctx, int cx, int cy, int radius, int color) {
        // Four-pointed star — vertical and horizontal bars tapering to tips.
        ctx.fill(cx - 1, cy - radius, cx + 2, cy + radius + 1, color);
        ctx.fill(cx - radius, cy - 1, cx + radius + 1, cy + 2, color);
        int inner = Math.max(1, radius / 2);
        ctx.fill(cx - inner, cy - inner, cx + inner + 1, cy + inner + 1, color);
        ctx.fill(cx, cy - radius - 1, cx + 1, cy + radius + 2, 0xFFFFFFFF);
    }

    private static void drawFlame(DrawContext ctx, int cx, int cy, int color) {
        fillCircle(ctx, cx, cy + 4, 7, color);
        fillCircle(ctx, cx, cy - 2, 5, color);
        fillCircle(ctx, cx, cy - 7, 3, 0xFFFFFFFF);
        fillCircle(ctx, cx, cy + 5, 4, 0xFF0B0714); // hollow core
    }

    private static void drawSnowflake(DrawContext ctx, int cx, int cy, int radius, int color) {
        drawThickLine(ctx, cx - radius, cy, cx + radius, cy, color);
        drawThickLine(ctx, cx - radius + 2, cy - radius + 2, cx + radius - 2, cy + radius - 2, color);
        drawThickLine(ctx, cx - radius + 2, cy + radius - 2, cx + radius - 2, cy - radius + 2, color);
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
    }

    private static void drawVoid(DrawContext ctx, int cx, int cy) {
        // Imploding ring of dark violet — void collapse.
        fillCircle(ctx, cx, cy, 9, 0xFF1E1030);
        fillCircle(ctx, cx, cy, 6, 0xFF3B0764);
        fillCircle(ctx, cx, cy, 3, 0xFF7C3AED);
        fillCircle(ctx, cx, cy, 1, 0xFFFFFFFF);
        drawThickLine(ctx, cx - 13, cy - 13, cx - 9, cy - 9, 0xFF7C3AED);
        drawThickLine(ctx, cx + 13, cy - 13, cx + 9, cy - 9, 0xFF7C3AED);
        drawThickLine(ctx, cx - 13, cy + 13, cx - 9, cy + 9, 0xFF7C3AED);
        drawThickLine(ctx, cx + 13, cy + 13, cx + 9, cy + 9, 0xFF7C3AED);
    }

    private static void drawBolt(DrawContext ctx, int cx, int cy) {
        // Lightning bolt — jagged filled zigzag.
        int[][] shape = {{-2, -10}, {3, -3}, {0, -2}, {4, 8}, {-4, -1}, {-1, -2}, {-5, -10}};
        for (int i = 0; i + 1 < shape.length; i++) {
            drawThickLine(ctx, cx + shape[i][0], cy + shape[i][1],
                    cx + shape[i + 1][0], cy + shape[i + 1][1], 0xFFFDE047);
        }
        drawThickLine(ctx, cx + 3, cy - 3, cx + 4, cy + 8, 0xFFFFFFFF);
    }

    private static String trimZero(String value) {
        String out = value;
        if (out.endsWith("x")) {
            String core = out.substring(0, out.length() - 1);
            while (core.endsWith("0")) {
                core = core.substring(0, core.length() - 1);
            }
            if (core.endsWith(".")) {
                core = core.substring(0, core.length() - 1);
            }
            out = core + "x";
        }
        return out;
    }

    private static void drawMiniToggle(DrawContext ctx, int x, int y, int width, int height, boolean on) {
        RenderUtil.fillRoundedRect(ctx, x, y, width, height, height / 2, on ? 0xFF0E86C9 : 0xFF332A4D);
        int knob = height - 4;
        int knobX = on ? x + width - knob - 2 : x + 2;
        RenderUtil.fillRoundedRect(ctx, knobX, y + 2, knob, knob, knob / 2, 0xFFF8FAFC);
    }

    private static void drawNone(DrawContext ctx, int cx, int cy, int color) {
        int r = 9;
        for (int dy = -r; dy <= r; dy++) {
            double dyC = dy - 0.5D;
            double span = Math.sqrt(Math.max(0.0D, r * (double) r - dyC * dyC));
            int half = (int) Math.floor(span);
            ctx.fill(cx - half, cy + dy, cx - half + 1, cy + dy + 1, color);
            ctx.fill(cx + half - 1, cy + dy, cx + half, cy + dy + 1, color);
        }
        drawThickLine(ctx, cx - 7, cy + 7, cx + 7, cy - 7, color);
    }

    private static void fillCircle(DrawContext ctx, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            double dyC = dy - 0.5D;
            int half = (int) Math.floor(Math.sqrt(Math.max(0.0D, radius * (double) radius - dyC * dyC)));
            ctx.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    private static void drawThickLine(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        int x = x1;
        int y = y1;
        while (true) {
            ctx.fill(x, y, x + 2, y + 2, color);
            if (x == x2 && y == y2) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    /** Word-wrap centered text within maxWidth. */
    private static void wrapCentered(DrawContext ctx, TextRenderer tr, String text, int centerX, int y,
                                     int maxWidth, int lineHeight, int color, int maxLines) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int lineIndex = 0;
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (tr.getWidth(candidate) > maxWidth && line.length() > 0) {
                Texts.drawCentered(ctx, tr, line.toString(), centerX, y + lineIndex * lineHeight, color, false);
                lineIndex++;
                if (lineIndex >= maxLines) {
                    return;
                }
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0 && lineIndex < maxLines) {
            Texts.drawCentered(ctx, tr, line.toString(), centerX, y + lineIndex * lineHeight, color, false);
        }
    }

    /* --------------------------- events ---------------------------- */

    @Override
    protected boolean azyronDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingIntensity && button == 0) {
            AzyronConfig.get().auraIntensity = intensitySlider.valueForMouse(mouseX);
            return true;
        }
        return super.azyronDrag(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    protected boolean azyronRelease(double mouseX, double mouseY, int button) {
        if (draggingIntensity) {
            draggingIntensity = false;
            AzyronConfig.save();
            return true;
        }
        return super.azyronRelease(mouseX, mouseY, button);
    }

    @Override
    protected boolean azyronClick(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int ctlY = this.height - 76;
            int ctlX = Math.max(24, (int) (this.width * 0.06D));
            AzyronConfigModel config = AzyronConfig.get();
            intensitySlider.x = ctlX + 138;
            intensitySlider.y = ctlY;
            intensitySlider.width = 130;
            if (intensitySlider.contains(mouseX, mouseY)) {
                draggingIntensity = true;
                config.auraIntensity = intensitySlider.valueForMouse(mouseX);
                AzyronConfig.save();
                return true;
            }
            int toggleX = intensitySlider.x + intensitySlider.width + 60;
            if (mouseX >= toggleX - 2 && mouseX < toggleX + 30 && mouseY >= ctlY - 2 && mouseY < ctlY + 15) {
                config.lowParticleMode = !config.lowParticleMode;
                AzyronConfig.save();
                return true;
            }
            int cardW = 132;
            int cardH = 122;
            int gap = 12;
            int cols = Math.max(1, Math.min(5, (this.width - 48) / (cardW + gap)));
            int startX = Math.max(24, (int) (this.width * 0.06D));
            int startY = 54;
            for (int i = 0; i < COSMETICS.length; i++) {
                int col = i % cols;
                int row = i / cols;
                int x = startX + col * (cardW + gap);
                int y = startY + row * (cardH + gap);
                if (mouseX >= x && mouseX < x + cardW && mouseY >= y && mouseY < y + cardH) {
                    AzyronConfig.get().aura = COSMETICS[i].id;
                    AzyronConfig.save();
                    return true;
                }
            }
        }
        return super.azyronClick(mouseX, mouseY, button);
    }
}
