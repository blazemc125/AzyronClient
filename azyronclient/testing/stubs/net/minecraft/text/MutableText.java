package net.minecraft.text;
public final class MutableText implements Text {
    public String getString() { return ""; }
    public MutableText append(Text other) { return this; }
}
