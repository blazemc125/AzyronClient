package net.fabricmc.fabric.api.client.screen.v1;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
public final class ScreenEvents {
    private ScreenEvents() {}
    public interface AfterInit {
        void afterInit(MinecraftClient client, Screen screen, int scaledWidth, int scaledHeight);
    }
    public static final Event<AfterInit> AFTER_INIT = new Event<AfterInit>();
    public static class Event<T> {
        public void register(T listener) {}
    }
}
