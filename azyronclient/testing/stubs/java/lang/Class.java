package java.lang;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
public final class Class<T> {
    private Class() {}
    public static Class<?> forName(String className) throws ClassNotFoundException { return null; }
    public String getName() { return null; }
    public String getSimpleName() { return null; }
    public boolean isInstance(Object obj) { return false; }
    public <U> Class<U> asSubclass(Class<U> clazz) { return null; }
    public Class<? super T> getSuperclass() { return null; }
    public Class<?>[] getInterfaces() { return new Class<?>[0]; }
    public Method[] getDeclaredMethods() { return new Method[0]; }
    public Method getDeclaredMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException { return null; }
    public Method getMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException { return null; }
    public Field getDeclaredField(String name) throws NoSuchFieldException { return null; }
    public Field[] getDeclaredFields() { return new Field[0]; }
    public <C> Constructor<C> getDeclaredConstructor(Class<?>... parameterTypes) throws NoSuchMethodException { return null; }
    public <C> Constructor<C> getConstructor(Class<?>... parameterTypes) throws NoSuchMethodException { return null; }
    public Object[] getEnumConstants() { return new Object[0]; }
    public boolean isEnum() { return false; }
    public boolean isInterface() { return false; }
    public boolean isPrimitive() { return false; }
    public String getCanonicalName() { return null; }
}
