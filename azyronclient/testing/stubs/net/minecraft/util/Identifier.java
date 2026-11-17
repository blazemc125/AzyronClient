package net.minecraft.util;
public final class Identifier {
    private Identifier() {}
    public static Identifier of(String namespace, String path) { return new Identifier(); }
    public static Identifier of(String id) { return new Identifier(); }
    public String getNamespace() { return "minecraft"; }
    public String getPath() { return ""; }
    public String toString() { return "minecraft:stub"; }
}
