package java.lang;
public class RuntimeException extends Exception {
    public RuntimeException() {}
    public RuntimeException(String message) { super(message); }
    public RuntimeException(String message, Throwable cause) { super(message, cause); }
}
