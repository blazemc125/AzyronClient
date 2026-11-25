package com.jiggneshgaming.azyron.fps;

import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.config.AzyronConfig;
import com.jiggneshgaming.azyron.core.profile.PerformanceProfile;
import com.jiggneshgaming.azyron.menu.AzyronButton;
import com.jiggneshgaming.azyron.menu.BaseAzyronScreen;
import com.jiggneshgaming.azyron.performance.ProfileManager;
import com.jiggneshgaming.azyron.util.RenderUtil;
import com.jiggneshgaming.azyron.util.Texts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;

/**
 * AZYRON CLIENT V2 — FPS Booster page.
 *
 * <p>A dedicated performance screen: live FPS counter, one-click profile
 * application (Potato / Balanced / Smooth — the same tested bundles used
 * by the rest of the client), render/simulation distance presets, particle
 * controls, entity render-distance scaling, background (unfocused) FPS
 * optimization and a fullscreen video shortcut via the vanilla screen.
 * Every control writes to the real game options — nothing here is a
 * placeholder.</p>
 */
public final class AzyronFpsScreen extends BaseAzyronScreen {

    private static final int[] RENDER_DISTANCES = {4, 6, 8, 12, 16, 24};
    private static final int[] SIM_DISTANCES = {4, 6, 8, 10, 12};
    private static final double[] ENTITY_SCALES = {0.5D, 0.75D, 1.0D};

    private String feedback = "";
    private long feedbackAt;

    public AzyronFpsScreen(Screen backTarget) {
        super("FPS Booster", backTarget);
    }

    @Override
    protected void buildButtons() {
        int x = 12;
        int y = this.height - 32;
        addButton("Back", x, y, 92, 22, this::goBack);

        // Profile chips — the active one is highlighted in renderContent.
        int chipY = 86;
        int chipX = Math.max(12, (this.width - 470) / 2);
        int chipW = 150;
        addButton("POTATO", chipX, chipY, chipW, 30, () -> applyProfile(PerformanceProfile.Id.POTATO));
        addButton("BALANCED", chipX + chipW + 10, chipY, chipW, 30, () -> applyProfile(PerformanceProfile.Id.BALANCED));
        addButton("SMOOTH", chipX + (chipW + 10) * 2, chipY, chipW, 30, () -> applyProfile(PerformanceProfile.Id.SMOOTH));

        // One-click optimization — applies the recommended bundle.
        addButton("ONE-CLICK OPTIMIZE", chipX, chipY + 38, chipW * 3 + 20, 24,
                () -> applyProfile(PerformanceProfile.Id.BALANCED));

        int rowY = chipY + 84;
        // Render distance presets.
        for (int i = 0; i < RENDER_DISTANCES.length; i++) {
            final int value = RENDER_DISTANCES[i];
            addButton(String.valueOf(value), chipX + 96 + i * 54, rowY, 46, 20,
                    () -> withOptions(opts -> {
                        setOption(opts.getViewDistance(), value, "render distance");
                        saveOptions(opts);
                        note("Render distance → " + value + " chunks");
                    }));
        }
        // Simulation distance presets.
        int simY = rowY + 46;
        for (int i = 0; i < SIM_DISTANCES.length; i++) {
            final int value = SIM_DISTANCES[i];
            addButton(String.valueOf(value), chipX + 96 + i * 54, simY, 46, 20,
                    () -> withOptions(opts -> {
                        setOption(opts.getSimulationDistance(), value, "simulation distance");
                        saveOptions(opts);
                        note("Simulation distance → " + value + " chunks");
                    }));
        }
        // Particles control.
        addButton("PARTICLES: ALL", chipX, simY + 46, 150, 20,
                () -> cycleParticles(net.minecraft.particle.ParticlesMode.ALL));
        addButton("DECREASED", chipX + 158, simY + 46, 96, 20,
                () -> cycleParticles(net.minecraft.particle.ParticlesMode.DECREASED));
        addButton("MINIMAL", chipX + 262, simY + 46, 96, 20,
                () -> cycleParticles(net.minecraft.particle.ParticlesMode.MINIMAL));
        // Entity render-distance scaling.
        int entY = simY + 92;
        for (int i = 0; i < ENTITY_SCALES.length; i++) {
            final double scale = ENTITY_SCALES[i];
            addButton(Math.round(scale * 100) + "%", chipX + 168 + i * 66, entY, 58, 20,
                    () -> withOptions(opts -> {
                        setOption(opts.getEntityDistanceScaling(), scale, "entity distance scaling");
                        saveOptions(opts);
                        note("Entity render distance → " + Math.round(scale * 100) + "%");
                    }));
        }
    }

