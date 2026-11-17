package org.spongepowered.asm.mixin.injection.callback;
public class CallbackInfoReturnable<T> extends CallbackInfo {
    public CallbackInfoReturnable(String id, boolean cancellable) { super(id, cancellable); }
    public void setReturnValue(T value) {}
    public T getReturnValue() { return null; }
}
