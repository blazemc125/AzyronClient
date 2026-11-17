package net.minecraft.client.gui;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
public class DrawContext {
    public void fill(int x1, int y1, int x2, int y2, int color) {}
    public int drawText(TextRenderer textRenderer, Text text, int x, int y, int color, boolean shadow) { return x; }
    public int drawTextWithShadow(TextRenderer textRenderer, Text text, int x, int y, int color) { return x; }
    public int drawTextWithShadow(TextRenderer textRenderer, String text, int x, int y, int color) { return x; }
    public MatrixStack getMatrices() { return new MatrixStack(); }
    public void drawItem(ItemStack stack, int x, int y) {}
    public int getScaledWindowWidth() { return 854; }
    public int getScaledWindowHeight() { return 480; }
}
