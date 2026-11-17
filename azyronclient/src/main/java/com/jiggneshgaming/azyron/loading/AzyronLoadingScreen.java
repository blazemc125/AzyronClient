package com.jiggneshgaming.azyron.loading;

import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.util.ClientRefs;
import com.jiggneshgaming.azyron.util.RenderUtil;
import com.jiggneshgaming.azyron.util.Texts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * AZYRON CLIENT — premium black & white loading screen.
 *
 * <p>Drawn by the splash-overlay mixin on top of (and hiding) the vanilla
 * splash. The progress shown is REAL: it is the vanilla resource reload's
 * own {@code getProgress()}, clamped to be monotonic and smoothed for
 * display. Nothing is ever invented or time-based — the bar can only move
 * forward, and it reaches 100% exactly when the game's own loading is done.
 * The overlay never touches pacing, threads or the reload itself; Minecraft
 * loads exactly as it would without us.</p>
 *
 * <p>Static state lives on this class (the splash overlay is a singleton);
 * it resets itself once the fade-out to the main menu completes, so an
 * in-game resource reload re-animates cleanly.</p>
 */
public final class AzyronLoadingScreen {

    /* ---------------- real progress, monotonic + smoothed --------- */
    private static float shown = 0F;          // what the bar displays
    private static float ceiling = 0F;        // highest raw progress ever seen (bar never goes back)
    private static long firstFrameAt = -1L;   // drives the intro fade
    private static long doneAt = -1L;         // first frame where the reload reported complete

    /* ---------------- cached background gradient ------------------- */
    private static final int BANDS = 40;
    private static int[] bgCache = new int[0];
    private static int bgCacheHeight = -1;

    private static final int STAR_COUNT = 42;
    private static final float[] STAR_X = new float[STAR_COUNT];
    private static final float[] STAR_Y = new float[STAR_COUNT];
    private static final float[] STAR_PHASE = new float[STAR_COUNT];

    static {
        java.util.Random random = new java.util.Random(20260611L);
        for (int i = 0; i < STAR_COUNT; i++) {
            STAR_X[i] = random.nextFloat();
            STAR_Y[i] = random.nextFloat() * 0.72F;
            STAR_PHASE[i] = random.nextFloat() * (float) (Math.PI * 2.0D);
        }
    }

    private AzyronLoadingScreen() {
    }

