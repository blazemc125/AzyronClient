package java.lang;
public class Throwable {
    public Throwable() {}
    public Throwable(String message) {}
    public Throwable(String message, Throwable cause) {}
    public String getMessage() { return null; }
    public String getLocalizedMessage() { return null; }
    public Throwable getCause() { return null; }
    public void printStackTrace() {}
    public StackTraceElement[] getStackTrace() { return null; }
    public void addSuppressed(Throwable exception) {}
    public Throwable[] getSuppressed() { return new Throwable[0]; }
    public String toString() { return null; }
    public void fillInStackTrace() {}
}
