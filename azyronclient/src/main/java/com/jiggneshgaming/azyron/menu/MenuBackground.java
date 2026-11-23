package com.jiggneshgaming.azyron.menu;

import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.config.AzyronConfig;
import com.jiggneshgaming.azyron.core.config.AzyronConfigModel;
import net.minecraft.client.gui.DrawContext;

/**
 * AZYRON CLIENT — procedural premium background for all AZYRON screens.
 *
 * <p>Layered vertical gradient + nebula glow orbs + twinkling star field +
 * bottom vignette. Pure rectangle fills; no shaders, no textures. The
 * gradient band colors are cached — they only depend on the viewport
 * height, so the per-frame cost is a handful of array lookups.</p>
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

    private static final int GRADIENT_BANDS = 48;
    private static int[] gradientCache = new int[0];
    private static int gradientCacheHeight = -1;

    private static final int VIGNETTE_BANDS = 12;
    private static int[] vignetteCache = new int[0];
    private static int vignetteCacheHeight = -1;

    private MenuBackground() {
    }

    public static void render(DrawContext ctx, int width, int height, long timeMs) {
        // Animation settings (V2) — LOW and "animations off" collapse every
        // time-based effect to a static, cheap backdrop.
        AzyronConfigModel config = AzyronConfig.get();
        boolean animate = config.animations;
        String quality = config.animationQuality == null ? "MEDIUM" : config.animationQuality;
        boolean lowQuality = "LOW".equals(quality);
        boolean highQuality = "HIGH".equals(quality);
        float orbScale = lowQuality || !animate ? 0.55F : (highQuality ? 1.25F : 1.0F);
        // 1) Deep vertical gradient (cached per window height).
        int[] bands = gradientColors(height);
        int bandHeight = Math.max(1, (int) Math.ceil(height / (double) GRADIENT_BANDS));
        for (int i = 0; i < GRADIENT_BANDS; i++) {
            int y = i * bandHeight;
            if (y >= height) {
                break;
            }
            ctx.fill(0, y, width, Math.min(height, y + bandHeight + 1), bands[i]);
        }

        // 2) Nebula glow orbs (translucent circles, stepped 2px rows).
        fillGlowOrb(ctx, (int) (width * 0.16F), (int) (height * 0.78F), Math.max(90, height / 3), scaleAlpha(0x2A7C3AED, orbScale));
        fillGlowOrb(ctx, (int) (width * 0.86F), (int) (height * 0.18F), Math.max(80, height / 4), scaleAlpha(0x2233CCFF, orbScale));
        fillGlowOrb(ctx, (int) (width * 0.52F), (int) (height * 0.45F), Math.max(120, height / 2), scaleAlpha(0x144C1D95, orbScale));

        // 3) Star field with twinkle (static alpha when animations are off/LOW).
        float time = (timeMs % 100_000L) / 1000.0F;
        int starLimit = (animate && !lowQuality) ? STAR_COUNT : STAR_COUNT / 3;
        for (int i = 0; i < starLimit; i++) {
            float twinkle;
            if (animate && !lowQuality) {
                twinkle = 0.35F + 0.65F * (0.5F + 0.5F * (float) Math.sin(time * STAR_SPEED[i] + STAR_PHASE[i]));
            } else {
                twinkle = 0.5F;
            }
            int alpha = (int) (twinkle * 200.0F);
            int x = (int) (STAR_X[i] * width);
            int y = (int) (STAR_Y[i] * height * 0.72F);
            int star = (alpha << 24) | 0xEDE9FE;
            int size = STAR_SIZE[i];
            ctx.fill(x, y, x + size, y + size, star);
        }

        // 4) Bottom vignette for text readability (cached per window height).
        int vignetteTop = (int) (height * 0.72F);
        int vignetteHeight = height - vignetteTop;
        if (vignetteHeight > 0) {
            int[] vignette = vignetteColors(vignetteHeight);
            int vBandHeight = Math.max(1, (int) Math.ceil(vignetteHeight / (double) VIGNETTE_BANDS));
            for (int i = 0; i < VIGNETTE_BANDS; i++) {
                int y0 = vignetteTop + i * vBandHeight;
                if (y0 >= height) {
                    break;
                }
                ctx.fill(0, y0, width, Math.min(height, y0 + vBandHeight + 1), vignette[i]);
            }
        }
    }

    /** Multiply the alpha channel of an ARGB color. */
    private static int scaleAlpha(int color, float scale) {
        int a = Math.min(255, Math.max(0, (int) (((color >>> 24) & 0xFF) * scale)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static int[] gradientColors(int height) {
        if (gradientCacheHeight != height || gradientCache.length != GRADIENT_BANDS) {
            int[] rebuilt = new int[GRADIENT_BANDS];
            for (int i = 0; i < GRADIENT_BANDS; i++) {
                float t = (float) i / (float) (GRADIENT_BANDS - 1);
                rebuilt[i] = AzyronConstants.lerpColor(AzyronConstants.COLOR_BG_TOP, AzyronConstants.COLOR_BG_BOTTOM, t);
            }
            gradientCache = rebuilt;
            gradientCacheHeight = height;
        }
        return gradientCache;
    }

    private static int[] vignetteColors(int vignetteHeight) {
        if (vignetteCacheHeight != vignetteHeight || vignetteCache.length != VIGNETTE_BANDS) {
            int[] rebuilt = new int[VIGNETTE_BANDS];
            for (int i = 0; i < VIGNETTE_BANDS; i++) {
                float t = (float) i / (float) Math.max(1, VIGNETTE_BANDS - 1);
                rebuilt[i] = AzyronConstants.lerpColor(0x00000000, 0x99000000, t);
            }
            vignetteCache = rebuilt;
            vignetteCacheHeight = vignetteHeight;
        }
        return vignetteCache;
    }

    /** Soft radial orb built from stacked translucent rings. */
    private static void fillGlowOrb(DrawContext ctx, int cx, int cy, int radius, int argb) {
        int rings = 5;
        for (int i = rings; i >= 1; i--) {
            float t = (float) i / (float) rings;
            int alpha = (int) (((argb >>> 24) & 0xFF) * t * 0.55F);
            int color = (alpha << 24) | (argb & 0x00FFFFFF);
            int r = (int) (radius * t);
            fillCircle(ctx, cx, cy, r, color);
        }
    }

    private static void fillCircle(DrawContext ctx, int cx, int cy, int radius, int color) {
        // Step 2px rows; each fill covers 2 rows — half the draw calls.
        for (int dy = -radius; dy <= radius; dy += 2) {
            double dyCenter = dy + 0.5D; // center of the 2-row slab
            int halfChord = (int) Math.floor(Math.sqrt(Math.max(0.0D, radius * (double) radius - dyCenter * dyCenter)));
            ctx.fill(cx - halfChord, cy + dy, cx + halfChord + 1, cy + dy + 2, color);
        }
    }
}
