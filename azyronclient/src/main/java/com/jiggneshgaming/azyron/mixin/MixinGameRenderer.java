package com.jiggneshgaming.azyron.mixin;

import com.jiggneshgaming.azyron.zoom.ZoomManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies AZYRON's eased zoom to the effective FOV.
 *
 * <p>Optional mixin — if the yarn signature of {@code getFov} changed,
 * ZoomManager detects the mixin never applied and falls back to clamping
 * the FOV slider instead (never a crash).</p>
 */
@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void azyronclient$applyZoomFov(CallbackInfoReturnable<Float> cir) {
        // Mark the mixin as operational so the FOV-slider fallback stays off.
        ZoomManager.fovMixinActive = true;
        try {
            float zoom = ZoomManager.effectiveZoom();
            if (zoom <= 1.01F) {
                return;
            }
            cir.setReturnValue(com.jiggneshgaming.azyron.core.zoom.ZoomMath.targetFov(cir.getReturnValue(), zoom));
        } catch (Throwable ignored) {
            // Leave vanilla FOV untouched.
        }
    }
}
