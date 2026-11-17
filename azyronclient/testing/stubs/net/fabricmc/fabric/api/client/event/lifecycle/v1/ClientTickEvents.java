package net.fabricmc.fabric.api.client.event.lifecycle.v1;
import net.minecraft.client.MinecraftClient;
public final class ClientTickEvents {
    private ClientTickEvents() {}
    public interface EndTick {
        void onEndTick(MinecraftClient client);
    }
    public static final Event<EndTick> END_CLIENT_TICK = new Event<EndTick>();
    public static class Event<T> {
        public void register(T listener) {}
    }
}
