package com.jiggneshgaming.azyron.compat;

import org.joml.Matrix3x2fStack;

/**
 * MC 1.21.9+ matrix-stack shim.
 *
 * <p>Since 1.21.9 {@code DrawContext.getMatrices()} returns a JOML {@link Matrix3x2fStack}
 * with 2D-only operations ({@code pushMatrix}/{@code popMatrix}/{@code translate(float,float)}).
 * Legacy profiles (&le;1.21.8) provide the same class shape from {@code src/compat/legacy}
 * backed by the classic 3D {@code MatrixStack}.</p>
 */
public final class MatrixHelper {

    private MatrixHelper() {
    }

    public static void pushMatrix(Matrix3x2fStack matrices) {
        matrices.pushMatrix();
    }

    public static void popMatrix(Matrix3x2fStack matrices) {
        matrices.popMatrix();
    }

    public static void translate(Matrix3x2fStack matrices, float x, float y) {
        matrices.translate(x, y);
    }

    public static void scale(Matrix3x2fStack matrices, float x, float y) {
        matrices.scale(x, y);
    }
}
