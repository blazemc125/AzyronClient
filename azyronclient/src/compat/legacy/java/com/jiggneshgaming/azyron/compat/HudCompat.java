package com.jiggneshgaming.azyron.compat;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

/**
 * Legacy (&le;1.21.8) HUD registration shim.
 *
 * <p>The {@code net.fabricmc.fabric.api.client.rendering.v1.hud} package (and the
 * {@code HudElementRegistry} it carries) does not exist in the Fabric API builds
 * published for these versions. The stable equivalent is the long-standing
 * {@code HudRenderCallback}, which fires after the whole vanilla HUD — the same
 * semantics as the modern {@code addLast} registration used by the modern profile.</p>
 */
public final class HudCompat {

    private HudCompat() {
    }

    @FunctionalInterface
    public interface HudDrawCallback {
        void render(DrawContext context, RenderTickCounter tickCounter);
    }

    /** Register a callback that draws after the vanilla HUD. */
    public static void register(Identifier id, HudDrawCallback callback) {
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
                (context, tickCounter) -> callback.render(context, tickCounter));
    }
}
