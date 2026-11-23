package com.jiggneshgaming.azyron.quicksettings;

import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.fps.AzyronFpsScreen;
import com.jiggneshgaming.azyron.hud.HudEditorScreen;
import com.jiggneshgaming.azyron.menu.AzyronButton;
import com.jiggneshgaming.azyron.menu.AzyronSettingsScreen;
import com.jiggneshgaming.azyron.menu.BaseAzyronScreen;
import com.jiggneshgaming.azyron.util.RenderUtil;
import com.jiggneshgaming.azyron.util.Texts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.option.GameOptions;

/**
 * AZYRON CLIENT V2 — Quick Settings hub.
 *
 * <p>Six large tiles, one per settings destination. Every tile opens the
 * real underlying screen (vanilla option screens where they exist, AZYRON
 * screens elsewhere) — nothing here is a dead button.</p>
 */
public final class AzyronQuickSettingsScreen extends BaseAzyronScreen {

    private String feedback = "";
    private long feedbackAt;

    public AzyronQuickSettingsScreen(Screen backTarget) {
        super("Quick Settings", backTarget);
    }

    private static GameOptions options() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client == null ? null : client.options;
    }

    private void openGraphics() {
        // Vanilla has no separate graphics-preset screen on this version —
        // the AZYRON FPS Booster page is the real graphics surface.
        MinecraftClient.getInstance().setScreen(new AzyronFpsScreen(this));
    }

    private void openVideo() {
        try {
            GameOptions o = options();
            if (o != null) {
                MinecraftClient.getInstance().setScreen(
                        new VideoOptionsScreen(this, MinecraftClient.getInstance(), o));
                return;
            }
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger("azyronclient/quick").warn("Video settings unavailable: {}", t.toString());
        }
        note("Video settings unavailable on this device.");
    }

    private void openControls() {
        try {
            GameOptions o = options();
            if (o != null) {
                MinecraftClient.getInstance().setScreen(new KeybindsScreen(this, o));
                return;
            }
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger("azyronclient/quick").warn("Controls unavailable: {}", t.toString());
        }
        note("Controls unavailable on this device.");
    }

    private void note(String message) {
        this.feedback = message;
        this.feedbackAt = System.currentTimeMillis();
    }

    @Override
    protected void buildButtons() {
        int x = 12;
        int y = this.height - 32;
        addButton("Back", x, y, 92, 22, this::goBack);
    }

    private static final class Tile {
        final String title;
        final String subtitle;
        final int accent;
        final Runnable action;

        Tile(String title, String subtitle, int accent, Runnable action) {
            this.title = title;
            this.subtitle = subtitle;
            this.accent = accent;
            this.action = action;
        }
    }

    private java.util.List<Tile> tiles() {
        java.util.List<Tile> list = new java.util.ArrayList<Tile>();
        MinecraftClient client = MinecraftClient.getInstance();
        list.add(new Tile("GRAPHICS", "Render & simulation distance, particles, entity scale", AzyronConstants.COLOR_PURPLE_BRIGHT, this::openGraphics));
        list.add(new Tile("VIDEO", "Brightness, fullscreen, FOV, bobbing", AzyronConstants.COLOR_ELECTRIC, this::openVideo));
        list.add(new Tile("CONTROLS", "Key bindings — including AZYRON keys", AzyronConstants.COLOR_PURPLE, this::openControls));
        list.add(new Tile("HUD SETTINGS", "Drag & toggle elements on the editor", AzyronConstants.COLOR_ELECTRIC_DEEP,
                () -> client.setScreen(new HudEditorScreen())));
        list.add(new Tile("FPS SETTINGS", "Booster page — profiles, distances, particles", AzyronConstants.COLOR_GOOD,
                () -> client.setScreen(new AzyronFpsScreen(this))));
        list.add(new Tile("CLIENT SETTINGS", "Zoom, crosshair, animations, updates", AzyronConstants.COLOR_WARN,
                () -> client.setScreen(new AzyronSettingsScreen(this))));
        return list;
    }

    @Override
    protected void renderContent(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY, float delta) {
        int panelW = Math.min(560, this.width - 20);
        int panelH = this.height - 88;
        int panelX = Math.max(10, (this.width - panelW) / 2);
        int panelY = 12;
        RenderUtil.fillMenuPanel(ctx, panelX, panelY, panelW, panelH, 8);

        Texts.draw(ctx, tr, "QUICK SETTINGS", panelX + 14, panelY + 9, AzyronConstants.COLOR_TEXT, true);
        Texts.draw(ctx, tr, "Every destination, one screen — no digging through menus",
                panelX + 14, panelY + 21, AzyronConstants.COLOR_TEXT_DIM, false);

        java.util.List<Tile> tiles = tiles();
        int cols = 2;
        int tileW = (panelW - 28 - 12) / cols;
        int tileH = 64;
        int startX = panelX + 14;
        int startY = panelY + 36;

        for (int i = 0; i < tiles.size(); i++) {
            Tile tile = tiles.get(i);
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (tileW + 12);
            int y = startY + row * (tileH + 12);
            boolean hover = mouseX >= x && mouseX < x + tileW && mouseY >= y && mouseY < y + tileH;
            RenderUtil.fillRoundedRect(ctx, x, y, tileW, tileH, 6, hover ? 0xE02A1F4D : 0xC01A1334);
            RenderUtil.drawRoundedBorder(ctx, x, y, tileW, tileH, 6, hover ? tile.accent : AzyronConstants.COLOR_PANEL_BORDER);
            ctx.fill(x + 8, y, x + 10, y + tileH, tile.accent);
            Texts.draw(ctx, tr, tile.title, x + 18, y + 10, AzyronConstants.COLOR_TEXT, true);
            wrapText(ctx, tr, tile.subtitle, x + 18, y + 24, tileW - 28, 10,
                    AzyronConstants.COLOR_TEXT_DIM, 3);
            Texts.draw(ctx, tr, "›", x + tileW - 14, y + tileH / 2 - 5,
                    hover ? tile.accent : 0xFF4B3B76, true);
        }

        if (!feedback.isEmpty() && System.currentTimeMillis() - feedbackAt < 4500L) {
            Texts.draw(ctx, tr, feedback, panelX + 14, panelY + panelH - 18, AzyronConstants.COLOR_WARN, true);
        }
    }

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

    /* ------------------------- click handling ------------------------ */

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 0) {
            int panelW = Math.min(560, this.width - 20);
            int panelX = Math.max(10, (this.width - panelW) / 2);
            int panelY = 12;
            int cols = 2;
            int tileW = (panelW - 28 - 12) / cols;
            int tileH = 64;
            int startX = panelX + 14;
            int startY = panelY + 36;
            java.util.List<Tile> tiles = tiles();
            for (int i = 0; i < tiles.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                int x = startX + col * (tileW + 12);
                int y = startY + row * (tileH + 12);
                if (mouseX >= x && mouseX < x + tileW && mouseY >= y && mouseY < y + tileH) {
                    try {
                        tiles.get(i).action.run();
                    } catch (Throwable t) {
                        note("Could not open that screen: " + t.getClass().getSimpleName());
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }
}
