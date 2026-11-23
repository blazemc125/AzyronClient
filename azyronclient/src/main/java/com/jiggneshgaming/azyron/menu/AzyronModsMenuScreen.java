package com.jiggneshgaming.azyron.menu;

import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.modules.AzyronModule;
import com.jiggneshgaming.azyron.modules.Modules;
import com.jiggneshgaming.azyron.util.ClientRefs;
import com.jiggneshgaming.azyron.util.RenderUtil;
import com.jiggneshgaming.azyron.util.Texts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

/**
 * AZYRON CLIENT — the in-game Mods Menu (Right Shift).
 *
 * <p>Sidebar categories, live search, per-module toggle switches and gear
 * shortcuts to settings. Grid rows are custom-rendered for the premium look.</p>
 */
public final class AzyronModsMenuScreen extends BaseAzyronScreen {

    private enum Side {
        ALL(AzyronModule.Category.ALL, "All Modules"),
        PERFORMANCE(AzyronModule.Category.PERFORMANCE, "Performance"),
        HUD(AzyronModule.Category.HUD, "HUD"),
        GAMEPLAY(AzyronModule.Category.GAMEPLAY, "Gameplay"),
        APPEARANCE(AzyronModule.Category.APPEARANCE, "Appearance");

        final AzyronModule.Category category;
        final String label;

        Side(AzyronModule.Category category, String label) {
            this.category = category;
            this.label = label;
        }
    }

    private static final int SIDEBAR_WIDTH = 108;
    private static final int ROW_HEIGHT = 31;
    private static final int SEARCH_HEIGHT = 19;

    private Side activeSide = Side.ALL;
    private final SearchBox search = new SearchBox(0, 0, 0, 0);
    private int scroll;

    public AzyronModsMenuScreen(Screen backTarget) {
        super("AZYRON Mods", backTarget);
    }

    @Override
    protected void buildButtons() {
        int sidebarX = 12;
        int y = 44;
        int buttonH = 20;
        int gap = 6;
        for (Side side : Side.values()) {
            final Side selected = side;
            AzyronButton button = addButton(selected.label, sidebarX, y, SIDEBAR_WIDTH, buttonH,
                    () -> {
                        activeSide = selected;
                        scroll = 0;
                    });
            button.height = buttonH;
            y += buttonH + gap;
        }

        addButton("Installed Mods", sidebarX, this.height - 66, SIDEBAR_WIDTH, 20,
                () -> MinecraftClient.getInstance().setScreen(new com.jiggneshgaming.azyron.mods.AzyronModManagerScreen(this)));
        addButton("Back", sidebarX, this.height - 40, SIDEBAR_WIDTH, 20, this::goBack);
    }

    @Override
    protected void renderContent(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY, float delta) {
        /* ------------------------------- header --------------------- */
        Texts.draw(ctx, tr, "AZYRON MODS", SIDEBAR_WIDTH + 40, 14, AzyronConstants.COLOR_TEXT, true);
        String count = enabledCount() + " / " + Modules.all().size() + " modules enabled";
        Texts.draw(ctx, tr, count, SIDEBAR_WIDTH + 40 + tr.getWidth("AZYRON MODS") + 10,
                16, AzyronConstants.COLOR_ELECTRIC, false);

        /* ------------------------------ search ---------------------- */
        int listX = SIDEBAR_WIDTH + 34;
        int listWidth = this.width - listX - 16;
        search.x = listX;
        search.y = 30;
        search.width = listWidth;
        search.height = SEARCH_HEIGHT;
        search.placeholder = "Search modules...";
        search.render(ctx, tr, mouseX, mouseY);

        /* --------------------------- sidebar ------------------------ */
        RenderUtil.fillMenuPanel(ctx, 12, 40, SIDEBAR_WIDTH, this.height - 96, 6);
        for (AzyronButton button : buttons) {
            boolean active = button.label.equals(activeSide.label);
            if (active) {
                ctx.fill(button.x, button.y, button.x + 3, button.y + button.height,
                        AzyronConstants.COLOR_ELECTRIC);
            }
        }

        /* ----------------------------- list ------------------------- */
        int listTop = search.y + SEARCH_HEIGHT + 6;
        int listBottom = this.height - 16;
        int visibleHeight = listBottom - listTop;
        List<AzyronModule> visible = filteredModules();

        // Clip-free manual scroll: compute content and skip rows above viewport.
        int contentHeight = visible.size() * ROW_HEIGHT;
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        int y = listTop - scroll;
        for (AzyronModule module : visible) {
            if (y >= listTop && y + ROW_HEIGHT - 2 <= listBottom) {
                renderModuleRow(ctx, tr, module, listX, y, listWidth, mouseX, mouseY);
            }
            y += ROW_HEIGHT;
        }

        if (visible.isEmpty()) {
            Texts.drawCentered(ctx, tr, "No modules match \"" + search.text + "\"",
                    listX + listWidth / 2, listTop + 40, AzyronConstants.COLOR_TEXT_DIM, false);
        }

        // Scroll bar
        if (maxScroll > 0) {
            int barX = listX + listWidth - 2;
            int trackH = visibleHeight;
            int thumbH = Math.max(18, trackH * visibleHeight / contentHeight);
            int thumbY = listTop + (trackH - thumbH) * scroll / maxScroll;
            ctx.fill(barX, listTop, barX + 2, listBottom, 0x332A1F4D);
            ctx.fill(barX, thumbY, barX + 2, thumbY + thumbH, AzyronConstants.COLOR_PURPLE);
        }
    }

