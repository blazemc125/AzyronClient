package com.jiggneshgaming.azyron.mixin;

import com.jiggneshgaming.azyron.zoom.ZoomManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyReturnValue;

/**
 * Applies AZYRON's eased zoom to the effective FOV.
 *
 * <p>Optional mixin — if the yarn signature of {@code getFov} changed,
 * ZoomManager detects the mixin never applied and falls back to clamping
 * the FOV slider instead (never a crash).</p>
 */
@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float azyronclient$applyZoomFov(float original) {
        // Mark the mixin as operational so the FOV-slider fallback stays off.
        ZoomManager.fovMixinActive = true;
        try {
            float zoom = ZoomManager.effectiveZoom();
            if (zoom <= 1.01F) {
                return original;
            }
            return com.jiggneshgaming.azyron.core.zoom.ZoomMath.targetFov(original, zoom);
        } catch (Throwable ignored) {
            return original;
        }
    }
}
