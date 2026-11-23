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
 *
 * <p>V2: thickness, gap, outline and dynamic (movement-reactive) spread are
 * all user-configurable and applied live.</p>
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
        float thickness = (float) config.crosshairThickness;
        float gapMult = (float) config.crosshairGap;
        boolean outline = config.crosshairOutline;
        int color = config.crosshairColor;

        int cx = width / 2;
        int cy = height / 2;

        // Dynamic spread — the faster you move, the wider the crosshair gaps.
        float dynamic = dynamicSpread(client, config);

        switch (style) {
            case DOT:
                drawDot(ctx, cx, cy, size, thickness, color);
                break;
            case PLUS:
                drawPlus(ctx, cx, cy, size, thickness, gapMult * dynamic, color, outline);
                break;
            case CLASSIC:
                drawClassic(ctx, cx, cy, size, thickness, gapMult * dynamic, color, outline);
                break;
            case CIRCLE:
                drawCircle(ctx, cx, cy, size, thickness, gapMult * dynamic, color, outline);
                break;
            case T_SHAPE:
                drawTShape(ctx, cx, cy, size, thickness, gapMult * dynamic, color, outline);
                break;
            default:
                break;
        }
    }

    /** 1.0 = standing still, up to 2.2 when sprint-moving (dynamic mode only). */
    private static float dynamicSpread(MinecraftClient client, AzyronConfigModel config) {
        if (!config.crosshairDynamic) {
            return 1.0F;
        }
        try {
            if (client == null || client.player == null) {
                return 1.0F;
            }
            double horizontal = client.player.getVelocity().horizontalLength();
            float t = (float) Math.min(1.0D, horizontal / 0.28D); // 0.28 ~ sprint speed
            float attack = client.player.handSwinging ? 0.45F : 0F;
            return 1.0F + 1.6F * t + attack;
        } catch (Throwable t) {
            return 1.0F;
        }
    }

    private static void drawDot(DrawContext ctx, int cx, int cy, float size, float thickness, int color) {
        int radius = Math.max(1, Math.round(1.6F * size * thickness));
        fillCircle(ctx, cx, cy, radius, color, true);
    }

    private static void drawPlus(DrawContext ctx, int cx, int cy, float size, float thickness,
                                 float gapMult, int color, boolean outline) {
        int armLength = Math.round(6 * size);
        int line = Math.max(1, Math.round(1.2F * size * thickness));
        int gap = Math.max(1, Math.round(2.2F * size * gapMult));
        int half = line / 2;
        if (outline) {
            // Outline pass first — slightly larger dark bars behind each arm.
            ctx.fill(cx - half - 1, cy - gap - armLength - 1, cx + half + 2, cy - gap + 1, 0xC0000000);
            ctx.fill(cx - half - 1, cy + gap, cx + half + 2, cy + gap + armLength + 2, 0xC0000000);
            ctx.fill(cx - gap - armLength - 1, cy - half - 1, cx - gap + 1, cy + half + 2, 0xC0000000);
            ctx.fill(cx + gap, cy - half - 1, cx + gap + armLength + 2, cy + half + 2, 0xC0000000);
        }
        // Up / down / left / right bars (cross with center gap).
        ctx.fill(cx - half, cy - gap - armLength, cx + half + 1, cy - gap, color);
        ctx.fill(cx - half, cy + gap + 1, cx + half + 1, cy + gap + 1 + armLength, color);
        ctx.fill(cx - gap - armLength, cy - half, cx - gap, cy + half + 1, color);
        ctx.fill(cx + gap + 1, cy - half, cx + gap + 1 + armLength, cy + half + 1, color);
        // Center dot
        ctx.fill(cx - half, cy - half, cx + half + 1, cy + half + 1, color);
    }

    private static void drawClassic(DrawContext ctx, int cx, int cy, float size, float thickness,
                                    float gapMult, int color, boolean outline) {
        // Classic "four ticks + dot" esports crosshair.
        int tickLength = Math.round(5.5F * size);
        int line = Math.max(1, Math.round(1.1F * size * thickness));
        int gap = Math.max(2, Math.round(2.6F * size * gapMult));
        int half = line / 2;
        int outlineColor = outline ? 0xC0000000 : 0x00000000;
        if (outline) {
            ctx.fill(cx - half - 1, cy - gap - tickLength - 1, cx + half + 2, cy - gap + 1, outlineColor);
            ctx.fill(cx - half - 1, cy + gap, cx + half + 2, cy + gap + tickLength + 2, outlineColor);
            ctx.fill(cx - gap - tickLength - 1, cy - half - 1, cx - gap + 1, cy + half + 2, outlineColor);
            ctx.fill(cx + gap, cy - half - 1, cx + gap + tickLength + 2, cy + half + 2, outlineColor);
        } else {
            // Plain bars — still readable on vanilla terrain.
            ctx.fill(cx - half, cy - gap - tickLength, cx + half + 1, cy - gap, color);
            ctx.fill(cx - half, cy + gap + 1, cx + half + 1, cy + gap + 1 + tickLength, color);
            ctx.fill(cx - gap - tickLength, cy - half, cx - gap, cy + half + 1, color);
            ctx.fill(cx + gap + 1, cy - half, cx + gap + 1 + tickLength, cy + half + 1, color);
        }
        if (outline) {
            ctx.fill(cx - half, cy - gap - tickLength, cx + half + 1, cy - gap, color);
            ctx.fill(cx - half, cy + gap + 1, cx + half + 1, cy + gap + 1 + tickLength, color);
            ctx.fill(cx - gap - tickLength, cy - half, cx - gap, cy + half + 1, color);
            ctx.fill(cx + gap + 1, cy - half, cx + gap + 1 + tickLength, cy + half + 1, color);
        }
        int dot = Math.max(1, Math.round(1.1F * size * thickness));
        if (outline) {
            ctx.fill(cx - dot - 1, cy - dot - 1, cx + dot + 2, cy + dot + 2, outlineColor);
        }
        ctx.fill(cx - dot, cy - dot, cx + dot + 1, cy + dot + 1, color);
    }

    private static void drawCircle(DrawContext ctx, int cx, int cy, float size, float thickness,
                                   float gapMult, int color, boolean outline) {
        int radius = Math.max(3, Math.round(5.0F * size * gapMult));
        if (outline) {
            fillCircle(ctx, cx, cy, radius + 1, 0xA0000000, false);
        }
        for (int i = 0; i < Math.round(thickness); i++) {
            fillCircle(ctx, cx, cy, radius - i, color, false);
        }
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, color);
    }

    private static void drawTShape(DrawContext ctx, int cx, int cy, float size, float thickness,
                                   float gapMult, int color, boolean outline) {
        int halfWidth = Math.round(7.0F * size * gapMult);
        int line = Math.max(1, Math.round(1.3F * size * thickness));
        int drop = Math.round(6.0F * size);
        int half = line / 2;
        if (outline) {
            ctx.fill(cx - halfWidth - 1, cy - half - 1, cx + halfWidth + 2, cy + half + 2, 0xC0000000);
            ctx.fill(cx - half - 1, cy, cx + half + 2, cy + drop + 2, 0xC0000000);
        }
        ctx.fill(cx - halfWidth, cy - half, cx + halfWidth + 1, cy + half + 1, color); // horizontal bar
        ctx.fill(cx - half, cy, cx + half + 1, cy + drop, color);                       // vertical drop
    }

    /** Render the live crosshair into an arbitrary preview box (used by the settings preview). */
    public static void renderPreview(DrawContext ctx, int centerX, int centerY, AzyronConfigModel config) {
        int width = ClientRefs.scaledWidth(MinecraftClient.getInstance());
        int height = ClientRefs.scaledHeight(MinecraftClient.getInstance());
        // Reuse the in-game renderer but shift coordinates by offsetting draw calls:
        // simplest honest preview = draw current settings centered at (centerX, centerY)
        // using a clipped translated matrix.
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) (centerX - width / 2), (float) (centerY - height / 2));
        render(ctx);
        ctx.getMatrices().popMatrix();
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
