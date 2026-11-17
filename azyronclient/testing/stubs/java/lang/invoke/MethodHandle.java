package java.lang.invoke;
public abstract class MethodHandle {
    MethodHandle() {}
    public MethodType type() { return null; }
    public Object invokeExact(Object... args) { return null; }
    public Object invoke(Object... args) { return null; }
}
