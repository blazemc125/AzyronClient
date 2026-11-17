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
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import com.jiggneshgaming.azyron.menu.AzyronMainMenuScreen;

/**
 * AZYRON CLIENT — HUD editor.
 *
 * <p>Full-screen layout screen: drag any HUD element to reposition it.
 * Positions are written to the config immediately on mouse release.</p>
 */
public final class HudEditorScreen extends Screen {

    private static final int BUTTON_DONE = 0;
    private static final int BUTTON_RESET = 1;

    private String dragging;
    private int dragOffsetX;
    private int dragOffsetY;
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

        TextRenderer tr = ClientRefs.textRenderer(MinecraftClient.getInstance());
        if (tr == null) {
            return; // nothing sensible to draw without a font
        }
        // Title bar
        RenderUtil.fillHorizontalGradient(ctx, this.width / 2 - 130, 12, 260, 2,
                AzyronConstants.COLOR_PURPLE, AzyronConstants.COLOR_ELECTRIC);
        Texts.drawCentered(ctx, tr, "HUD EDITOR", this.width / 2, 20, AzyronConstants.COLOR_TEXT, true);
        Texts.drawCentered(ctx, tr, "Drag any element to reposition it — ESC to finish",
                this.width / 2, 33, AzyronConstants.COLOR_TEXT_DIM, false);

        // Live previews of every element.
        hovered = null;
        for (String id : HudRenderer.ALL_IDS) {
            AzyronConfigModel.HudPosition pos = AzyronConfig.get().positionOf(id);
            int[] size = HudRenderer.measure(id, true);
            HudRenderer.renderElement(ctx, id, true);
            int[] clamped = com.jiggneshgaming.azyron.core.hud.HudGeometry.clampToScreen(
                    pos.x, pos.y, size[0], size[1], this.width, this.height);
            int ex = clamped[0];
            int ey = clamped[1];
            boolean over = mouseX >= ex && mouseX < ex + size[0] && mouseY >= ey && mouseY < ey + size[1];
            if (over) {
                hovered = id;
            }
            if (over || id.equals(dragging)) {
                RenderUtil.drawRoundedBorder(ctx, ex - 2, ey - 2, size[0] + 4, size[1] + 4, 4,
                        AzyronConstants.COLOR_ELECTRIC);
            }
        }

        // Footer buttons
        int btnW = 110;
        int y = this.height - 26;
        int resetX = this.width / 2 - btnW - 8;
        int doneX = this.width / 2 + 8;
        drawFooterButton(ctx, tr, resetX, y, btnW, 18, "Reset layout", BUTTON_RESET, mouseX, mouseY);
        drawFooterButton(ctx, tr, doneX, y, btnW, 18, "Done", BUTTON_DONE, mouseX, mouseY);
        Texts.drawCentered(ctx, tr, "Elements keep their position between sessions",
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
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
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
            // Hit-test elements in reverse draw order (top-most last wins).
            for (int i = HudRenderer.ALL_IDS.length - 1; i >= 0; i--) {
                String id = HudRenderer.ALL_IDS[i];
                AzyronConfigModel.HudPosition pos = AzyronConfig.get().positionOf(id);
                int[] size = HudRenderer.measure(id, true);
                int[] clamped = com.jiggneshgaming.azyron.core.hud.HudGeometry.clampToScreen(
                        pos.x, pos.y, size[0], size[1], this.width, this.height);
                if (mouseX >= clamped[0] && mouseX < clamped[0] + size[0]
                        && mouseY >= clamped[1] && mouseY < clamped[1] + size[1]) {
                    dragging = id;
                    dragOffsetX = (int) mouseX - clamped[0];
                    dragOffsetY = (int) mouseY - clamped[1];
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (dragging != null && button == 0) {
            AzyronConfigModel.HudPosition pos = AzyronConfig.get().positionOf(dragging);
            pos.x = (int) mouseX - dragOffsetX;
            pos.y = (int) mouseY - dragOffsetY;
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (dragging != null && button == 0) {
            dragging = null;
            AzyronConfig.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            closeToSafeScreen();
            return true;
        }
        return super.keyPressed(input);
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