    /**
     * @param rawProgress vanilla reload progress 0..1 (from ResourceReload.getProgress())
     * @param complete    vanilla reload isComplete() — drives the fade-out to the menu
     */
    public static void render(DrawContext ctx, float rawProgress, boolean complete) {
        long now = System.currentTimeMillis();
        if (firstFrameAt < 0L) {
            firstFrameAt = now;
        }
        float t = (now - firstFrameAt) / 1000.0F;

        // Real progress only: monotonic clamp + critically-damped-ish smoothing.
        ceiling = Math.max(ceiling, clamp01(rawProgress));
        if (complete) {
            ceiling = 1.0F;
        }
        shown += (ceiling - shown) * 0.14F;

        MinecraftClient client = MinecraftClient.getInstance();
        int width = ClientRefs.scaledWidth(client);
        int height = ClientRefs.scaledHeight(client);
        TextRenderer tr = client != null ? client.textRenderer : null;

        /* ---------------- fade-out: game finished loading ---------- */
        if (complete) {
            if (doneAt < 0L) {
                doneAt = now;
            }
            float fade = clamp01((now - doneAt) / 420.0F); // quick premium dissolve
            if (fade >= 1.0F) {
                reset(); // next overlay re-animates from scratch
                return;   // we stop drawing — the main menu takes over
            }
            ctx.fill(0, 0, width, height, withAlpha(0xFF000000, 1.0F - fade));
            return;
        }

        /* ============================================================ */
        /* Background — premium black with a soft white aura behind AS   */
        /* ============================================================ */
        int[] bands = bgBands(height);
        int bandHeight = Math.max(1, (int) Math.ceil(height / (double) BANDS));
        for (int i = 0; i < BANDS; i++) {
            int y = i * bandHeight;
            if (y >= height) {
                break;
            }
            ctx.fill(0, y, width, Math.min(height, y + bandHeight + 1), bands[i]);
        }

        float midX = width / 2.0F;
        float scaleF = clamp(height / 720.0F, 0.62F, 1.30F);

        // Faint dust stars, twinkling very low — atmosphere, not noise.
        for (int i = 0; i < STAR_COUNT; i++) {
            float tw = 0.25F + 0.75F * (0.5F + 0.5F * (float) Math.sin(now * 0.0011 + STAR_PHASE[i]));
            int alpha = (int) (tw * 46);
            if (alpha <= 2) {
                continue;
            }
            int x = (int) (STAR_X[i] * width);
            int y = (int) (STAR_Y[i] * height);
            ctx.fill(x, y, x + 1, y + 1, (alpha << 24) | 0xFFFFFF);
        }

        // Soft white aura behind the logo block.
        int cy = Math.round(height * 0.335F);
        fillGlowOrb(ctx, (int) midX, cy, Math.round(150 * scaleF), 0x0FFFFFFF);

        /* ============================================================ */
        /* The AS monogram — breath + orbit comet + intro zoom          */
        /* ============================================================ */
        float intro = clamp01(t / 0.75F);
        float pop = easeOutBack(intro);            // overshoot pop-in
        float breath = 1.0F + 0.045F * (float) Math.sin(now * 0.0016);
        int baseR = Math.round(clamp(height / 6.4F, 44, 80) * (0.82F + 0.18F * pop) * breath);
        float logoAlpha = clamp01(t / 0.45F);

        drawMonogram(ctx, tr, (int) midX, cy, baseR, now, logoAlpha);

        /* ============================================================ */
        /* WELCOME TO AZYRON CLIENT                                     */
        /* ============================================================ */
        float textAlpha = clamp01((t - 0.22F) / 0.55F);
        if (tr != null && textAlpha > 0F) {
            int welcomeY = cy + baseR + Math.round(26 * scaleF);
            drawTrackedCentered(ctx, tr, "WELCOME TO AZYRON CLIENT", (int) midX, welcomeY,
                    withAlpha(0xFFFFFFFF, textAlpha), 3, 1.42F * clamp(scaleF + 0.25F, 0.85F, 1.25F));
            // Micro tagline
            float tagAlpha = clamp01((t - 0.42F) / 0.55F);
            if (tagAlpha > 0F) {
                drawTrackedCentered(ctx, tr, "P E R F O R M A N C E   \u00B7   S M O O T H N E S S   \u00B7   S T Y L E",
                        (int) midX, welcomeY + Math.round(26 * scaleF),
                        withAlpha(0xFF8A8A8A, tagAlpha), 1, 1.0F);
            }
        }

        /* ============================================================ */
        /* Bottom block — percentage, progress bar, status, footer      */
        /* ============================================================ */
        int barW = Math.min(460, width - Math.round(clamp(width * 0.18F, 84, 220)));
        int barH = Math.max(9, Math.round(11 * scaleF));
        int barX = ((int) midX) - barW / 2;
        int barY = height - Math.round(clamp(height * 0.185F, 92, 148));
        float uiAlpha = clamp01((t - 0.30F) / 0.55F);

        // Percentage
        int pct = Math.round(shown * 100.0F);
        if (tr != null && uiAlpha > 0F) {
            String pctText = pct + "%";
            int pctColor = withAlpha(0xFFFFFFFF, uiAlpha);
            ctx.getMatrices().pushMatrix();
            float s = 1.62F * clamp(scaleF + 0.15F, 0.85F, 1.2F);
            int textW = tr.getWidth(pctText);
            ctx.getMatrices().translate((float) ((int) midX - (int) (textW * s / 2.0F)), (float) (barY - Math.round(34 * scaleF)));
            ctx.getMatrices().scale(s, s);
            Texts.draw(ctx, tr, pctText, 0, 0, pctColor, true);
            ctx.getMatrices().popMatrix();
        }

        // Track
        RenderUtil.fillRoundedRect(ctx, barX, barY, barW, barH, barH / 2, withAlpha(0xFF101010, uiAlpha));
        RenderUtil.drawRoundedBorder(ctx, barX, barY, barW, barH, barH / 2, withAlpha(0xFF2C2C2C, uiAlpha));

        // Fill — real, monotonic progress
        int fillW = Math.round((barW - 4) * shown);
        if (fillW > 3 && uiAlpha > 0F) {
            RenderUtil.fillHorizontalGradient(ctx, barX + 2, barY + 2, fillW, barH - 4,
                    withAlpha(0xFFFFFFFF, uiAlpha), withAlpha(0xFFB8B8B8, uiAlpha));
            // Sliding shimmer band along the filled part — premium touch.
            float cycle = (now % 1400L) / 1400.0F;
            int shimmerX = barX + 2 + Math.round((fillW + 96) * cycle) - 48;
            int sx = Math.max(barX + 2, Math.min(shimmerX, barX + 2 + fillW - 18));
            int sw = Math.min(36, barX + 2 + fillW - sx);
            if (sw > 2) {
                ctx.fill(sx, barY + 2, sx + sw, barY + barH - 2, withAlpha(0x30FFFFFF, uiAlpha));
            }
            // Hot leading tip
            int tipX = barX + 2 + fillW;
            float tipPulse = 0.55F + 0.45F * (float) Math.sin(now * 0.008);
            ctx.fill(tipX - 2, barY + 1, tipX + 1, barY + barH - 1, withAlpha(0xFFFFFFFF, uiAlpha * tipPulse));
        }

        // Status line — the phase label is derived from real progress.
        if (tr != null) {
            String status = statusLabel(ceiling); // phase derived from the real reload progress
            // Animated trailing dots — cosmetic only, progress-free.
            int dots = (int) ((now / 380L) % 4L);
            StringBuilder sb = new StringBuilder(status);
            for (int i = 0; i < dots; i++) {
                sb.append('.');
            }
            Texts.drawCentered(ctx, tr, sb.toString(), (int) midX, barY + barH + Math.round(12 * scaleF),
                    withAlpha(0xFFD4D4D4, uiAlpha), false);
        }

        // Footer — brand, micro type.
        if (tr != null) {
            Texts.drawCentered(ctx, tr,
                    AzyronConstants.NAME + " v" + AzyronConstants.VERSION
                            + "   \u00B7   MC " + AzyronConstants.MINECRAFT_VERSION
                            + "   \u00B7   MADE BY " + AzyronConstants.DEVELOPER,
                    (int) midX, height - 13, 0xFF5B5B5B, false);
        }
    }

