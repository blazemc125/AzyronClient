package java.lang.reflect;
public final class Method {
    private Method() {}
    public String getName() { return null; }
    public int getParameterCount() { return 0; }
    public Class<?>[] getParameterTypes() { return new Class<?>[0]; }
    public Class<?> getReturnType() { return null; }
    public void setAccessible(boolean flag) {}
    public boolean isAccessible() { return false; }
    public Object invoke(Object obj, Object... args) { return null; }
}
