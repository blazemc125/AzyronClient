package java.nio.file;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
public final class Files {
    private Files() {}
    public static boolean isRegularFile(Path path, java.nio.file.attribute.FileAttribute<?>... attrs) { return false; }
    public static Reader newBufferedReader(Path path, Charset charset) throws IOException { return null; }
    public static Writer newBufferedWriter(Path path, Charset charset, java.nio.file.OpenOption... options) throws IOException { return null; }
    public static void createDirectories(Path path, java.nio.file.attribute.FileAttribute<?>... attrs) throws IOException {}
    public static Path move(Path source, Path target, CopyOption... options) throws IOException { return null; }
    public static Path copy(Path source, Path target, CopyOption... options) throws IOException { return null; }
    public static boolean deleteIfExists(Path path) throws IOException { return false; }
}
