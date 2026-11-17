package java.lang.reflect;
public final class Constructor<T> {
    private Constructor() {}
    public void setAccessible(boolean flag) {}
    public boolean isAccessible() { return false; }
    public T newInstance(Object... args) { return null; }
    public Class<?>[] getParameterTypes() { return new Class<?>[0]; }
    public String getName() { return null; }
}
