package java.awt;
import java.io.IOException;
import java.net.URI;
public class Desktop {
    public static boolean isDesktopSupported() { return false; }
    public static Desktop getDesktop() { return new Desktop(); }
    public void browse(URI uri) throws IOException {}
}
