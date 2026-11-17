package com.jiggneshgaming.azyron.menu;

import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.config.AzyronConfig;
import com.jiggneshgaming.azyron.core.config.AzyronConfigModel;
import com.jiggneshgaming.azyron.util.RenderUtil;
import com.jiggneshgaming.azyron.util.Texts;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * AZYRON CLIENT — global settings screen.
 *
 * <p>Data-driven rows; the same list drives rendering and click handling,
 * so the two can never drift apart. Everything applies live.</p>
 */
public final class AzyronSettingsScreen extends BaseAzyronScreen {

    private static final String[] CROSSHAIR_STYLES = {
            "VANILLA", "CLASSIC", "PLUS", "DOT", "CIRCLE", "T_SHAPE"
    };

    private static final int[] CROSSHAIR_COLORS = {
            0xFF33CCFF, 0xFF7C3AED, 0xFF4ADE80, 0xFFFACC15, 0xFFF87171, 0xFFF472B6, 0xFFFFFFFF
    };

    private static final int ROW_HEIGHT = 22;
    private static final int SECTION_HEIGHT = 17;

    private interface Row {
        // marker interface
    }

    private static final class SectionRow implements Row {
        final String title;

        SectionRow(String title) {
            this.title = title;
        }
    }

    private static final class SliderRow implements Row {
        final String label;
        final SettingSlider slider;
        final java.util.function.DoubleSupplier getter;
        final java.util.function.DoubleConsumer setter;
        final java.util.function.Function<Double, String> formatter;

        SliderRow(String label, SettingSlider slider,
                  java.util.function.DoubleSupplier getter,
                  java.util.function.DoubleConsumer setter,
                  java.util.function.Function<Double, String> formatter) {
            this.label = label;
            this.slider = slider;
            this.getter = getter;
            this.setter = setter;
            this.formatter = formatter;
        }
    }

    private static final class ToggleRow implements Row {
        final String label;
        final BooleanGetter getter;
        final BooleanSetter setter;

