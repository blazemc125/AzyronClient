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
        buildButtons();
    }

    protected AzyronButton addButton(String label, int x, int y, int width, int height, Runnable action) {
        AzyronButton button = new AzyronButton(label, x, y, width, height);
        button.action = action;
        buttons.add(button);
        return button;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MenuBackground.render(ctx, this.width, this.height, System.currentTimeMillis());
        TextRenderer tr = ClientRefs.textRenderer(MinecraftClient.getInstance());
        if (tr != null) {
            renderContent(ctx, tr, mouseX, mouseY, delta);
            for (AzyronButton button : buttons) {
                button.render(ctx, tr, mouseX, mouseY);
            }
            drawFooter(ctx, tr);
        }
    }

    /** Footer chrome: version, developer credit and current profile. */
    protected void drawFooter(DrawContext ctx, TextRenderer tr) {
        int y = this.height - 13;
        Texts.draw(ctx, tr, AzyronConstants.NAME + " v" + AzyronConstants.VERSION + " • MC " + AzyronConstants.MINECRAFT_VERSION,
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
                    btn.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        // GLFW_KEY_ESCAPE — sub-screens return to their parent, top-level closes.
        if (keyCode == 256) {
            goBack();
            return true;
        }
        return super.keyPressed(input);
    }

    protected void goBack() {
        MinecraftClient client = MinecraftClient.getInstance();
        AzyronConfig.save();
        client.setScreen(backTarget);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // handled in keyPressed so sub-screens can return home
    }

    @Override
    public void removed() {
        AzyronConfig.save();
    }

    @Override
    public boolean shouldPause() {
        // Keep the world ticking for single-player — premium clients don't freeze.
        return false;
    }
}
