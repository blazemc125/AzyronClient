package org.spongepowered.asm.mixin.injection.callback;
public class CallbackInfo {
    public CallbackInfo(String id, boolean cancellable) {}
    public void cancel() {}
    public boolean isCancelled() { return false; }
}