        ToggleRow(String label, BooleanGetter getter, BooleanSetter setter) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
        }
    }

    private static final class CycleRow implements Row {
        final String label;
        final Runnable onCycle;
        final java.util.function.Supplier<String> valueSupplier;

        CycleRow(String label, Runnable onCycle, java.util.function.Supplier<String> valueSupplier) {
            this.label = label;
            this.onCycle = onCycle;
            this.valueSupplier = valueSupplier;
        }
    }

    private static final class ColorsRow implements Row {
        // Crosshair color swatches
    }

    private interface BooleanGetter {
        boolean get();
    }

    private interface BooleanSetter {
        void set(boolean value);
    }

    private final List<Row> rows = new ArrayList<Row>();
    private final List<SettingSlider> sliders = new ArrayList<SettingSlider>();
    private final List<Integer> rowY = new ArrayList<Integer>();
    private SettingSlider dragging;
    private long resetAllArmedAt;
    private AzyronButton resetAllButton;

    public AzyronSettingsScreen(Screen backTarget) {
        super("AZYRON Settings", backTarget);
        buildRows();
    }

    /* ---------------------------------------------------------- */

    private int panelX() {
        return Math.max(12, (this.width - 480) / 2);
    }

    private int panelWidth() {
        return Math.min(480, this.width - 24);
    }

    private int controlX() {
        return panelX() + 188;
    }

    private int controlWidth() {
        return panelX() + panelWidth() - controlX() - 56;
    }

    private SettingSlider newSlider(String id, double min, double max, double step) {
        SettingSlider slider = new SettingSlider(id, min, max, step, controlX(), 0, Math.max(60, controlWidth()));
        sliders.add(slider);
        return slider;
    }

    private void buildRows() {
        AzyronConfigModel config = AzyronConfig.get();

        rows.add(new SectionRow("Zoom"));
        rows.add(new SliderRow("Zoom strength", newSlider("zoomLevel", 2.0D, 8.0D, 0.5D),
                () -> config.zoomLevel, v -> config.zoomLevel = v,
                v -> trim(String.format(java.util.Locale.ROOT, "%.1fx", v))));
        rows.add(new ToggleRow("Smooth zoom", () -> config.smoothZoom, v -> config.smoothZoom = v));
        rows.add(new ToggleRow("Zoom enabled", () -> config.zoomEnabled, v -> config.zoomEnabled = v));

        rows.add(new SectionRow("Custom Crosshair"));
        rows.add(new CycleRow("Style", () -> cycleStyle(config), () -> config.crosshairStyle));
        rows.add(new ColorsRow());
        rows.add(new SliderRow("Size", newSlider("crosshairSize", 0.5D, 3.0D, 0.25D),
                () -> config.crosshairSize, v -> config.crosshairSize = v,
                v -> trim(String.format(java.util.Locale.ROOT, "%.2fx", v))));
        rows.add(new ToggleRow("Hide vanilla crosshair", () -> config.hideVanillaCrosshair, v -> config.hideVanillaCrosshair = v));

        rows.add(new SectionRow("HUD"));
        rows.add(new SliderRow("HUD scale", newSlider("hudScale", 0.5D, 2.0D, 0.25D),
                () -> config.hudScale, v -> config.hudScale = v,
                v -> trim(String.format(java.util.Locale.ROOT, "%.2fx", v))));
        rows.add(new SliderRow("Background opacity", newSlider("backgroundOpacity", 0.0D, 0.9D, 0.05D),
                () -> config.backgroundOpacity, v -> config.backgroundOpacity = v,
                v -> Integer.toString((int) Math.round(v * 100)) + "%"));

        rows.add(new SectionRow("Performance"));
        rows.add(new ToggleRow("Toggle sprint", () -> config.toggleSprint, v -> config.toggleSprint = v));
        rows.add(new ToggleRow("Dynamic FPS (unfocused)", () -> config.dynamicFps, v -> config.dynamicFps = v));
        rows.add(new SliderRow("Unfocused FPS cap", newSlider("unfocusedFps", 1.0D, 60.0D, 1.0D),
                () -> (double) config.unfocusedFps, v -> config.unfocusedFps = (int) Math.round(v),
                v -> Integer.toString((int) Math.round(v)) + " fps"));

        rows.add(new SectionRow("Interface"));
        rows.add(new ToggleRow("Menu animations", () -> config.animations, v -> config.animations = v));
    }

    private static String trim(String value) {
        String suffix = "";
        String out = value;
        if (out.endsWith("x")) {
            suffix = "x";
            out = out.substring(0, out.length() - 1);
        }
        if (out.contains(".")) {
            while (out.endsWith("0")) {
                out = out.substring(0, out.length() - 1);
            }
            if (out.endsWith(".")) {
                out = out.substring(0, out.length() - 1);
            }
        }
        return out + suffix;
    }

    /** Compute absolute Y positions for each row; called on render and click. */
    private void layoutRows() {
        rowY.clear();
        int y = 30 + 40;
        for (Row row : rows) {
            if (row instanceof SectionRow) {
                rowY.add(y);
                y += SECTION_HEIGHT;
            } else {
                rowY.add(y);
                if (row instanceof SliderRow) {
                    SettingSlider slider = ((SliderRow) row).slider;
                    slider.x = controlX();
                    slider.y = y;
                    slider.width = Math.max(60, controlWidth());
                }
                y += ROW_HEIGHT;
            }
        }
    }

    @Override
    protected void buildButtons() {
        int panelX = panelX();
        int panelW = panelWidth();
        int y = this.height - 34;

        addButton("Reset HUD layout", panelX, y, 132, 22, () -> {
            AzyronConfig.get().resetPositions();
            AzyronConfig.save();
        });
        this.resetAllButton = addButton("Reset EVERYTHING", panelX + panelW - 148, y, 148, 22, this::onResetAll);
        addButton("Back", panelX + 140, y, 84, 22, this::goBack);
    }

    private void onResetAll() {
        long now = System.currentTimeMillis();
        if (now - resetAllArmedAt < 3500L) {
            AzyronConfig.resetAll();
            resetAllArmedAt = 0L;
        } else {
            resetAllArmedAt = now;
        }
    }

    @Override
    protected void renderContent(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY, float delta) {
        if (this.resetAllButton != null) {
            boolean armed = System.currentTimeMillis() - this.resetAllArmedAt < 3500L;
            this.resetAllButton.label = armed ? "Click again!" : "Reset EVERYTHING";
            this.resetAllButton.enabled = true;
        }
        int panelX = panelX();
        int panelW = panelWidth();
        int panelY = 30;
        int panelH = this.height - 74;

        RenderUtil.fillMenuPanel(ctx, panelX, panelY, panelW, panelH, 8);
        Texts.draw(ctx, tr, "AZYRON SETTINGS", panelX + 14, panelY + 11, AzyronConstants.COLOR_TEXT, true);
        Texts.draw(ctx, tr, "Every option applies live — no restart required",
                panelX + 14, panelY + 23, AzyronConstants.COLOR_TEXT_DIM, false);

        layoutRows();
        int index = 0;
        for (Row row : rows) {
            int y = rowY.get(index++);
            if (row instanceof SectionRow) {
                Texts.draw(ctx, tr, ((SectionRow) row).title.toUpperCase(java.util.Locale.ROOT),
                        panelX + 14, y + 4, AzyronConstants.COLOR_PURPLE_BRIGHT, true);
            } else if (row instanceof SliderRow) {
                SliderRow sliderRow = (SliderRow) row;
                Texts.draw(ctx, tr, sliderRow.label, panelX + 14, y + 3, AzyronConstants.COLOR_TEXT, true);
                double value = sliderRow.getter.getAsDouble();
                sliderRow.slider.render(ctx, tr, value, sliderRow.formatter.apply(Double.valueOf(value)));
            } else if (row instanceof ToggleRow) {
                ToggleRow toggleRow = (ToggleRow) row;
                Texts.draw(ctx, tr, toggleRow.label, panelX + 14, y + 3, AzyronConstants.COLOR_TEXT, true);
                drawMiniToggle(ctx, controlX(), y + 1, 28, 13, toggleRow.getter.get());
            } else if (row instanceof CycleRow) {
                CycleRow cycleRow = (CycleRow) row;
                Texts.draw(ctx, tr, cycleRow.label, panelX + 14, y + 3, AzyronConstants.COLOR_TEXT, true);
                drawCycleButton(ctx, tr, controlX(), y, 132, 15, cycleRow.valueSupplier.get());
            } else if (row instanceof ColorsRow) {
                drawColorsRow(ctx, tr, panelX + 14, y, controlX(), AzyronConfig.get());
            }
        }

        if (resetAllArmedAt != 0L && System.currentTimeMillis() - resetAllArmedAt < 3500L) {
            Texts.draw(ctx, tr, "Click Reset EVERYTHING again to wipe every setting!",
                    panelX + 14, this.height - 62, AzyronConstants.COLOR_BAD, true);
        }
    }

    private void drawColorsRow(DrawContext ctx, TextRenderer tr, int labelX, int y, int swatchStartX,
                               AzyronConfigModel config) {
        Texts.draw(ctx, tr, "Color", labelX, y + 3, AzyronConstants.COLOR_TEXT, true);
        int swatch = 15;
        for (int i = 0; i < CROSSHAIR_COLORS.length; i++) {
            int sx = swatchStartX + i * (swatch + 5);
            boolean selected = (config.crosshairColor & 0x00FFFFFF) == (CROSSHAIR_COLORS[i] & 0x00FFFFFF);
            ctx.fill(sx, y, sx + swatch, y + swatch, CROSSHAIR_COLORS[i]);
            RenderUtil.drawRoundedBorder(ctx, sx, y, swatch, swatch, 3,
                    selected ? AzyronConstants.COLOR_ELECTRIC : AzyronConstants.COLOR_PANEL_BORDER);
            if (selected) {
                ctx.fill(sx + swatch / 2 - 1, y + swatch / 2 - 1, sx + swatch / 2 + 1, y + swatch / 2 + 1, 0xFF0B0714);
            }
        }
    }

    private static void drawMiniToggle(DrawContext ctx, int x, int y, int width, int height, boolean on) {
        RenderUtil.fillRoundedRect(ctx, x, y, width, height, height / 2, on ? 0xFF0E86C9 : 0xFF332A4D);
        int knob = height - 4;
        int knobX = on ? x + width - knob - 2 : x + 2;
        RenderUtil.fillRoundedRect(ctx, knobX, y + 2, knob, knob, knob / 2, 0xFFF8FAFC);
    }

    private static void drawCycleButton(DrawContext ctx, TextRenderer tr, int x, int y, int width, int height, String value) {
        RenderUtil.fillRoundedRect(ctx, x, y, width, height, 4, 0xFF241A3E);
        RenderUtil.drawRoundedBorder(ctx, x, y, width, height, 4, AzyronConstants.COLOR_PANEL_BORDER);
        Texts.draw(ctx, tr, value + "  >", x + 6, y + 3, AzyronConstants.COLOR_TEXT, false);
    }

    private static String cycleStyle(AzyronConfigModel config) {
        int index = 0;
        for (int i = 0; i < CROSSHAIR_STYLES.length; i++) {
            if (CROSSHAIR_STYLES[i].equals(config.crosshairStyle)) {
                index = i;
                break;
            }
        }
        config.crosshairStyle = CROSSHAIR_STYLES[(index + 1) % CROSSHAIR_STYLES.length];
        AzyronConfig.save();
        return config.crosshairStyle;
    }

    /* --------------------------- events ---------------------------- */

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            layoutRows();
            AzyronConfigModel config = AzyronConfig.get();
            int index = 0;
            for (Row row : rows) {
                int y = rowY.get(index++);
                if (row instanceof SliderRow) {
                    SettingSlider slider = ((SliderRow) row).slider;
                    if (slider.contains(mouseX, mouseY)) {
                        dragging = slider;
                        ((SliderRow) row).setter.accept(Double.valueOf(slider.valueForMouse(mouseX)));
                        AzyronConfig.save();
                        return true;
                    }
                } else if (row instanceof ToggleRow) {
                    ToggleRow toggleRow = (ToggleRow) row;
                    int controlX = controlX();
                    if (mouseX >= controlX - 4 && mouseX < controlX + 36 && mouseY >= y - 2 && mouseY < y + ROW_HEIGHT) {
                        toggleRow.setter.set(!toggleRow.getter.get());
                        AzyronConfig.save();
                        return true;
                    }
                } else if (row instanceof CycleRow) {
                    int controlX = controlX();
                    if (mouseX >= controlX && mouseX < controlX + 132 && mouseY >= y && mouseY < y + 15) {
                        ((CycleRow) row).onCycle.run();
                        return true;
                    }
                } else if (row instanceof ColorsRow) {
                    int swatch = 15;
                    for (int i = 0; i < CROSSHAIR_COLORS.length; i++) {
                        int sx = controlX() + i * (swatch + 5);
                        if (mouseX >= sx && mouseX < sx + swatch && mouseY >= y && mouseY < y + swatch) {
                            config.crosshairColor = CROSSHAIR_COLORS[i];
                            AzyronConfig.save();
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging != null && button == 0) {
            for (Row row : rows) {
                if (row instanceof SliderRow && ((SliderRow) row).slider == dragging) {
                    ((SliderRow) row).setter.accept(Double.valueOf(dragging.valueForMouse(mouseX)));
                    return true;
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) {
            dragging = null;
            AzyronConfig.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
