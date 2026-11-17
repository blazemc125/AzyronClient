package java.nio.file;
public interface Path {
    static Path of(String first, String... more) { return null; }
    static Path of(java.net.URI uri) { return null; }
    Path resolve(String other);
    Path resolveSibling(String other);
    Path getParent();
    Path getFileName();
    String toString();
}