    /* ============================================================ */
    /* Monogram                                                       */
    /* ============================================================ */

    private static void drawMonogram(DrawContext ctx, TextRenderer tr, int cx, int cy,
                                     int radius, long now, float alpha) {
        if (alpha <= 0F) {
            return;
        }
        // Base ring — 2px, dim white, quartered with tiny gaps for a tech feel.
        int ringR = radius;
        int segments = 72;
        int gapEvery = 9;
        for (int i = 0; i < segments; i++) {
            if (i % gapEvery == gapEvery - 1) {
                continue; // hairline gap
            }
            double a0 = Math.toRadians(i * (360.0 / segments));
            double a1 = Math.toRadians((i + 1) * (360.0 / segments));
            drawArc(ctx, cx, cy, ringR, a0, a1, 2, withAlpha(0x59FFFFFF, alpha));
        }

        // Inner hairline ring — gently pulsing.
        float pulse = 0.5F + 0.5F * (float) Math.sin(now * 0.0019);
        drawRing(ctx, cx, cy, ringR - 9, 1, withAlpha(0x3AFFFFFF + ((int) (0x36 * pulse) << 24) , alpha));

        // Comet — a 70° arc that orbits the ring, white to transparent.
        double head = Math.toRadians((now * 0.095) % 360.0);
        int trail = 44;
        for (int i = 0; i < trail; i++) {
            double a = head - Math.toRadians(i * 1.6D);
            float k = 1.0F - (i / (float) trail);
            int arcAlpha = (int) (k * k * 235);
            if (arcAlpha <= 4) {
                continue;
            }
            int px = cx + (int) Math.round(Math.cos(a) * ringR);
            int py = cy + (int) Math.round(Math.sin(a) * ringR);
            int size = i < 4 ? 3 : 2;
            ctx.fill(px - size / 2, py - size / 2, px + (size + 1) / 2, py + (size + 1) / 2,
                    ((arcAlpha) << 24) | 0xFFFFFF);
        }

        // "AS" — soft glow pass then crisp glyph, gently breathing scale.
        if (tr != null) {
            float breathe = 1.0F + 0.035F * (float) Math.sin(now * 0.0016 + 0.8);
            float s = (radius / 15.5F) * breathe;
            int glyphW = tr.getWidth("AS");
            int ox = (int) (glyphW * s / 2.0F);
            int oy = (int) (4.5F * s / 2.0F);
            // Glow: 4 diagonal offset passes at low alpha.
            int glow = withAlpha(0x22FFFFFF, alpha);
            for (int[] off : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate((float) (cx - ox + off[0]), (float) (cy - oy + off[1]));
                ctx.getMatrices().scale(s, s);
                Texts.draw(ctx, tr, "AS", 0, 0, glow, false);
                ctx.getMatrices().popMatrix();
            }
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate((float) (cx - ox), (float) (cy - oy));
            ctx.getMatrices().scale(s, s);
            Texts.draw(ctx, tr, "AS", 0, 0, withAlpha(0xFFFFFFFF, alpha), true);
            ctx.getMatrices().popMatrix();
        }

        // Orbiting spark — a single photon circling with two ghosts behind it.
        double orbit = now * 0.00042;
        for (int i = 2; i >= 0; i--) {
            double a = orbit - Math.toRadians(i * 24.0);
            int sparkAlpha = i == 0 ? 235 : (i == 1 ? 110 : 48);
            int px = cx + (int) Math.round(Math.cos(a) * (ringR + 13));
            int py = cy + (int) Math.round(Math.sin(a) * (ringR + 13));
            int size = i == 0 ? 2 : 1;
            ctx.fill(px - size / 2, py - size / 2, px + 1, py + 1,
                    (withAlpha(0xFFFFFFFF, alpha) & 0x00FFFFFF) | (Math.min(255, (int) (sparkAlpha * alpha)) << 24));
        }
    }

