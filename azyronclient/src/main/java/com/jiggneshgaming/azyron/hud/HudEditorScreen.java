package com.jiggneshgaming.azyron.hud;

import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.config.AzyronConfig;
import com.jiggneshgaming.azyron.core.config.AzyronConfigModel;
import com.jiggneshgaming.azyron.util.ClientRefs;
import com.jiggneshgaming.azyron.util.RenderUtil;
import com.jiggneshgaming.azyron.util.Texts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import com.jiggneshgaming.azyron.compat.AzyronScreen;
import com.jiggneshgaming.azyron.menu.AzyronMainMenuScreen;

/**
 * AZYRON CLIENT — HUD editor.
 *
 * <p>Full-screen layout screen: drag any HUD element to reposition it.
 * Positions are written to the config immediately on mouse release.</p>
 */
public final class HudEditorScreen extends AzyronScreen {

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("azyronclient/hud-editor");

    private static final int BUTTON_DONE = 0;
    private static final int BUTTON_RESET = 1;

    private static final String[] MODULE_LABELS = {
            "FPS", "CPS", "Ping", "Coordinates", "Keystrokes", "Armor", "Potions",
            "Sprint", "Zoom", "Direction", "Clock", "Biome", "Player Status"
    };

    private static final int[] HUD_ACCENTS = {
            0xFF33CCFF, 0xFF7C3AED, 0xFF4ADE80, 0xFFFACC15, 0xFFF87171, 0xFFFFFFFF
    };

