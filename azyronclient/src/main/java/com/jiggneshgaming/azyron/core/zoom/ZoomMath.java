package com.jiggneshgaming.azyron.core.zoom;

/**
 * AZYRON CLIENT — smooth zoom math.
 *
 * <p>Pure functions implementing eased FOV zoom. Unit-tested standalone.</p>
 */
public final class ZoomMath {

    private ZoomMath() {
    }

    public static final float MIN_ZOOM = 1.0F;
    public static final float MAX_ZOOM = 12.0F;

    /** Clamp a zoom multiplier into [MIN_ZOOM, MAX_ZOOM]. */
    public static float clampZoom(float zoom) {
        if (Float.isNaN(zoom)) {
            return MIN_ZOOM;
        }
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
    }

    /** Target FOV (degrees) for the given base FOV and zoom multiplier. Never below 1°. */
    public static float targetFov(float baseFovDegrees, float zoom) {
        float z = clampZoom(zoom);
        float fov = baseFovDegrees / z;
        return Math.max(1.0F, fov);
    }

    /**
     * Move {@code current} toward {@code target} by a fraction of the remaining distance.
     *
     * <p>{@code fraction} in (0, 1]; 0.35 gives a fast-but-smooth ease-out per tick.
     * Snaps to target when the remaining delta is below {@code epsilon} to avoid jitter.</p>
     */
    public static float lerp(float current, float target, float fraction, float epsilon) {
        float delta = target - current;
        if (Math.abs(delta) <= epsilon) {
            return target;
        }
        float f = Math.max(0.0F, Math.min(1.0F, fraction));
        return current + delta * f;
    }

    /** Recommended per-tick interpolation factor for smooth zoom. */
    public static final float SMOOTH_STEP = 0.32F;

    /** Per-tick interpolation factor for the "instant but eased" non-smooth mode. */
    public static final float SNAPPY_STEP = 0.65F;
}
