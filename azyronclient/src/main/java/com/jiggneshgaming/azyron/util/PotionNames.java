package com.jiggneshgaming.azyron.util;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

/**
 * AZYRON CLIENT — resolve a human-readable status effect name.
 *
 * <p>Yarn 1.21.11: StatusEffectInstance.getEffectType() returns
 * RegistryEntry&lt;StatusEffect&gt;; RegistryEntry.value() unwraps the
 * StatusEffect; StatusEffect.getName() returns a Text.</p>
 */
public final class PotionNames {

    private PotionNames() {
    }

    /** Display name: "Speed", "Strength"... falls back to "Effect". */
    public static String displayName(StatusEffectInstance instance) {
        try {
            RegistryEntry<StatusEffect> entry = instance.getEffectType();
            if (entry == null) {
                return "Effect";
            }
            StatusEffect effect = entry.value();
            if (effect == null) {
                return "Effect";
            }
            Text name = effect.getName();
            if (name != null) {
                String text = name.getString();
                if (text != null && !text.isEmpty()) {
                    return text;
                }
            }
            String key = effect.getTranslationKey();
            return prettify(key);
        } catch (Throwable t) {
            return "Effect";
        }
    }

    /** "effect.minecraft.speed" -> "Speed". */
    private static String prettify(String raw) {
        String key = raw == null ? "" : raw.trim();
        int lastDot = key.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < key.length() - 1) {
            key = key.substring(lastDot + 1);
        }
        key = key.replace('_', ' ').replace('-', ' ').trim();
        if (key.isEmpty()) {
            return "Effect";
        }
        StringBuilder out = new StringBuilder();
        for (String word : key.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.length() == 0 ? "Effect" : out.toString();
    }
}
