package java.lang.invoke;
public abstract class CallSite {
    CallSite(MethodHandle target) {}
    public MethodHandle getTarget() { return null; }
    public void setTarget(MethodHandle newTarget) {}
}
