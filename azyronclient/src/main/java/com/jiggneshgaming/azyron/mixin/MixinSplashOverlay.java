package com.jiggneshgaming.azyron.mixin;

import com.jiggneshgaming.azyron.loading.AzyronLoadingScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AZYRON CLIENT — premium black & white loading screen.
 *
 * <p>Replaces the visuals of the vanilla startup/reload splash with the
 * AZYRON experience: AS monogram, "WELCOME TO AZYRON CLIENT", a real-time
 * progress bar (the reload's own getProgress()) and the current loading
 * phase. Injected at TAIL so the vanilla overlay keeps owning the entire
 * lifecycle (progress bookkeeping, fades, removal) — we only paint over it.
 * Loading is never blocked, never re-ordered, never duplicated.</p>
 *
 * <p>Optional mixin (required=false) — if the target moves, the vanilla
 * splash simply stays visible; nothing breaks.</p>
 */
@Mixin(SplashOverlay.class)
public abstract class MixinSplashOverlay {

    @Shadow @Final
    private ResourceReload reload;

    @Inject(method = "render", at = @At("TAIL"))
    private void azyronclient$brandLoadingScreen(DrawContext context, int mouseX, int mouseY,
                                                 float deltaTicks, CallbackInfo ci) {
        try {
            float progress = reload != null ? reload.getProgress() : 0.0F;
            boolean complete = reload != null && reload.isComplete();
            AzyronLoadingScreen.render(context, progress, complete);
        } catch (Throwable ignored) {
            // Splash render must never crash the game — vanilla stays beneath.
        }
    }
}