    /* ============================================================ */
    /* Phase labels — indicative, progress-derived                    */
    /* ============================================================ */

    private static String statusLabel(float progress) {
        if (progress >= 0.85F) {
            return "Preparing Main Menu";
        } else if (progress >= 0.58F) {
            return "Initializing Renderer";
        } else if (progress >= 0.32F) {
            return "Loading Mods";
        } else if (progress >= 0.13F) {
            return "Loading Fabric";
        }
        return "Initializing Minecraft";
    }

    /* ============================================================ */
    /* Small drawing helpers                                          */
    /* ============================================================ */

    private static int[] bgBands(int height) {
        if (bgCacheHeight != height || bgCache.length != BANDS) {
            int[] rebuilt = new int[BANDS];
            for (int i = 0; i < BANDS; i++) {
                float k = (float) i / (float) (BANDS - 1);
                rebuilt[i] = AzyronConstants.lerpColor(0xFF050505, 0xFF121212, k);
            }
            bgCache = rebuilt;
            bgCacheHeight = height;
        }
        return bgCache;
    }

    /** Soft radial aura built from stacked translucent circles. */
    private static void fillGlowOrb(DrawContext ctx, int cx, int cy, int radius, int argb) {
        int rings = 5;
        for (int i = rings; i >= 1; i--) {
            float k = (float) i / (float) rings;
            int alpha = (int) (((argb >>> 24) & 0xFF) * k * 0.6F);
            int r = (int) (radius * k);
            fillCircle(ctx, cx, cy, r, (alpha << 24) | (argb & 0x00FFFFFF));
        }
    }

