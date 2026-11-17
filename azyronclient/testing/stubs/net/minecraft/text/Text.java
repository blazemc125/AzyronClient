package net.minecraft.text;
public interface Text {
    String getString();
    static MutableText literal(String text) { return new MutableText(); }
    static MutableText translatable(String key, Object... args) { return new MutableText(); }
}
