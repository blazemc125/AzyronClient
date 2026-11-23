package com.jiggneshgaming.azyron.menu;

import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.config.AzyronConfig;
import com.jiggneshgaming.azyron.core.profile.PerformanceProfile;
import com.jiggneshgaming.azyron.performance.ProfileManager;
import com.jiggneshgaming.azyron.util.Links;
import com.jiggneshgaming.azyron.util.RenderUtil;
import com.jiggneshgaming.azyron.util.ScreenNav;
import com.jiggneshgaming.azyron.util.Texts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.option.GameOptions;

/**
 * AZYRON CLIENT — premium main menu.
 *
 * <p>Replaces the vanilla title screen (see the ScreenEvents hook in the
 * client initializer) with a dark, purple/electric-blue branded experience:
 * AZYRON CLIENT logo, Singleplayer / Multiplayer / Cosmetics / Mods /
 * Settings / Quit, Discord section and a live performance-profile chip.</p>
 */
public final class AzyronMainMenuScreen extends BaseAzyronScreen {

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("azyronclient/mainmenu");

    private AzyronButton profileChip;

    public AzyronMainMenuScreen() {
        super(AzyronConstants.NAME, null);
    }

    @Override
    protected void buildButtons() {
        // The complete vanilla main menu, plus the AZYRON entries.
        // Singleplayer / Multiplayer / Options / Language / Quit match the
        // vanilla title screen 1:1; Mods / Cosmetics / Settings are the
        // AZYRON additions. Layout tightens itself on small screens.
        int buttonWidth = 216;
        int buttonHeight = 24;
        int gap = 8;
        int pitch = buttonHeight + gap;
        int count = 8;
        int y = (int) (this.height * 0.24D);
        y = Math.min(y, this.height - count * pitch - 18); // never collide with the footer
        y = Math.max(64, y);
        int x = Math.max(28, (int) (this.width * 0.065D));

        addButton("Singleplayer", x, y, buttonWidth, buttonHeight,
                () -> ScreenNav.openSingleplayer(MinecraftClient.getInstance(), this));
        addButton("Multiplayer", x, y + pitch, buttonWidth, buttonHeight,
                () -> ScreenNav.openMultiplayer(MinecraftClient.getInstance(), this));
        addButton("Options", x, y + pitch * 2, buttonWidth, buttonHeight,
                () -> openOptions());
        addButton("Language", x, y + pitch * 3, buttonWidth, buttonHeight,
                () -> openLanguage());
        addButton("Mods", x, y + pitch * 4, buttonWidth, buttonHeight,
                () -> MinecraftClient.getInstance().setScreen(new AzyronModsMenuScreen(this)));
        addButton("Cosmetics", x, y + pitch * 5, buttonWidth, buttonHeight,
                () -> MinecraftClient.getInstance().setScreen(new AzyronCosmeticsScreen(this)));
        addButton("Settings", x, y + pitch * 6, buttonWidth, buttonHeight,
                () -> MinecraftClient.getInstance().setScreen(new AzyronSettingsScreen(this)));
        addButton("Quit Game", x, y + pitch * 7, buttonWidth, buttonHeight,
                AzyronMainMenuScreen::quitGame);

        // Profile chip — click to cycle POTATO → BALANCED → SMOOTH.
        AzyronButton chip = addButton(AzyronConfig.get().profile, this.width - 200, 14, 186, 30,
                () -> {
                    cycleProfile();
                    if (profileChip != null) {
                        profileChip.label = AzyronConfig.get().profile;
                    }
                });
        chip.leftHint = "PROFILE";
        this.profileChip = chip;

        // Discord invite button.
        int discordW = 216;
        addButton("Join Discord", this.width - discordW - 30, this.height - 92,
                discordW, 26, () -> Links.open(AzyronConstants.DISCORD_INVITE));
    }

