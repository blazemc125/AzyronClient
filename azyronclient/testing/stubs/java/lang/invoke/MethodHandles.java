package java.lang.invoke;
public final class MethodHandles {
    private MethodHandles() {}
    public static final class Lookup {
        public MethodHandle findStatic(Class<?> refc, String name, MethodType type) { return null; }
        public MethodHandle findVirtual(Class<?> refc, String name, MethodType type) { return null; }
        public MethodHandle findSpecial(Class<?> refc, String name, MethodType type, Class<?> specialCaller) { return null; }
    }
    public static Lookup lookup() { return new Lookup(); }
    public static Lookup publicLookup() { return new Lookup(); }
}