    private void renderModuleRow(DrawContext ctx, TextRenderer tr, AzyronModule module,
                                 int x, int y, int width, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + ROW_HEIGHT - 2;
        int fill = hover ? 0xD0241838 : 0xB0181028;
        RenderUtil.fillRoundedRect(ctx, x, y, width, ROW_HEIGHT - 2, 5, fill);

        boolean enabled = Modules.isEnabled(module);
        Texts.draw(ctx, tr, module.name, x + 8, y + 3,
                enabled ? AzyronConstants.COLOR_TEXT : AzyronConstants.COLOR_TEXT_DIM, true);
        Texts.draw(ctx, tr, module.description, x + 8, y + 14, 0xFF7A8291, false);

        int switchX = x + width - 34;
        int switchY = y + (ROW_HEIGHT - 2 - 12) / 2;
        drawToggle(ctx, switchX, switchY, 26, 12, enabled);

        // Gear shortcut for configurable modules
        if (module.hasSettings) {
            int gearX = switchX - 18;
            int gearY = y + (ROW_HEIGHT - 2 - 14) / 2;
            boolean gearHover = mouseX >= gearX && mouseX < gearX + 14 && mouseY >= gearY && mouseY < gearY + 14;
            drawGear(ctx, gearX + 7, gearY + 7, 5,
                    gearHover ? AzyronConstants.COLOR_ELECTRIC : 0xFF8B7CF6);
        }
    }

    private static void drawToggle(DrawContext ctx, int x, int y, int width, int height, boolean on) {
        int track = on ? 0xFF0E86C9 : 0xFF332A4D;
        RenderUtil.fillRoundedRect(ctx, x, y, width, height, height / 2, track);
        int knobSize = height - 4;
        int knobX = on ? x + width - knobSize - 2 : x + 2;
        RenderUtil.fillRoundedRect(ctx, knobX, y + 2, knobSize, knobSize, knobSize / 2, 0xFFF8FAFC);
    }

    private static void drawGear(DrawContext ctx, int cx, int cy, int radius, int color) {
        // 8-tooth gear drawn with lines and a center disc.
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            int x1 = cx + (int) Math.round(Math.cos(angle) * (radius - 1));
            int y1 = cy + (int) Math.round(Math.sin(angle) * (radius - 1));
            int x2 = cx + (int) Math.round(Math.cos(angle) * (radius + 2));
            int y2 = cy + (int) Math.round(Math.sin(angle) * (radius + 2));
            drawThickLine(ctx, x1, y1, x2, y2, color);
        }
        for (int dy = -radius; dy <= radius; dy++) {
            double dyC = dy - 0.5D;
            double span = Math.sqrt(Math.max(0.0D, radius * (double) radius - dyC * dyC));
            int half = (int) Math.floor(span);
            ctx.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF14101F);
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

    private List<AzyronModule> filteredModules() {
        List<AzyronModule> base = new ArrayList<AzyronModule>(Modules.forCategory(activeSide.category));
        String query = search.text == null ? "" : search.text.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return base;
        }
        List<AzyronModule> out = new ArrayList<AzyronModule>();
        for (AzyronModule module : base) {
            if (module.name.toLowerCase(Locale.ROOT).contains(query)
                    || module.description.toLowerCase(Locale.ROOT).contains(query)) {
                out.add(module);
            }
        }
        return out;
    }

    private int enabledCount() {
        int count = 0;
        for (AzyronModule module : Modules.all()) {
            if (Modules.isEnabled(module)) {
                count++;
            }
        }
        return count;
    }

    /* ---------------------------------------------------------- */

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 0) {
            // Search focus
            if (search.contains(mouseX, mouseY)) {
                search.focused = true;
                return true;
            }
            search.focused = false;

            // Module rows
            int listX = SIDEBAR_WIDTH + 34;
            int listWidth = this.width - listX - 16;
            int listTop = search.y + SEARCH_HEIGHT + 6;
            int listBottom = this.height - 16;
            int y = listTop - scroll;
            List<AzyronModule> visible = filteredModules();
            for (AzyronModule module : visible) {
                if (y >= listTop && y + ROW_HEIGHT - 2 <= listBottom) {
                    int rowBottom = y + ROW_HEIGHT - 2;
                    if (mouseY >= y && mouseY < rowBottom) {
                        int switchX = listX + listWidth - 34;
                        int switchY = y + (ROW_HEIGHT - 2 - 12) / 2;
                        int gearX = switchX - 18;
                        int gearY = y + (ROW_HEIGHT - 2 - 14) / 2;
                        if (module.hasSettings && mouseX >= gearX && mouseX < gearX + 14
                                && mouseY >= gearY && mouseY < gearY + 14) {
                            MinecraftClient.getInstance().setScreen(new AzyronSettingsScreen(this));
                            return true;
                        }
                        if (mouseX >= switchX - 4 && mouseX < switchX + 30 && mouseY >= y && mouseY < rowBottom) {
                            Modules.setEnabled(module, !Modules.isEnabled(module));
                            return true;
                        }
                    }
                }
                y += ROW_HEIGHT;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= (int) (verticalAmount * ROW_HEIGHT);
        return true;
    }

    @Override
    public boolean charTyped(CharInput input) {
        char chr = (char) input.codepoint();
        if (search.focused) {
            search.charTyped(chr);
            scroll = 0;
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (search.focused && search.keyPressed(keyCode)) {
            return true;
        }
        return super.keyPressed(input);
    }
}
