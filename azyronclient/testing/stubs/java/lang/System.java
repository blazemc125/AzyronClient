package java.lang;
import java.io.PrintStream;
public final class System {
    private System() {}
    public static PrintStream out = new PrintStream();
    public static PrintStream err = new PrintStream();
    public static void exit(int status) {}
    public static long currentTimeMillis() { return 0L; }
    public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {}
}