    private static void fillCircle(DrawContext ctx, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy += 2) {
            double dyCenter = dy + 0.5D;
            int half = (int) Math.floor(Math.sqrt(Math.max(0.0D, radius * (double) radius - dyCenter * dyCenter)));
            ctx.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 2, color);
        }
    }

    /** Hairline 1px ring (2px stepped rows). */
    private static void drawRing(DrawContext ctx, int cx, int cy, int radius, int thickness, int color) {
        int inner = Math.max(1, radius - thickness);
        for (int dy = -radius; dy <= radius; dy += 2) {
            double dyCenter = dy + 0.5D;
            double outer = Math.sqrt(Math.max(0.0D, radius * (double) radius - dyCenter * dyCenter));
            double in = Math.sqrt(Math.max(0.0D, inner * (double) inner - dyCenter * dyCenter));
            int o = (int) Math.floor(outer);
            int n = (int) Math.floor(in);
            ctx.fill(cx - o, cy + dy, cx - n + 1, cy + dy + 2, color);
            ctx.fill(cx + n, cy + dy, cx + o + 1, cy + dy + 2, color);
        }
    }

    /** Draw a filled annulus segment between two angles (2px stepped). */
    private static void drawArc(DrawContext ctx, int cx, int cy, int radius,
                                double from, double to, int thickness, int color) {
        double step = Math.toRadians(2.2);
        for (double a = from; a < to; a += step) {
            double am = a + step / 2.0D;
            int px = cx + (int) Math.round(Math.cos(am) * radius);
            int py = cy + (int) Math.round(Math.sin(am) * radius);
            ctx.fill(px - thickness / 2, py - thickness / 2,
                    px + (thickness + 1) / 2, py + (thickness + 1) / 2, color);
        }
    }

    /** Letter-spaced text centered on x. Alpha is baked into the color. */
    private static void drawTrackedCentered(DrawContext ctx, TextRenderer tr, String text,
                                            int centerX, int y, int color, float tracking, float scale) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += tr.getWidth(String.valueOf(text.charAt(i)));
            if (i < text.length() - 1) {
                width += Math.round(tracking);
            }
        }
        float drawWidth = width * scale;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) (centerX - drawWidth / 2.0F), (float) y);
        ctx.getMatrices().scale(scale, scale);
        int cursor = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            String s = String.valueOf(ch);
            Texts.draw(ctx, tr, s, cursor, 0, color, false);
            cursor += tr.getWidth(s) + Math.round(tracking);
        }
        ctx.getMatrices().popMatrix();
    }

    /** Multiply a color's alpha channel by a 0..1 factor. */
    private static int withAlpha(int color, float factor) {
        int a = (int) (((color >>> 24) & 0xFF) * clamp01(factor));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static float clamp01(float v) {
        return v < 0F ? 0F : (v > 1F ? 1F : v);
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    /** Overshoot-enabled pop-in curve. */
    private static float easeOutBack(float x) {
        float f = clamp01(x) - 1F;
        float c1 = 1.70158F;
        float c3 = c1 + 1F;
        return 1F + c3 * f * f * f + c1 * f * f;
    }

    /** Reset all animation state (called once the fade-out to the menu completes). */
    private static void reset() {
        shown = 0F;
        ceiling = 0F;
        firstFrameAt = -1L;
        doneAt = -1L;
    }
}
