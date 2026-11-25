package com.jiggneshgaming.azyron.compat;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

/**
 * AZYRON CLIENT V2 — input router for Minecraft 1.21.9+ (the "modern" GUI
 * input API: {@link Click}, {@link KeyInput}, {@link CharInput}).
 *
 * <p>Screens never touch the version-specific signatures directly; they
 * implement the version-agnostic {@code azyron*} hooks below. This class is
 * final-routed: it always forwards to the hooks first and only then to the
 * vanilla super method, so the vanilla ESC / focus behaviour is preserved
 * for every screen that does not consume the input itself.</p>
 */
public abstract class AzyronScreen extends Screen {

    protected AzyronScreen(Text title) {
        super(title);
    }

    /* -------------------------------------------------- hooks (override what you need) --- */
    protected boolean azyronClick(double mouseX, double mouseY, int button) {
        return false;
    }

    protected boolean azyronDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    protected boolean azyronRelease(double mouseX, double mouseY, int button) {
        return false;
    }

    protected boolean azyronKey(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    protected boolean azyronChar(char chr, int modifiers) {
        return false;
    }

    /* ------------------------------------------- final routing (1.21.9+) --- */
    @Override
    public final boolean mouseClicked(Click click, boolean doubled) {
        if (azyronClick(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public final boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (azyronDrag(click.x(), click.y(), click.button(), deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public final boolean mouseReleased(Click click) {
        if (azyronRelease(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public final boolean keyPressed(KeyInput input) {
        if (azyronKey(input.key(), 0, 0)) {
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public final boolean charTyped(CharInput input) {
        if (azyronChar((char) input.codepoint(), 0)) {
            return true;
        }
        return super.charTyped(input);
    }
}