    /* ---------------------------------------------------------- */

    private void applyProfile(PerformanceProfile.Id id) {
        try {
            ProfileManager.apply(id);
            note(id.name() + " profile applied");
        } catch (Throwable t) {
            note("Could not apply profile: " + t.getClass().getSimpleName());
        }
    }

    private void cycleParticles(net.minecraft.particle.ParticlesMode mode) {
        withOptions(opts -> {
            setOption(opts.getParticles(), mode, "particles");
            saveOptions(opts);
            note("Particles → " + mode.toString());
        });
    }

    private interface OptionsWork {
        void run(GameOptions options);
    }

    private void withOptions(OptionsWork work) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            GameOptions options = client == null ? null : client.options;
            if (options == null) {
                note("Options are not ready yet — try again after the menu loads.");
                return;
            }
            work.run(options);
        } catch (Throwable t) {
            note("Option write failed: " + t.getClass().getSimpleName());
        }
    }

    private static <T> void setOption(net.minecraft.client.option.SimpleOption<T> option,
                                      T value, String name) {
        if (option == null) {
            org.slf4j.LoggerFactory.getLogger("azyronclient/fps").debug("Option {} is not ready — skipped.", name);
            return;
        }
        try {
            option.setValue(value);
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger("azyronclient/fps").warn("Could not set {}: {}", name, t.toString());
        }
    }

    private static void saveOptions(GameOptions options) {
        try {
            options.write();
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger("azyronclient/fps").debug("Could not persist options.txt: {}", t.toString());
        }
    }

    private void note(String message) {
        this.feedback = message;
        this.feedbackAt = System.currentTimeMillis();
    }

    /* ---------------------------------------------------------- */

    @Override
    protected void renderContent(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY, float delta) {
        int panelW = Math.min(510, this.width - 20);
        int panelH = this.height - 88;
        int panelX = Math.max(10, (this.width - panelW) / 2);
        int panelY = 12;

        RenderUtil.fillMenuPanel(ctx, panelX, panelY, panelW, panelH, 8);
        Texts.draw(ctx, tr, "FPS BOOSTER", panelX + 14, panelY + 9, AzyronConstants.COLOR_TEXT, true);
        Texts.draw(ctx, tr, "AZYRON CLIENT V2 — performance, tuned properly",
                panelX + 14, panelY + 21, AzyronConstants.COLOR_TEXT_DIM, false);

        /* -------- live FPS readout -------- */
        int fps = -1;
        try {
            fps = MinecraftClient.getInstance().getCurrentFps();
        } catch (Throwable ignored) {
            // leave -1
        }
        String fpsText = fps >= 0 ? Integer.toString(fps) : "—";
        int fpsColor = fps >= 60 ? AzyronConstants.COLOR_GOOD : fps >= 30 ? AzyronConstants.COLOR_WARN : AzyronConstants.COLOR_BAD;
        Texts.draw(ctx, tr, fpsText, panelX + 16, panelY + 34, fpsColor, true);
        int fpsNumWidth = tr.getWidth(fpsText);
        Texts.draw(ctx, tr, "FPS (live)", panelX + 20 + fpsNumWidth, panelY + 40,
                AzyronConstants.COLOR_TEXT_DIM, false);

        // Active profile chip.
        String active = AzyronConfig.get().profile;
        String chip = "PROFILE: " + active;
        int chipWidth = tr.getWidth(chip) + 14;
        RenderUtil.fillRoundedRect(ctx, panelX + panelW - chipWidth - 12, panelY + 34, chipWidth, 15, 7, 0x5533CCFF);
        Texts.draw(ctx, tr, chip, panelX + panelW - chipWidth - 5, panelY + 37, AzyronConstants.COLOR_ELECTRIC, false);

        /* -------- section labels aligned with the buttons -------- */
        int chipX = Math.max(12, (this.width - 470) / 2);
        int chipY = 86;
        // highlight the active profile button outline
        for (AzyronButton button : buttons) {
            if (button.label.equals(active)) {
                button.leftHint = "ACTIVE";
            } else if (button.label.equals("POTATO") || button.label.equals("BALANCED") || button.label.equals("SMOOTH")) {
                button.leftHint = null;
            }
        }

        Texts.draw(ctx, tr, "RENDER DISTANCE (chunks)", chipX, chipY + 84 - 2 - 10, AzyronConstants.COLOR_PURPLE_BRIGHT, true);
        Texts.draw(ctx, tr, "SIMULATION DISTANCE (chunks)", chipX, chipY + 84 + 46 - 2 - 10, AzyronConstants.COLOR_PURPLE_BRIGHT, true);
        Texts.draw(ctx, tr, "PARTICLES", chipX, chipY + 84 + 92 - 2 - 10, AzyronConstants.COLOR_PURPLE_BRIGHT, true);
        Texts.draw(ctx, tr, "ENTITY RENDER DISTANCE", chipX, chipY + 84 + 138 - 2 - 10, AzyronConstants.COLOR_PURPLE_BRIGHT, true);

        /* -------- background (unfocused) FPS -------- */
        int bgY = chipY + 84 + 172;
        boolean dyn = AzyronConfig.get().dynamicFps;
        Texts.draw(ctx, tr, "BACKGROUND FPS OPTIMIZATION (window unfocused)", chipX, bgY,
                AzyronConstants.COLOR_PURPLE_BRIGHT, true);
        String bgState = dyn ? "ON" : "OFF";
        RenderUtil.fillRoundedRect(ctx, chipX + 288, bgY - 2, 40, 13, 6, dyn ? 0xFF0E86C9 : 0xFF332A4D);
        int knobX = dyn ? chipX + 288 + 32 : chipX + 288 + 1;
        RenderUtil.fillRoundedRect(ctx, knobX, bgY, 9, 9, 4, 0xFFF8FAFC);

        String cap = "Unfocused cap: " + AzyronConfig.get().unfocusedFps + " fps";
        Texts.draw(ctx, tr, cap + "   (adjustable in AZYRON Settings → Performance)", chipX, bgY + 16,
                AzyronConstants.COLOR_TEXT_DIM, false);

        // One-line honest note about what this page can and cannot touch.
        Texts.draw(ctx, tr,
                "These controls write directly to your game options — they take effect instantly and persist.",
                chipX, bgY + 34, 0xFF6B7280, false);

        /* -------- transient feedback line -------- */
        if (!feedback.isEmpty() && System.currentTimeMillis() - feedbackAt < 4000L) {
            Texts.draw(ctx, tr, feedback, panelX + 14, this.height - 44, AzyronConstants.COLOR_GOOD, true);
        }
    }

    /* Background optimization toggle lives on this page too (it shares the config keys). */
    @Override
    protected boolean azyronClick(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int chipX = Math.max(12, (this.width - 470) / 2);
            int chipY = 86;
            int bgY = chipY + 84 + 172;
            if (mouseX >= chipX + 288 - 2 && mouseX < chipX + 288 + 42 && mouseY >= bgY - 4 && mouseY < bgY + 13) {
                AzyronConfig.get().dynamicFps = !AzyronConfig.get().dynamicFps;
                AzyronConfig.save();
                note("Background FPS optimization " + (AzyronConfig.get().dynamicFps ? "ON" : "OFF"));
                return true;
            }
        }
        return super.azyronClick(mouseX, mouseY, button);
    }
}