    private static int indexOf(String[] array, String target) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(target)) {
                return i;
            }
        }
        return 0;
    }

    private String dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private String resizing;
    private String hovered;

    public HudEditorScreen() {
        super(Text.literal("AZYRON HUD Editor"));
    }

    @Override
    protected void init() {
        // Layout is custom — everything is drawn and hit-tested manually.
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Dim the world / background (menu-style backdrop).
        ctx.fill(0, 0, this.width, this.height, 0xE60B0714);

        // Screen's own font field — zero reflection, always available.
        TextRenderer tr = this.textRenderer != null
                ? this.textRenderer
                : ClientRefs.textRenderer(MinecraftClient.getInstance());
        if (tr == null) {
            return; // nothing sensible to draw without a font
        }
        // Title bar
        RenderUtil.fillHorizontalGradient(ctx, this.width / 2 - 130, 12, 260, 2,
                AzyronConstants.COLOR_PURPLE, AzyronConstants.COLOR_ELECTRIC);
        Texts.drawCentered(ctx, tr, "HUD EDITOR", this.width / 2, 20, AzyronConstants.COLOR_TEXT, true);
        Texts.drawCentered(ctx, tr, "Drag to move  •  drag corner square to resize  •  right panel toggles modules  •  swatches recolor  •  ESC to finish",
                this.width / 2, 33, AzyronConstants.COLOR_TEXT_DIM, false);

        // Live previews of every element.
        hovered = null;
        for (String id : HudRenderer.ALL_IDS) {
            try {
                AzyronConfigModel.HudPosition pos = AzyronConfig.get().positionOf(id);
                int[] size = HudRenderer.measureScaled(id, true);
                HudRenderer.renderElement(ctx, id, true);
                int[] clamped = com.jiggneshgaming.azyron.core.hud.HudGeometry.clampToScreen(
                        pos.x, pos.y, size[0], size[1], this.width, this.height);
            int ex = clamped[0];
            int ey = clamped[1];
            boolean over = mouseX >= ex && mouseX < ex + size[0] && mouseY >= ey && mouseY < ey + size[1];
            if (over) {
                hovered = id;
            }
            if (over || id.equals(dragging) || id.equals(resizing)) {
                RenderUtil.drawRoundedBorder(ctx, ex - 2, ey - 2, size[0] + 4, size[1] + 4, 4,
                        AzyronConstants.COLOR_ELECTRIC);
                // Resize handle — bottom-right corner square.
                int hs = 7;
                int hx = ex + size[0] - hs / 2;
                int hy = ey + size[1] - hs / 2;
                ctx.fill(hx, hy, hx + hs, hy + hs, AzyronConstants.COLOR_ELECTRIC);
                RenderUtil.drawRoundedBorder(ctx, hx, hy, hs, hs, 2, 0xFF0B0714);
            }
            } catch (Throwable t) {
                // A broken preview must not take down the whole editor.
                LOGGER.debug("HUD preview failed for '{}': {}", id, t.toString());
            }
        }

        // ---- Right panel: per-module visibility toggles ----
        int panelX = this.width - 158;
        int panelY = 60;
        int panelW = 146;
        int panelH = 34 + HudRenderer.ALL_IDS.length * 17 + 8;
        RenderUtil.fillMenuPanel(ctx, panelX, panelY, panelW, panelH, 6);
        Texts.draw(ctx, tr, "HUD MODULES", panelX + 10, panelY + 8, AzyronConstants.COLOR_PURPLE_BRIGHT, true);
        int rowY = panelY + 26;
        for (String id : HudRenderer.ALL_IDS) {
            boolean visible = AzyronConfig.get().isHudVisible(id);
            boolean hover = mouseX >= panelX + 6 && mouseX < panelX + panelW - 6
                    && mouseY >= rowY && mouseY < rowY + 15;
            // checkbox
            ctx.fill(panelX + 8, rowY + 2, panelX + 19, rowY + 13, hover ? 0xFF241A3E : 0xFF1B1332);
            RenderUtil.drawRoundedBorder(ctx, panelX + 8, rowY + 2, 11, 11, 2,
                    visible ? AzyronConstants.COLOR_ELECTRIC : AzyronConstants.COLOR_PANEL_BORDER);
            if (visible) {
                Texts.draw(ctx, tr, "x", panelX + 10, rowY + 3, AzyronConstants.COLOR_ELECTRIC, true);
            }
            Texts.draw(ctx, tr, MODULE_LABELS[indexOf(HudRenderer.ALL_IDS, id)],
                    panelX + 24, rowY + 4,
                    visible ? AzyronConstants.COLOR_TEXT : AzyronConstants.COLOR_TEXT_DIM, false);
            rowY += 17;
        }

        // ---- Bottom-left: HUD accent swatches ----
        int swatchY = this.height - 66;
        int swatchX = 12;
        Texts.draw(ctx, tr, "HUD ACCENT", swatchX, swatchY - 12, AzyronConstants.COLOR_TEXT_DIM, true);
        int currentAccent = AzyronConfig.get().hudAccent & 0x00FFFFFF;
        for (int i = 0; i < HUD_ACCENTS.length; i++) {
            int sx = swatchX + i * 22;
            boolean sel = (HUD_ACCENTS[i] & 0x00FFFFFF) == currentAccent;
            ctx.fill(sx, swatchY, sx + 17, swatchY + 17, HUD_ACCENTS[i]);
            RenderUtil.drawRoundedBorder(ctx, sx, swatchY, 17, 17, 3,
                    sel ? AzyronConstants.COLOR_TEXT : AzyronConstants.COLOR_PANEL_BORDER);
        }

        // Footer buttons
        int btnW = 110;
        int y = this.height - 26;
        int resetX = this.width / 2 - btnW - 8;
        int doneX = this.width / 2 + 8;
        drawFooterButton(ctx, tr, resetX, y, btnW, 18, "Reset layout", BUTTON_RESET, mouseX, mouseY);
        drawFooterButton(ctx, tr, doneX, y, btnW, 18, "Done", BUTTON_DONE, mouseX, mouseY);
        Texts.drawCentered(ctx, tr, "Live preview — elements update instantly  •  layout is saved automatically",
                this.width / 2, this.height - 40, 0xFF6B7280, false);
    }

    private void drawFooterButton(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h,
                                  String label, int id, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        int fill = hover ? 0xFF2A1F4D : 0xFF1B1332;
        int border = hover ? AzyronConstants.COLOR_ELECTRIC : AzyronConstants.COLOR_PANEL_BORDER;
        RenderUtil.fillRoundedRect(ctx, x, y, w, h, 5, fill);
        RenderUtil.drawRoundedBorder(ctx, x, y, w, h, 5, border);
        Texts.drawCentered(ctx, tr, label, x + w / 2, y + (h - 9) / 2 + 1, AzyronConstants.COLOR_TEXT, true);
    }

    @Override
    protected boolean azyronClick(double mouseX, double mouseY, int button) {
        int footerY = this.height - 26;
        int btnW = 110;
        int resetX = this.width / 2 - btnW - 8;
        int doneX = this.width / 2 + 8;
        if (mouseY >= footerY && mouseY < footerY + 18) {
            if (mouseX >= resetX && mouseX < resetX + btnW) {
                AzyronConfig.get().resetPositions();
                AzyronConfig.save();
                return true;
            }
            if (mouseX >= doneX && mouseX < doneX + btnW) {
                closeToSafeScreen();
                return true;
            }
        }
        if (button == 0) {
            // ---- HUD module toggle panel (right side) ----
            int panelX = this.width - 158;
            int panelY = 60;
            int panelW = 146;
            int panelH = 34 + HudRenderer.ALL_IDS.length * 17 + 8;
            if (mouseX >= panelX && mouseX < panelX + panelW && mouseY >= panelY && mouseY < panelY + panelH) {
                int rowY = panelY + 26;
                for (String id : HudRenderer.ALL_IDS) {
                    if (mouseY >= rowY && mouseY < rowY + 17) {
                        AzyronConfigModel config = AzyronConfig.get();
                        config.setHudVisible(id, !config.isHudVisible(id));
                        AzyronConfig.save();
                        return true;
                    }
                    rowY += 17;
                }
                return true; // swallow clicks anywhere inside the panel chrome
            }
            // ---- HUD accent swatches (bottom-left) ----
            int swatchY = this.height - 66;
            int swatchX = 12;
            if (mouseY >= swatchY && mouseY < swatchY + 17) {
                for (int i = 0; i < HUD_ACCENTS.length; i++) {
                    int sx = swatchX + i * 22;
                    if (mouseX >= sx && mouseX < sx + 22) {
                        AzyronConfig.get().hudAccent = HUD_ACCENTS[i];
                        AzyronConfig.save();
                        return true;
                    }
                }
            }
            // Hit-test elements in reverse draw order (top-most last wins).
            for (int i = HudRenderer.ALL_IDS.length - 1; i >= 0; i--) {
                String id = HudRenderer.ALL_IDS[i];
                AzyronConfigModel.HudPosition pos = AzyronConfig.get().positionOf(id);
                int[] size = HudRenderer.measureScaled(id, true);
                int[] clamped = com.jiggneshgaming.azyron.core.hud.HudGeometry.clampToScreen(
                        pos.x, pos.y, size[0], size[1], this.width, this.height);
                // Resize handle: within 9px of the bottom-right corner.
                int hx = clamped[0] + size[0];
                int hy = clamped[1] + size[1];
                if (Math.abs(mouseX - hx) <= 9 && Math.abs(mouseY - hy) <= 9) {
                    resizing = id;
                    return true;
                }
                if (mouseX >= clamped[0] && mouseX < clamped[0] + size[0]
                        && mouseY >= clamped[1] && mouseY < clamped[1] + size[1]) {
                    dragging = id;
                    dragOffsetX = (int) mouseX - clamped[0];
                    dragOffsetY = (int) mouseY - clamped[1];
                    return true;
                }
            }
        }
        return super.azyronClick(mouseX, mouseY, button);
    }

    @Override
    protected boolean azyronDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (resizing != null && button == 0) {
            AzyronConfigModel config = AzyronConfig.get();
            AzyronConfigModel.HudPosition pos = config.positionOf(resizing);
            int[] base = HudRenderer.measure(resizing, true);
            double hudScale = Math.max(0.01D, config.hudScale);
            // sx/sy recover the desired *effective* scale (visual size / base size).
            double sx = (mouseX - pos.x) / Math.max(1, base[0]);
            double sy = (mouseY - pos.y) / Math.max(1, base[1]);
            double desiredEffective = (sx + sy) / 2.0D;
            // Store per-element scale on top of the global hudScale; clamped like validate() does.
            pos.scale = Math.max(0.6D, Math.min(2.0D, desiredEffective / hudScale));
            return true;
        }
        if (dragging != null && button == 0) {
            AzyronConfigModel.HudPosition pos = AzyronConfig.get().positionOf(dragging);
            pos.x = (int) mouseX - dragOffsetX;
            pos.y = (int) mouseY - dragOffsetY;
            return true;
        }
        return super.azyronDrag(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    protected boolean azyronRelease(double mouseX, double mouseY, int button) {
        if (resizing != null && button == 0) {
            AzyronConfigModel.HudPosition pos = AzyronConfig.get().positionOf(resizing);
            int[] size = HudRenderer.measureScaled(resizing, true);
            int[] clamped = com.jiggneshgaming.azyron.core.hud.HudGeometry.clampToScreen(
                    pos.x, pos.y, size[0], size[1], this.width, this.height);
            pos.x = clamped[0];
            pos.y = clamped[1];
            resizing = null;
            AzyronConfig.save();
            return true;
        }
        if (dragging != null && button == 0) {
            // Clamp the element fully inside the screen — off-screen HUDs are a bug.
            AzyronConfigModel.HudPosition pos = AzyronConfig.get().positionOf(dragging);
            int[] size = HudRenderer.measureScaled(dragging, true);
            int[] clamped = com.jiggneshgaming.azyron.core.hud.HudGeometry.clampToScreen(
                    pos.x, pos.y, size[0], size[1], this.width, this.height);
            pos.x = clamped[0];
            pos.y = clamped[1];
            dragging = null;
            AzyronConfig.save();
            return true;
        }
        return super.azyronRelease(mouseX, mouseY, button);
    }

    @Override
    protected boolean azyronKey(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            closeToSafeScreen();
            return true;
        }
        return super.azyronKey(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    /** Close the editor — back to the in-game HUD when in a world, otherwise to the AZYRON main menu. */
    private void closeToSafeScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        AzyronConfig.save();
        if (client.world != null) {
            client.setScreen(null);
        } else {
            client.setScreen(new AzyronMainMenuScreen());
        }
    }

    @Override
    public void removed() {
        AzyronConfig.save();
    }
}
