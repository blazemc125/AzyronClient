package com.jiggneshgaming.azyron.mixin;

import com.jiggneshgaming.azyron.crosshair.Crosshairs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.CrosshairHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;

/**
 * Suppresses the vanilla crosshair when AZYRON's custom crosshair is active.
 * Optional mixin (required=false) — if the target signature moved, the
 * vanilla crosshair simply stays visible.
 */
@Mixin(CrosshairHud.class)
public abstract class MixinCrosshairHud {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void azyronclient$hideVanillaCrosshair(DrawContext context, RenderTickCounter tickCounter,
                                                   CallbackInfo ci) {
        try {
            if (Crosshairs.shouldHideVanilla()) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
            // Never break the vanilla HUD.
        }
    }
}
