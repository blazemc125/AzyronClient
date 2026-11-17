package java.lang.invoke;
public final class MethodType {
    private MethodType() {}
    public static MethodType methodType(Class<?> returnType) { return new MethodType(); }
    public static MethodType methodType(Class<?> returnType, Class<?>... parameterTypes) { return new MethodType(); }
    public Class<?> returnType() { return null; }
    public Class<?> parameterType(int index) { return null; }
    public int parameterCount() { return 0; }
}
