package net.fabricmc.fabric.api.client.rendering.v1.hud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
public final class HudElementRegistry {
    private HudElementRegistry() {}
    public static void addLast(Identifier id, HudRenderCallback element) {}
    public static void addFirst(Identifier id, HudRenderCallback element) {}
}
