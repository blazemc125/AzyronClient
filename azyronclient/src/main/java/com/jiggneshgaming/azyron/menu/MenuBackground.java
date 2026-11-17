package com.jiggneshgaming.azyron.menu;

import com.jiggneshgaming.azyron.AzyronConstants;
import net.minecraft.client.gui.DrawContext;

/**
 * AZYRON CLIENT — procedural premium background for all AZYRON screens.
 *
 * <p>Layered vertical gradient + nebula glow orbs + twinkling star field +
 * bottom vignette. Pure rectangle fills; no shaders, no textures.</p>
 */
public final class MenuBackground {

    private static final int STAR_COUNT = 96;
    private static final float[] STAR_X = new float[STAR_COUNT];
    private static final float[] STAR_Y = new float[STAR_COUNT];
    private static final float[] STAR_PHASE = new float[STAR_COUNT];
    private static final float[] STAR_SPEED = new float[STAR_COUNT];
    private static final int[] STAR_SIZE = new int[STAR_COUNT];

    static {
        java.util.Random random = new java.util.Random(1337L);
        for (int i = 0; i < STAR_COUNT; i++) {
            STAR_X[i] = random.nextFloat();
            STAR_Y[i] = random.nextFloat();
            STAR_PHASE[i] = random.nextFloat() * (float) (Math.PI * 2.0D);
            STAR_SPEED[i] = 0.9F + random.nextFloat() * 1.7F;
            STAR_SIZE[i] = random.nextFloat() < 0.85F ? 1 : 2;
        }
    }

    private MenuBackground() {
    }

    public static void render(DrawContext ctx, int width, int height, long timeMs) {
        // 1) Deep vertical gradient (64 bands).
        int bands = 64;
        int bandHeight = Math.max(1, height / bands);
        for (int i = 0; i < bands; i++) {
            float t = (float) i / (float) (bands - 1);
            int color = AzyronConstants.lerpColor(AzyronConstants.COLOR_BG_TOP, AzyronConstants.COLOR_BG_BOTTOM, t);
            int y = i * bandHeight;
            ctx.fill(0, y, width, Math.min(height, y + bandHeight + 1), color);
        }

        // 2) Nebula glow orbs (translucent circles).
        fillGlowOrb(ctx, (int) (width * 0.16F), (int) (height * 0.78F), Math.max(90, height / 3), 0x2A7C3AED);
        fillGlowOrb(ctx, (int) (width * 0.86F), (int) (height * 0.18F), Math.max(80, height / 4), 0x2233CCFF);
        fillGlowOrb(ctx, (int) (width * 0.52F), (int) (height * 0.45F), Math.max(120, height / 2), 0x144C1D95);

        // 3) Star field with twinkle.
        float time = (timeMs % 100_000L) / 1000.0F;
        for (int i = 0; i < STAR_COUNT; i++) {
            float twinkle = 0.35F + 0.65F * (0.5F + 0.5F * (float) Math.sin(time * STAR_SPEED[i] + STAR_PHASE[i]));
            int alpha = (int) (twinkle * 200.0F);
            int x = (int) (STAR_X[i] * width);
            int y = (int) (STAR_Y[i] * height * 0.72F);
            int star = (alpha << 24) | 0xEDE9FE;
            int size = STAR_SIZE[i];
            ctx.fill(x, y, x + size, y + size, star);
        }

        // 4) Bottom vignette for text readability.
        fillVerticalBand(ctx, 0, (int) (height * 0.72F), width, height - (int) (height * 0.72F),
                0x00000000, 0x99000000);
    }

    /** Soft radial orb built from stacked translucent rings. */
    private static void fillGlowOrb(DrawContext ctx, int cx, int cy, int radius, int argb) {
        int rings = 7;
        for (int i = rings; i >= 1; i--) {
            float t = (float) i / (float) rings;
            int alpha = (int) (((argb >>> 24) & 0xFF) * t * 0.55F);
            int color = (alpha << 24) | (argb & 0x00FFFFFF);
            int r = (int) (radius * t);
            fillCircle(ctx, cx, cy, r, color);
        }
    }

    private static void fillCircle(DrawContext ctx, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy += 1) {
            double dyCenter = dy - 0.5D;
            int halfChord = (int) Math.floor(Math.sqrt(Math.max(0.0D, radius * (double) radius - dyCenter * dyCenter)));
            ctx.fill(cx - halfChord, cy + dy, cx + halfChord + 1, cy + dy + 1, color);
        }
    }

    private static void fillVerticalBand(DrawContext ctx, int x, int y, int width, int height,
                                         int colorTop, int colorBottom) {
        if (height <= 0) {
            return;
        }
        int bands = Math.min(48, height);
        int bandHeight = Math.max(1, height / bands);
        for (int i = 0; i < bands; i++) {
            float t = (float) i / (float) Math.max(1, bands - 1);
            int y0 = y + i * bandHeight;
            ctx.fill(x, y0, x + width, Math.min(y + height, y0 + bandHeight + 1),
                    AzyronConstants.lerpColor(colorTop, colorBottom, t));
        }
    }
}
