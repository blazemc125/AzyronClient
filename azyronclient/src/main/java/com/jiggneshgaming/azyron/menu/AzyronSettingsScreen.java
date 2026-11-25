package com.jiggneshgaming.azyron.menu;

import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.config.AzyronConfig;
import com.jiggneshgaming.azyron.core.config.AzyronConfigModel;
import com.jiggneshgaming.azyron.fps.AzyronFpsScreen;
import com.jiggneshgaming.azyron.mods.AzyronModManagerScreen;
import com.jiggneshgaming.azyron.profiles.AzyronProfilesScreen;
import com.jiggneshgaming.azyron.quicksettings.AzyronQuickSettingsScreen;
import com.jiggneshgaming.azyron.screenshots.AzyronScreenshotsScreen;
import com.jiggneshgaming.azyron.util.Links;
import com.jiggneshgaming.azyron.util.RenderUtil;
import com.jiggneshgaming.azyron.util.Texts;
import com.jiggneshgaming.azyron.util.UpdateChecker;
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

    /** Supported zoom-key presets (GLFW codes) and their display names. */
    private static final int[] ZOOM_KEYS = {67, 86, 90, 88, 70};
    private static final String[] ZOOM_KEY_NAMES = {"C", "V", "Z", "X", "F"};
    private static final int[] ACCENTS = {0xFF33CCFF, 0xFF7C3AED, 0xFF4ADE80, 0xFFFACC15, 0xFFF87171, 0xFFFFFFFF};
    private static final String[] ACCENT_NAMES = {"Electric", "Amethyst", "Emerald", "Amber", "Crimson", "White"};

    private static final int ROW_HEIGHT = 18;
    private static final int SECTION_HEIGHT = 13;

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
        // Row lambdas must follow the LIVE model — config profiles can swap
        // the instance at runtime (AzyronConfig.replaceModel).
        AzyronConfigModel config = AzyronConfig.get();

        rows.add(new SectionRow("Zoom"));
        rows.add(new SliderRow("Zoom strength", newSlider("zoomLevel", 2.0D, 8.0D, 0.5D),
                () -> AzyronConfig.get().zoomLevel, v -> AzyronConfig.get().zoomLevel = v,
                v -> trim(String.format(java.util.Locale.ROOT, "%.1fx", v))));
        rows.add(new SliderRow("Zoom speed", newSlider("zoomSpeed", 0.25D, 2.0D, 0.25D),
                () -> AzyronConfig.get().zoomSensitivity, v -> AzyronConfig.get().zoomSensitivity = v,
                v -> trim(String.format(java.util.Locale.ROOT, "%.2fx", v))));
        rows.add(new ToggleRow("Smooth zoom", () -> AzyronConfig.get().smoothZoom, v -> AzyronConfig.get().smoothZoom = v));
        rows.add(new ToggleRow("Zoom enabled", () -> AzyronConfig.get().zoomEnabled, v -> AzyronConfig.get().zoomEnabled = v));
        rows.add(new CycleRow("Zoom key", () -> cycleZoomKey(), () -> zoomKeyName()));
        rows.add(new ToggleRow("Hold to zoom (off = press to toggle)",
                () -> AzyronConfig.get().zoomHoldMode, v -> AzyronConfig.get().zoomHoldMode = v));

        rows.add(new SectionRow("Custom Crosshair"));
        rows.add(new CycleRow("Style", () -> cycleStyle(AzyronConfig.get()), () -> AzyronConfig.get().crosshairStyle));
        rows.add(new ColorsRow());
        rows.add(new SliderRow("Size", newSlider("crosshairSize", 0.5D, 3.0D, 0.25D),
                () -> AzyronConfig.get().crosshairSize, v -> AzyronConfig.get().crosshairSize = v,
                v -> trim(String.format(java.util.Locale.ROOT, "%.2fx", v))));
        rows.add(new SliderRow("Thickness", newSlider("crosshairThickness", 0.5D, 3.0D, 0.25D),
                () -> AzyronConfig.get().crosshairThickness, v -> AzyronConfig.get().crosshairThickness = v,
                v -> trim(String.format(java.util.Locale.ROOT, "%.2fx", v))));
        rows.add(new SliderRow("Gap", newSlider("crosshairGap", 0.25D, 3.0D, 0.25D),
                () -> AzyronConfig.get().crosshairGap, v -> AzyronConfig.get().crosshairGap = v,
                v -> trim(String.format(java.util.Locale.ROOT, "%.2fx", v))));
        rows.add(new ToggleRow("Outline",
                () -> AzyronConfig.get().crosshairOutline, v -> AzyronConfig.get().crosshairOutline = v));
        rows.add(new ToggleRow("Dynamic (spreads when you move)",
                () -> AzyronConfig.get().crosshairDynamic, v -> AzyronConfig.get().crosshairDynamic = v));
        rows.add(new CycleRow("Reset crosshair", () -> resetCrosshair(), () -> "Defaults"));
        rows.add(new ToggleRow("Hide vanilla crosshair", () -> AzyronConfig.get().hideVanillaCrosshair, v -> AzyronConfig.get().hideVanillaCrosshair = v));

        rows.add(new SectionRow("HUD"));
        rows.add(new SliderRow("HUD scale", newSlider("hudScale", 0.5D, 2.0D, 0.25D),
                () -> AzyronConfig.get().hudScale, v -> AzyronConfig.get().hudScale = v,
                v -> trim(String.format(java.util.Locale.ROOT, "%.2fx", v))));
        rows.add(new SliderRow("Background opacity", newSlider("backgroundOpacity", 0.0D, 0.9D, 0.05D),
                () -> AzyronConfig.get().backgroundOpacity, v -> AzyronConfig.get().backgroundOpacity = v,
                v -> Integer.toString((int) Math.round(v * 100)) + "%"));
        rows.add(new CycleRow("HUD accent", () -> cycleAccent(), () -> accentName()));

        rows.add(new SectionRow("Performance"));
        rows.add(new ToggleRow("Toggle sprint", () -> AzyronConfig.get().toggleSprint, v -> AzyronConfig.get().toggleSprint = v));
        rows.add(new ToggleRow("Dynamic FPS (unfocused)", () -> AzyronConfig.get().dynamicFps, v -> AzyronConfig.get().dynamicFps = v));
        rows.add(new SliderRow("Unfocused FPS cap", newSlider("unfocusedFps", 1.0D, 60.0D, 1.0D),
                () -> (double) config.unfocusedFps, v -> AzyronConfig.get().unfocusedFps = (int) Math.round(v),
                v -> Integer.toString((int) Math.round(v)) + " fps"));

        rows.add(new SectionRow("Interface"));
        rows.add(new ToggleRow("Menu animations", () -> AzyronConfig.get().animations, v -> AzyronConfig.get().animations = v));
        rows.add(new CycleRow("Animation quality", () -> cycleAnimationQuality(), () -> AzyronConfig.get().animationQuality));

        rows.add(new SectionRow("Updates"));
        rows.add(new CycleRow("Check for updates", () -> UpdateChecker.checkAsync(),
                () -> updateBadge()));
        rows.add(new CycleRow("Release page", () -> Links.open(UpdateChecker.RELEASES_PAGE),
                () -> "Open"));
    }

    private static String zoomKeyName() {
        int key = AzyronConfig.get().zoomKey;
        for (int i = 0; i < ZOOM_KEYS.length; i++) {
            if (ZOOM_KEYS[i] == key) {
                return ZOOM_KEY_NAMES[i];
            }
        }
        return "C";
    }

    private static String cycleZoomKey() {
        AzyronConfigModel config = AzyronConfig.get();
        int index = 0;
        for (int i = 0; i < ZOOM_KEYS.length; i++) {
            if (ZOOM_KEYS[i] == config.zoomKey) {
                index = i;
                break;
            }
        }
        config.zoomKey = ZOOM_KEYS[(index + 1) % ZOOM_KEYS.length];
        AzyronConfig.save();
        return zoomKeyName();
    }

    private static String accentName() {
        int accent = AzyronConfig.get().hudAccent;
        for (int i = 0; i < ACCENTS.length; i++) {
            if ((ACCENTS[i] & 0x00FFFFFF) == (accent & 0x00FFFFFF)) {
                return ACCENT_NAMES[i];
            }
        }
        return "Custom";
    }

    private static String cycleAccent() {
        AzyronConfigModel config = AzyronConfig.get();
        int index = 0;
        for (int i = 0; i < ACCENTS.length; i++) {
            if ((ACCENTS[i] & 0x00FFFFFF) == (config.hudAccent & 0x00FFFFFF)) {
                index = i;
                break;
            }
        }
        config.hudAccent = ACCENTS[(index + 1) % ACCENTS.length];
        AzyronConfig.save();
        return accentName();
    }

    private static String cycleAnimationQuality() {
        AzyronConfigModel config = AzyronConfig.get();
        String current = config.animationQuality == null ? "MEDIUM" : config.animationQuality;
        String next = current.equals("LOW") ? "MEDIUM" : current.equals("MEDIUM") ? "HIGH" : "LOW";
        config.animationQuality = next;
        AzyronConfig.save();
        return next;
    }

    private static String updateBadge() {
        switch (UpdateChecker.state()) {
            case CHECKING:
                return "Checking...";
            case UP_TO_DATE:
                return "Up to date";
            case UPDATE_AVAILABLE:
                return "v" + UpdateChecker.latestVersion() + " AVAILABLE";
            case ERROR:
                return "Failed — retry";
            default:
                return "Check now";
        }
    }

    /** Restore every crosshair option to the factory defaults. */
    private static String resetCrosshair() {
        AzyronConfigModel config = AzyronConfig.get();
        config.crosshairStyle = "CLASSIC";
        config.crosshairColor = 0xFF33CCFF;
        config.crosshairSize = 1.0D;
        config.crosshairThickness = 1.0D;
        config.crosshairGap = 1.0D;
        config.crosshairOutline = true;
        config.crosshairDynamic = false;
        config.hideVanillaCrosshair = true;
        AzyronConfig.save();
        return "Defaults";
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
        int y = 26 + 32;
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
        int y = this.height - 30;

        addButton("Reset HUD layout", panelX, y, 132, 22, () -> {
            AzyronConfig.get().resetPositions();
            AzyronConfig.save();
        });
        this.resetAllButton = addButton("Reset EVERYTHING", panelX + panelW - 148, y, 148, 22, this::onResetAll);
        addButton("Back", panelX + 140, y, 84, 22, this::goBack);

        // V2 navigation row — every destination, one tap away.
        int navY = this.height - 56;
        int navWidth = 90;
        int navGap = 4;
        int navX = panelX;
        addButton("FPS Booster", navX, navY, navWidth, 18,
                () -> net.minecraft.client.MinecraftClient.getInstance().setScreen(new AzyronFpsScreen(this)));
        addButton("Quick Settings", navX + (navWidth + navGap), navY, navWidth, 18,
                () -> net.minecraft.client.MinecraftClient.getInstance().setScreen(new AzyronQuickSettingsScreen(this)));
        addButton("Screenshots", navX + (navWidth + navGap) * 2, navY, navWidth, 18,
                () -> net.minecraft.client.MinecraftClient.getInstance().setScreen(new AzyronScreenshotsScreen(this)));
        addButton("Profiles", navX + (navWidth + navGap) * 3, navY, navWidth, 18,
                () -> net.minecraft.client.MinecraftClient.getInstance().setScreen(new AzyronProfilesScreen(this)));
        addButton("Mod Manager", navX + (navWidth + navGap) * 4, navY, 104, 18,
                () -> net.minecraft.client.MinecraftClient.getInstance().setScreen(new AzyronModManagerScreen(this)));
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
        int panelY = 26;
        int panelH = this.height - 94;

        RenderUtil.fillMenuPanel(ctx, panelX, panelY, panelW, panelH, 8);
        Texts.draw(ctx, tr, "AZYRON SETTINGS", panelX + 14, panelY + 8, AzyronConstants.COLOR_TEXT, true);
        Texts.draw(ctx, tr, "AZYRON CLIENT V2 — every option applies live",
                panelX + 14, panelY + 19, AzyronConstants.COLOR_TEXT_DIM, false);

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

        int statusColor = UpdateChecker.state() == UpdateChecker.State.UPDATE_AVAILABLE
                ? AzyronConstants.COLOR_WARN
                : (UpdateChecker.state() == UpdateChecker.State.ERROR ? AzyronConstants.COLOR_BAD : AzyronConstants.COLOR_TEXT_DIM);
        Texts.draw(ctx, tr, "Updates: " + UpdateChecker.statusLine(),
                panelX + 14, panelY + panelH - 14, statusColor, false);

        if (resetAllArmedAt != 0L && System.currentTimeMillis() - resetAllArmedAt < 3500L) {
            Texts.draw(ctx, tr, "Click Reset EVERYTHING again to wipe every setting!",
                    panelX + 14, this.height - 66, AzyronConstants.COLOR_BAD, true);
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
    protected boolean azyronClick(double mouseX, double mouseY, int button) {
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
        return super.azyronClick(mouseX, mouseY, button);
    }

    @Override
    protected boolean azyronDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging != null && button == 0) {
            for (Row row : rows) {
                if (row instanceof SliderRow && ((SliderRow) row).slider == dragging) {
                    ((SliderRow) row).setter.accept(Double.valueOf(dragging.valueForMouse(mouseX)));
                    return true;
                }
            }
        }
        return super.azyronDrag(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    protected boolean azyronRelease(double mouseX, double mouseY, int button) {
        if (dragging != null) {
            dragging = null;
            AzyronConfig.save();
            return true;
        }
        return super.azyronRelease(mouseX, mouseY, button);
    }
}
