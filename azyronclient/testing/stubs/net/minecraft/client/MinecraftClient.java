package net.minecraft.client;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.util.Session;
import net.minecraft.client.world.ClientWorld;
public class MinecraftClient {
    public static MinecraftClient getInstance() { return new MinecraftClient(); }
    public Window getWindow() { return new Window(); }
    public Session getSession() { return null; }
    public TextRenderer getTextRenderer() { return new TextRenderer(); }
    public int getCurrentFps() { return 60; }
    public GameOptions options = new GameOptions();
    public ClientPlayerEntity player = null;
    public ClientWorld world = null;
    public ParticleManager particleManager = null;
    public Screen currentScreen = null;
    public ClientPlayNetworkHandler getNetworkHandler() { return null; }
    public void setScreen(Screen screen) {}
    public void scheduleStop() {}
    public Screen getCurrentScreen() { return currentScreen; }
}
