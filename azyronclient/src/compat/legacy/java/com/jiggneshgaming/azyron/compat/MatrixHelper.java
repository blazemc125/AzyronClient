package com.jiggneshgaming.azyron.compat;

import net.minecraft.client.util.math.MatrixStack;

/**
 * Legacy (&le;1.21.8) matrix-stack shim.
 *
 * <p>On these versions {@code DrawContext.getMatrices()} returns the classic 3D
 * {@code MatrixStack}: {@code push()}/{@code pop()} and 3-component
 * {@code translate(float,float,float)}/{@code scale(float,float,float)}. The 2D
 * overloads used by the modern profile do not exist. All 2D GUI transforms map
 * onto the z=0 plane with a uniform z-scale of 1.</p>
 */
public final class MatrixHelper {

    private MatrixHelper() {
    }

    public static void pushMatrix(MatrixStack matrices) {
        matrices.push();
    }

    public static void popMatrix(MatrixStack matrices) {
        matrices.pop();
    }

    public static void translate(MatrixStack matrices, float x, float y) {
        matrices.translate(x, y, 0.0F);
    }

    public static void scale(MatrixStack matrices, float x, float y) {
        matrices.scale(x, y, 1.0F);
    }
}
