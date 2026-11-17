package com.jiggneshgaming.azyron.crosshair;

import com.jiggneshgaming.azyron.config.AzyronConfig;
import com.jiggneshgaming.azyron.core.config.AzyronConfigModel;
import com.jiggneshgaming.azyron.util.ClientRefs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT — custom crosshair.
 *
 * <p>Drawn from the AZYRON HUD hook, centered on screen. The vanilla
 * crosshair is suppressed by {@code MixinCrosshairHud} (optional mixin —
 * if it fails to apply you may briefly see both, never a crash).</p>
 */
public final class Crosshairs {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/crosshair");

    private Crosshairs() {
    }

    public static boolean shouldHideVanilla() {
        AzyronConfigModel config = AzyronConfig.get();
        return config.hideVanillaCrosshair && !"VANILLA".equals(config.crosshairStyle);
    }

    /** Called from the HUD hook — draws only when centered on screen. */
    public static void render(DrawContext ctx) {
        AzyronConfigModel config = AzyronConfig.get();
        String styleName = config.crosshairStyle == null ? "CLASSIC" : config.crosshairStyle;
        AzyronConfigModel.CrosshairStyle style = AzyronConfigModel.CrosshairStyle.fromString(styleName);
        if (style == AzyronConfigModel.CrosshairStyle.VANILLA) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        int width = ClientRefs.scaledWidth(client);
        int height = ClientRefs.scaledHeight(client);
        float size = (float) config.crosshairSize;
        int color = config.crosshairColor;

        int cx = width / 2;
        int cy = height / 2;

        switch (style) {
            case DOT:
                drawDot(ctx, cx, cy, size, color);
                break;
            case PLUS:
                drawPlus(ctx, cx, cy, size, color, 3);
                break;
            case CLASSIC:
                drawClassic(ctx, cx, cy, size, color);
                break;
            case CIRCLE:
                drawCircle(ctx, cx, cy, size, color);
                break;
            case T_SHAPE:
                drawTShape(ctx, cx, cy, size, color);
                break;
            default:
                break;
        }
    }

    private static void drawDot(DrawContext ctx, int cx, int cy, float size, int color) {
        int radius = Math.max(1, Math.round(1.6F * size));
        fillCircle(ctx, cx, cy, radius, color, true);
    }

    private static void drawPlus(DrawContext ctx, int cx, int cy, float size, int color, int arm) {
        int armLength = Math.round((arm + 3) * size);
        int thickness = Math.max(1, Math.round(1.2F * size));
        int gap = Math.max(1, Math.round(2.2F * size));
        int half = thickness / 2;
        // Up / down / left / right bars (cross with center gap).
        ctx.fill(cx - half, cy - gap - armLength, cx + half + 1, cy - gap, color);
        ctx.fill(cx - half, cy + gap + 1, cx + half + 1, cy + gap + 1 + armLength, color);
        ctx.fill(cx - gap - armLength, cy - half, cx - gap, cy + half + 1, color);
        ctx.fill(cx + gap + 1, cy - half, cx + gap + 1 + armLength, cy + half + 1, color);
        // Center dot
        ctx.fill(cx - half, cy - half, cx + half + 1, cy + half + 1, color);
    }

    private static void drawClassic(DrawContext ctx, int cx, int cy, float size, int color) {
        // Classic "four ticks + dot" esports crosshair.
        int tickLength = Math.round(5.5F * size);
        int thickness = Math.max(1, Math.round(1.1F * size));
        int gap = Math.max(2, Math.round(2.6F * size));
        int half = thickness / 2;
        int outline = 0xC0000000;
        // Outlined ticks for visibility on any background.
        drawOutlinedBar(ctx, cx - half, cy - gap - tickLength, cx + half + 1, cy - gap, color, outline);  // top
        drawOutlinedBar(ctx, cx - half, cy + gap + 1, cx + half + 1, cy + gap + 1 + tickLength, color, outline); // bottom
        drawOutlinedBar(ctx, cx - gap - tickLength, cy - half, cx - gap, cy + half + 1, color, outline);  // left
        drawOutlinedBar(ctx, cx + gap + 1, cy - half, cx + gap + 1 + tickLength, cy + half + 1, color, outline); // right
        int dot = Math.max(1, Math.round(1.1F * size));
        ctx.fill(cx - dot, cy - dot, cx + dot + 1, cy + dot + 1, color);
    }

    private static void drawCircle(DrawContext ctx, int cx, int cy, float size, int color) {
        int radius = Math.max(3, Math.round(5.0F * size));
        fillCircle(ctx, cx, cy, radius, color, false);
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, color);
    }

    private static void drawTShape(DrawContext ctx, int cx, int cy, float size, int color) {
        int halfWidth = Math.round(7.0F * size);
        int thickness = Math.max(1, Math.round(1.3F * size));
        int drop = Math.round(6.0F * size);
        int half = thickness / 2;
        ctx.fill(cx - halfWidth, cy - half, cx + halfWidth + 1, cy + half + 1, color); // horizontal bar
        ctx.fill(cx - half, cy, cx + half + 1, cy + drop, color);                       // vertical drop
    }

    private static void drawOutlinedBar(DrawContext ctx, int x1, int y1, int x2, int y2,
                                        int fill, int outline) {
        ctx.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, outline);
        ctx.fill(x1, y1, x2, y2, fill);
    }

    private static void fillCircle(DrawContext ctx, int cx, int cy, int radius, int color, boolean filled) {
        for (int dy = -radius; dy <= radius; dy++) {
            double dyCenter = dy - 0.5D;
            double span = Math.sqrt(Math.max(0.0D, radius * (double) radius - dyCenter * dyCenter));
            int halfChord = filled ? (int) Math.floor(span) : Math.max(0, (int) Math.floor(span) - 1);
            if (filled) {
                ctx.fill(cx - halfChord, cy + dy, cx + halfChord + 1, cy + dy + 1, color);
            } else {
                // Ring only — draw the edge pixels of the scanline.
                ctx.fill(cx - halfChord, cy + dy, cx - halfChord + 1, cy + dy + 1, color);
                ctx.fill(cx + halfChord, cy + dy, cx + halfChord + 1, cy + dy + 1, color);
            }
        }
    }
}
