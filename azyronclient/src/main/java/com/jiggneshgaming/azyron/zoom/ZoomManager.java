package com.jiggneshgaming.azyron.zoom;

import com.jiggneshgaming.azyron.config.AzyronConfig;
import com.jiggneshgaming.azyron.core.config.AzyronConfigModel;
import com.jiggneshgaming.azyron.core.zoom.ZoomMath;
import com.jiggneshgaming.azyron.keybinds.KeyBinds;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT — zoom manager.
 *
 * <p>Zoom is applied as an FOV divisor. If the FOV mixin fails to apply
 * (different yarn signature), a fallback clamps the vanilla FOV option to
 * its minimum instead of crashing.</p>
 */
public final class ZoomManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/zoom");

    /** Set by {@code MixinGameRenderer} when it starts handling FOV. */
    public static volatile boolean fovMixinActive = false;

    private static float currentFovOffset = 1.0F; // 1.0 = not zoomed
    private static float smoothProgress = 0F;      // 0 = base fov, 1 = full zoom
    private static boolean fallbackApplied;
    private static int savedFov = -1;

    private ZoomManager() {
    }

    public static void tick() {
        AzyronConfigModel config = AzyronConfig.get();
        if (!config.zoomEnabled) {
            smoothProgress = approach(smoothProgress, 0F, 0.35F);
            currentFovOffset = ZoomMath.clampZoom((float) config.zoomLevel);
            restoreFallback();
            return;
        }

        boolean held = KeyBinds.zoomIsPressed();
        float step = config.smoothZoom ? ZoomMath.SMOOTH_STEP : ZoomMath.SNAPPY_STEP;
        if (held) {
            smoothProgress = approach(smoothProgress, 1F, step);
        } else {
            smoothProgress = approach(smoothProgress, 0F, step);
        }
        currentFovOffset = ZoomMath.clampZoom((float) config.zoomLevel);

        applyFallbackIfNeeded(held || smoothProgress > 0.01F);
    }

    /** Effective zoom multiplier right now (1 when fully zoomed out). */
    public static float effectiveZoom() {
        if (!AzyronConfig.get().zoomEnabled) {
            return 1.0F;
        }
        float target = ZoomMath.clampZoom((float) AzyronConfig.get().zoomLevel);
        float eased = easeOutCubic(smoothProgress);
        return 1.0F + (target - 1.0F) * eased;
    }

    /** Whether any zoom is currently applied (HUD indicator visibility). */
    public static boolean isZooming() {
        return AzyronConfig.get().zoomEnabled && smoothProgress > 0.05F;
    }

    /** FOV for the given base FOV, honoring the eased zoom. Called via mixin. */
    public static float computeFov(float baseFov) {
        float zoom = effectiveZoom();
        return ZoomMath.targetFov(baseFov, zoom);
    }

    private static float easeOutCubic(float t) {
        float c = Math.max(0F, Math.min(1F, t));
        float inv = 1.0F - c;
        return 1.0F - inv * inv * inv;
    }

    private static float approach(float current, float target, float step) {
        float delta = target - current;
        if (Math.abs(delta) <= 0.02F) {
            return target;
        }
        return current + Math.signum(delta) * Math.min(Math.abs(delta), step);
    }

    /* Fallback path — only when the FOV mixin did not apply. */

    private static void applyFallbackIfNeeded(boolean zooming) {
        if (fovMixinActive) {
            restoreFallback();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }
        try {
            if (zooming) {
                if (!fallbackApplied) {
                    savedFov = client.options.getFov().getValue().intValue();
                    fallbackApplied = true;
                }
                int min = 30; // vanilla FOV slider minimum
                client.options.getFov().setValue(Integer.valueOf(min));
            } else {
                restoreFallback();
            }
        } catch (Throwable t) {
            LOGGER.debug("FOV fallback failed: {}", t.toString());
        }
    }

    private static void restoreFallback() {
        if (!fallbackApplied) {
            return;
        }
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.options != null && savedFov >= 0) {
                client.options.getFov().setValue(Integer.valueOf(savedFov));
            }
        } catch (Throwable t) {
            LOGGER.debug("Could not restore FOV: {}", t.toString());
        } finally {
            fallbackApplied = false;
            savedFov = -1;
        }
    }
}
