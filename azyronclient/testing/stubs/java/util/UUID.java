package java.util;
public final class UUID {
    private UUID() {}
    public static UUID randomUUID() { return new UUID(); }
    public static UUID fromString(String name) { return new UUID(); }
    public String toString() { return null; }
    public long getMostSignificantBits() { return 0L; }
    public long getLeastSignificantBits() { return 0L; }
}
