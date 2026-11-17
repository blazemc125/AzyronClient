package org.lwjgl.glfw;
public final class GLFW {
    private GLFW() {}
    public static final int GLFW_MOUSE_BUTTON_LEFT = 0;
    public static final int GLFW_MOUSE_BUTTON_RIGHT = 1;
    public static final int GLFW_MOUSE_BUTTON_MIDDLE = 2;
    public static final int GLFW_PRESS = 1;
    public static final int GLFW_RELEASE = 0;
    public static final int GLFW_KEY_W = 87;
    public static final int GLFW_KEY_A = 65;
    public static final int GLFW_KEY_S = 83;
    public static final int GLFW_KEY_D = 68;
    public static final int GLFW_KEY_R = 82;
    public static final int GLFW_KEY_C = 67;
    public static final int GLFW_KEY_H = 72;
    public static final int GLFW_KEY_SPACE = 32;
    public static final int GLFW_KEY_LEFT_SHIFT = 340;
    public static final int GLFW_KEY_LEFT_CONTROL = 341;
    public static final int GLFW_KEY_RIGHT_SHIFT = 344;
    public static final int GLFW_KEY_ESCAPE = 256;
    public static int glfwGetMouseButton(long window, int button) { return 0; }
    public static int glfwGetKey(long window, int key) { return 0; }
}
