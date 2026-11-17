package net.fabricmc.fabric.api.client.rendering.v1.hud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
@FunctionalInterface
public interface HudRenderCallback {
    void render(DrawContext drawContext, RenderTickCounter tickCounter);
}
