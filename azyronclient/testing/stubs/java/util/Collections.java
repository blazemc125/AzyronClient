package java.util;
public final class Collections {
    private Collections() {}
    public static <T> List<T> emptyList() { return null; }
    public static <T> Set<T> emptySet() { return null; }
    public static <K, V> Map<K, V> emptyMap() { return null; }
    public static <T> List<T> singletonList(T o) { return null; }
    public static <T> List<T> unmodifiableList(List<? extends T> list) { return null; }
}