    /** Vanilla Options screen — the same one the vanilla title screen opens. */
    private void openOptions() {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            GameOptions options = client.options;
            if (options == null) {
                LOGGER.error("Game options not ready — cannot open the Options screen yet.");
                return;
            }
            client.setScreen(new OptionsScreen(this, options));
        } catch (Throwable t) {
            LOGGER.error("Could not open the Options screen", t);
        }
    }

    /** Vanilla Language screen — needs the client's LanguageManager (private field). */
    private void openLanguage() {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            LanguageManager manager = client.languageManager; // open via the access widener
            if (manager == null) {
                LOGGER.error("LanguageManager unavailable — cannot open the Language screen.");
                return;
            }
            GameOptions options = client.options;
            client.setScreen(new LanguageOptionsScreen(this, options, manager));
        } catch (Throwable t) {
            LOGGER.error("Could not open the Language screen", t);
        }
    }

    private static void cycleProfile() {
        PerformanceProfile.Id current = ProfileManager.activeProfile();
        PerformanceProfile.Id next = current.next();
        ProfileManager.apply(next);
    }

    private static void quitGame() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            client.scheduleStop();
        } catch (Throwable t) {
            try {
                MinecraftClient.class.getMethod("scheduleStop").invoke(MinecraftClient.getInstance());
            } catch (Throwable ignored) {
                // Last resort — never hard-exit the JVM from a mod.
            }
        }
    }

    @Override
    protected void renderContent(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY, float delta) {
        /* ------------------------------ logo ----------------------- */
        int logoX = Math.max(30, (int) (this.width * 0.065D));
        int logoY = Math.max(8, (int) (this.height * 0.06D));

        // Crest rings behind the logo text block, left side.
        drawCrest(ctx, tr, (int) (this.width * 0.72D), (int) (this.height * 0.34D),
                Math.min(120, this.height / 5));

        drawScaled(ctx, tr, logoX, logoY, 3.4F,
                "AZYRON", AzyronConstants.COLOR_PURPLE_BRIGHT, AzyronConstants.COLOR_ELECTRIC);
        drawScaled(ctx, tr, logoX, logoY + 34, 1.15F,
                "C L I E N T", 0xFFE2E8F0, 0xFF94A3B8);
        Texts.draw(ctx, tr, AzyronConstants.TAGLINE, logoX, logoY + 56, AzyronConstants.COLOR_ELECTRIC, true);

        // Version pill under tagline.
        String pill = "AZYRON CLIENT V2  •  v" + AzyronConstants.VERSION + "  •  Minecraft " + AzyronConstants.MINECRAFT_VERSION;
        int pillWidth = tr.getWidth(pill) + 18;
        RenderUtil.fillRoundedRect(ctx, logoX, logoY + 70, pillWidth, 15, 7, 0x5533CCFF);
        RenderUtil.drawRoundedBorder(ctx, logoX, logoY + 70, pillWidth, 15, 7, 0x9933CCFF);
        Texts.draw(ctx, tr, pill, logoX + 9, logoY + 73, AzyronConstants.COLOR_ELECTRIC, false);

        /* ------------------------- profile chip -------------------- */
        String profile = AzyronConfig.get().profile;
        String summary = profileBlurb(profile);
        int summaryX = this.width - 200;
        int summaryY = 52;
        wrapText(ctx, tr, summary, summaryX, summaryY, 190, 10, AzyronConstants.COLOR_TEXT_DIM, 3);

        /* ------------------------- discord panel -------------------- */
        int panelW = 260;
        int panelH = 96;
        int panelX = this.width - panelW - 30;
        int panelY = this.height - 196;
        RenderUtil.fillMenuPanel(ctx, panelX, panelY, panelW, panelH, 8);
        drawDiscordGlyph(ctx, panelX + 26, panelY + 30, 17);
        Texts.draw(ctx, tr, "Join the Discord!", panelX + 56, panelY + 16, AzyronConstants.COLOR_TEXT, true);
        wrapText(ctx, tr, "News, updates, modpack support and JIGNESH GAMING community events.",
                panelX + 56, panelY + 30, panelW - 66, 10, AzyronConstants.COLOR_TEXT_DIM, 3);
        Texts.draw(ctx, tr, AzyronConstants.DISCORD_LABEL, panelX + 10, panelY + panelH - 13,
                AzyronConstants.COLOR_ELECTRIC, true);

        /* --------------------- tech spec strip --------------------- */
        int stripY = this.height - 34;
        String specs = "Sodium • Lithium • FerriteCore • EntityCulling • DynamicFPS • 1.21.11";
        Texts.draw(ctx, tr, specs, Math.max(28, (int) (this.width * 0.065D)), stripY, 0xFF4B5563, false);
    }

    /* ---------------------------------------------------------- */

    private static String profileBlurb(String profile) {
        PerformanceProfile.Id id = PerformanceProfile.Id.fromString(profile, PerformanceProfile.Id.BALANCED);
        return PerformanceProfile.forId(id).blurb;
    }

    /** Scale-rendered gradient logo text using the model matrix. */
    private void drawScaled(DrawContext ctx, TextRenderer tr, int x, int y, float scale,
                            String text, int fromColor, int toColor) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) x, (float) y);
        ctx.getMatrices().scale(scale, scale);
        RenderUtil.drawGradientText(ctx, tr, text, 0, 0, fromColor, toColor, true);
        ctx.getMatrices().popMatrix();
    }

    /** Concentric-ring crest with a gradient "A". */
    private void drawCrest(DrawContext ctx, TextRenderer tr, int cx, int cy, int radius) {
        // Outer rings
        drawRing(ctx, cx, cy, radius, 2, 0x557C3AED);
        drawRing(ctx, cx, cy, radius - 8, 1, 0x5533CCFF);
        // Radial spokes (every 30°)
        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30);
            int inner = radius / 2;
            int x1 = cx + (int) (Math.cos(angle) * inner);
            int y1 = cy + (int) (Math.sin(angle) * inner);
            int x2 = cx + (int) (Math.cos(angle) * (radius - 10));
            int y2 = cy + (int) (Math.sin(angle) * (radius - 10));
            drawLine(ctx, x1, y1, x2, y2, 0x2E33CCFF);
        }
        // Gradient A glyph
        String a = "A";
        int glyphWidth = tr.getWidth(a);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) (cx - glyphWidth * 2.6D), (float) (cy - 18.0D));
        ctx.getMatrices().scale(5.2F, 5.2F);
        RenderUtil.drawGradientText(ctx, tr, a, 0, 0,
                AzyronConstants.COLOR_PURPLE_BRIGHT, AzyronConstants.COLOR_ELECTRIC, true);
        ctx.getMatrices().popMatrix();
    }

    private static void drawRing(DrawContext ctx, int cx, int cy, int radius, int thickness, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            double dyCenter = dy - 0.5D;
            double span = Math.sqrt(Math.max(0.0D, radius * (double) radius - dyCenter * dyCenter));
            int outer = (int) Math.floor(span);
            double innerSpan = Math.sqrt(Math.max(0.0D,
                    (radius - thickness) * (double) (radius - thickness) - dyCenter * dyCenter));
            int inner = (int) Math.floor(innerSpan);
            ctx.fill(cx - outer, cy + dy, cx - inner + 1, cy + dy + 1, color);
            ctx.fill(cx + inner, cy + dy, cx + outer + 1, cy + dy + 1, color);
        }
    }

    private static void drawLine(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        // Bresenham line — used for the crest spokes.
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        int x = x1;
        int y = y1;
        while (true) {
            ctx.fill(x, y, x + 1, y + 1, color);
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

    /** Word-wrapped dim text. */
    private static void wrapText(DrawContext ctx, TextRenderer tr, String text, int x, int y,
                                 int maxWidth, int lineHeight, int color, int maxLines) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int lineIndex = 0;
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (tr.getWidth(candidate) > maxWidth && line.length() > 0) {
                Texts.draw(ctx, tr, line.toString(), x, y + lineIndex * lineHeight, color, false);
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
            Texts.draw(ctx, tr, line.toString(), x, y + lineIndex * lineHeight, color, false);
        }
    }

    /** Discord "game controller" glyph — stylized, drawn with fills. */
    private static void drawDiscordGlyph(DrawContext ctx, int cx, int cy, int size) {
        int purple = 0xFF5865F2;
        int white = 0xFFFFFFFF;
        // Purple rounded backdrop circle (stepped).
        for (int dy = -size; dy <= size; dy++) {
            double dyC = dy - 0.5D;
            int half = (int) Math.floor(Math.sqrt(Math.max(0.0D, size * (double) size - dyC * dyC)));
            ctx.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, purple);
        }
        // Controller body — white capsule.
        int capsuleW = size + 4;
        int capsuleH = size - 3;
        RenderUtil.fillRoundedRect(ctx, cx - capsuleW / 2, cy - capsuleH / 2 + 1, capsuleW, capsuleH,
                capsuleH / 2, white);
        // Eyes (purple dots inside the capsule).
        int eyeOffset = capsuleW / 4;
        ctx.fill(cx - eyeOffset - 1, cy - 2, cx - eyeOffset + 2, cy + 2, purple);
        ctx.fill(cx + eyeOffset - 1, cy - 2, cx + eyeOffset + 2, cy + 2, purple);
        // Mouth notch.
        ctx.fill(cx - 2, cy + capsuleH / 2 - 2, cx + 3, cy + capsuleH / 2 + 1, purple);
    }
}
