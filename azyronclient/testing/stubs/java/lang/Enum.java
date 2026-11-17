package java.lang;
public abstract class Enum<E extends Enum<E>> implements Comparable<E> {
    private final String name;
    private final int ordinal;
    protected Enum(String name, int ordinal) { this.name = name; this.ordinal = ordinal; }
    public final String name() { return name; }
    public final int ordinal() { return ordinal; }
    public String toString() { return name; }
    public final boolean equals(Object other) { return this == other; }
    public final int hashCode() { return ordinal; }
    public final int compareTo(E o) { return ordinal - o.ordinal(); }
    public static <T extends Enum<T>> T valueOf(Class<T> enumType, String name) { return null; }
}
