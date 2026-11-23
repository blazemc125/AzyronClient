package com.jiggneshgaming.azyron.menu;

import com.jiggneshgaming.azyron.AzyronConstants;
import com.jiggneshgaming.azyron.config.AzyronConfig;
import com.jiggneshgaming.azyron.util.ClientRefs;
import com.jiggneshgaming.azyron.util.Texts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;

/**
 * AZYRON CLIENT — shared scaffold for all AZYRON screens.
 *
 * <p>Renders the premium menu background, footer chrome and dispatches
 * clicks to {@link AzyronButton} actions.</p>
 */
public abstract class BaseAzyronScreen extends Screen {

    protected final Screen backTarget;
    protected final List<AzyronButton> buttons = new ArrayList<AzyronButton>();

    protected BaseAzyronScreen(String title, Screen backTarget) {
        super(Text.literal(title));
        this.backTarget = backTarget;
    }

    protected abstract void buildButtons();

    protected abstract void renderContent(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY, float delta);

    @Override
    protected void init() {
        buttons.clear();
        try {
            buildButtons();
        } catch (Throwable t) {
            // Never propagate out of init — that would kill the screen and,
            // with it, the whole menu. Buttons are added one by one, so on
            // failure keep whichever ones made it and log loudly.
            LOGGER.error("Could not build the AZYRON screen layout — partial menu will be shown.", t);
        }
    }

    protected AzyronButton addButton(String label, int x, int y, int width, int height, Runnable action) {
        AzyronButton button = new AzyronButton(label, x, y, width, height);
        button.action = action;
        buttons.add(button);
        return button;
    }

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("azyronclient/ui");
    private static boolean warnedMissingFont;

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // 1) Background — isolated: if the procedural background ever fails,
        //    fall back to a plain dark fill so the GUI stays fully visible.
        try {
            MenuBackground.render(ctx, this.width, this.height, System.currentTimeMillis());
        } catch (Throwable t) {
            logOnce("background", t);
            ctx.fill(0, 0, this.width, this.height, 0xFF0D0A1A);
        }

        // 2) Text renderer — the protected field on Screen never fails to
        //    resolve. Reflection is only a last-resort fallback for paranoia.
        TextRenderer tr = this.textRenderer;
        if (tr == null) {
            try {
                tr = ClientRefs.textRenderer(MinecraftClient.getInstance());
            } catch (Throwable t) {
                logOnce("textRenderer", t);
            }
        }
        if (tr == null) {
            // Should be impossible — but NEVER leave a dead black screen.
            // Draw the buttons as bare panels so the GUI stays usable.
            if (!warnedMissingFont) {
                warnedMissingFont = true;
                LOGGER.error("TextRenderer unavailable — AZYRON menu will draw panels without text.");
            }
            for (AzyronButton button : buttons) {
                button.renderBare(ctx, mouseX, mouseY);
            }
            return;
        }

        // 3) Content — isolated: a failure in one decorative layer must not
        //    hide the buttons below it.
        try {
            renderContent(ctx, tr, mouseX, mouseY, delta);
        } catch (Throwable t) {
            logOnce("content", t);
            drawErrorBadge(ctx, tr, t);
        }

        // 4) Buttons — isolated per button.
        for (AzyronButton button : buttons) {
            try {
                button.render(ctx, tr, mouseX, mouseY);
            } catch (Throwable t) {
                logOnce("button:" + button.label, t);
            }
        }

        // 5) Footer — isolated.
        try {
            drawFooter(ctx, tr);
        } catch (Throwable t) {
            logOnce("footer", t);
        }
    }

    /** One log line per distinct failing phase — never spam the game loop. */
    private static final java.util.Set<String> REPORTED_PHASES =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private void logOnce(String phase, Throwable t) {
        if (REPORTED_PHASES.add(phase)) {
            LOGGER.error("AZYRON menu phase '{}' failed — continuing without it.", phase, t);
        }
    }

    /** Visible, honest error badge — the user always knows something broke. */
    private void drawErrorBadge(DrawContext ctx, TextRenderer tr, Throwable t) {
        String line = "AZYRON UI error: " + t.getClass().getSimpleName()
                + (t.getMessage() != null ? " — " + t.getMessage() : "");
        int w = Math.min(this.width - 16, tr.getWidth(line) + 16);
        int x = (this.width - w) / 2;
        int y = this.height - 64;
        ctx.fill(x, y, x + w, y + 18, 0xCC7F1D1D);
        Texts.draw(ctx, tr, line, x + 8, y + 5, 0xFFFFFFFF, false);
    }

    /** Footer chrome: version, developer credit and current profile. */
    protected void drawFooter(DrawContext ctx, TextRenderer tr) {
        int y = this.height - 13;
        Texts.draw(ctx, tr, AzyronConstants.NAME + " " + AzyronConstants.EDITION + "  •  v" + AzyronConstants.VERSION + "  •  MC " + AzyronConstants.MINECRAFT_VERSION,
                8, y, 0xFF6B7280, false);
        String credit = "Made by " + AzyronConstants.DEVELOPER;
        Texts.drawCentered(ctx, tr, credit, this.width / 2, y, 0xFF8B7CF6, false);
        String profile = "PROFILE: " + AzyronConfig.get().profile;
        Texts.draw(ctx, tr, profile, this.width - tr.getWidth(profile) - 8, y, AzyronConstants.COLOR_ELECTRIC, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 0) {
            for (AzyronButton btn : buttons) {
                if (btn.enabled && btn.contains(mouseX, mouseY) && btn.action != null) {
                    try {
                        btn.action.run();
                    } catch (Throwable t) {
                        LOGGER.error("AZYRON button '{}' action failed", btn.label, t);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    /** Android Back (mapped to ESC by the launcher) and desktop ESC. */
    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        // GLFW_KEY_ESCAPE — sub-screens return to their parent, top-level closes.
        if (keyCode == 256) {
            try {
                goBack();
            } catch (Throwable t) {
                LOGGER.error("Could not navigate back", t);
            }
            return true;
        }
        return super.keyPressed(input);
    }

    /**
     * Navigate back to the parent screen. Top-level screens (no parent):
     * return to the in-game HUD when a world is loaded, otherwise to the
     * AZYRON main menu — never to a blank/dead screen.
     */
    public void goBack() {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            AzyronConfig.save();
        } catch (Throwable t) {
            LOGGER.warn("Could not persist the config: {}", t.toString());
        }
        if (backTarget != null) {
            client.setScreen(backTarget);
            return;
        }
        if (client.world != null) {
            client.setScreen(null); // back to the in-game HUD
            return;
        }
        if (!(this instanceof AzyronMainMenuScreen)) {
            client.setScreen(new AzyronMainMenuScreen()); // safe escape — never a blank screen
        }
        // On the main menu itself with no world: stay where we are.
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // handled in keyPressed so sub-screens can return home
    }

    @Override
    public void removed() {
        try {
            AzyronConfig.save();
        } catch (Throwable t) {
            LOGGER.warn("Could not persist the config on screen close: {}", t.toString());
        }
    }

    @Override
    public boolean shouldPause() {
        // Keep the world ticking for single-player — premium clients don't freeze.
        return false;
    }
}
