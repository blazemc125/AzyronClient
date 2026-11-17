package net.minecraft.client.font;
import net.minecraft.text.Text;
public class TextRenderer {
    public int getWidth(String text) { return text == null ? 0 : text.length() * 6; }
    public int getWidth(Text text) { return text == null ? 0 : getWidth(text.getString()); }
    public int getHeight() { return 9; }
}
