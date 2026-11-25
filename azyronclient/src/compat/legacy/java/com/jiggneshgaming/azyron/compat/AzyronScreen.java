package com.jiggneshgaming.azyron.compat;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * AZYRON CLIENT V2 — input router for Minecraft 1.21 through 1.21.8
 * (the classic GUI input API: mouse callbacks with primitive coordinates,
 * {@code keyPressed(int, int, int)} and {@code charTyped(char, int)}).
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

    /* -------------------------------------- final routing (1.21 – 1.21.8) --- */
    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (azyronClick(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public final boolean mouseDragged(double mouseX, double mouseY, int button,
                                      double deltaX, double deltaY) {
        if (azyronDrag(mouseX, mouseY, button, deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public final boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (azyronRelease(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public final boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (azyronKey(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public final boolean charTyped(char chr, int modifiers) {
        if (azyronChar(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }
}
