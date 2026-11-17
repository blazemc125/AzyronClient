package net.minecraft.entity.player;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import java.util.Collection;
public class PlayerEntity {
    public double getX() { return 0D; }
    public double getY() { return 0D; }
    public double getZ() { return 0D; }
    public boolean isSpectator() { return false; }
    public boolean isSprinting() { return false; }
    public void setSprinting(boolean sprinting) {}
    public boolean isUsingItem() { return false; }
    public void sendMessage(Text message, boolean overlay) {}
    public net.minecraft.entity.player.HungerManager getHungerManager() { return new HungerManager(); }
    public net.minecraft.entity.player.Abilities getAbilities() { return new Abilities(); }
    public Iterable<net.minecraft.item.ItemStack> getArmorItems() { return java.util.Collections.emptyList(); }
    public Collection<StatusEffectInstance> getStatusEffects() { return java.util.Collections.emptyList(); }
}
