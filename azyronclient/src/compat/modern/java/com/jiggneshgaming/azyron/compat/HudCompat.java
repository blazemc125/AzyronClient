package com.jiggneshgaming.azyron.compat;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

/**
 * MC 1.21.9+ HUD registration shim.
 *
 * <p>Wraps the modern {@code net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry}
 * introduced by recent Fabric API builds. Legacy profiles (&le;1.21.8) provide the same
 * class shape from {@code src/compat/legacy} backed by the old {@code HudRenderCallback} API.</p>
 */
public final class HudCompat {

    private HudCompat() {
    }

    @FunctionalInterface
    public interface HudDrawCallback {
        void render(DrawContext context, RenderTickCounter tickCounter);
    }

    /** Register a callback that draws after the vanilla HUD (equivalent to {@code addLast}). */
    public static void register(Identifier id, HudDrawCallback callback) {
        HudElementRegistry.addLast(id, (context, tickCounter) -> callback.render(context, tickCounter));
    }
}
