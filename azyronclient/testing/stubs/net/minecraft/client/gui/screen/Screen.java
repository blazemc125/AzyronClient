package net.minecraft.client.gui.screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
public abstract class Screen {
    protected int width;
    protected int height;
    protected final Text title;
    protected Screen(Text title) { this.title = title; }
    protected void init() {}
    public void init(int width, int height) { this.width = width; this.height = height; init(); }
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {}
    public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) { return false; }
    public boolean mouseReleased(double mouseX, double mouseY, int button) { return false; }
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) { return false; }
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
    public boolean charTyped(char chr, int modifiers) { return false; }
    public void removed() {}
    public boolean shouldCloseOnEsc() { return true; }
    public boolean shouldPause() { return true; }
    public void close() {}
    public Text getTitle() { return title; }
}
