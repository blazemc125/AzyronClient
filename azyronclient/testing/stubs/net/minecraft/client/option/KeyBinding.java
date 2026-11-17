package net.minecraft.client.option;
import net.minecraft.client.util.InputUtil;
public class KeyBinding {
    public KeyBinding(String translationKey, InputUtil.Type type, int code, String category) {}
    public boolean wasPressed() { return false; }
    public boolean isPressed() { return false; }
    public String getTranslationKey() { return ""; }
}
