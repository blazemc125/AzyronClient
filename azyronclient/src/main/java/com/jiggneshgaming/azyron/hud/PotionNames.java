package com.jiggneshgaming.azyron.hud;

import com.jiggneshgaming.azyron.util.ReflectionUtil;
import net.minecraft.entity.effect.StatusEffectInstance;

/**
 * AZYRON CLIENT — resolve a human-readable status effect name without a
 * compile-time dependency on the registry entry internals.
 */
public final class PotionNames {

    private PotionNames() {
    }

    /** Best-effort display name: "Speed", "Strength"... falls back to "Effect". */
    public static String displayName(StatusEffectInstance instance) {
        try {
            Object effectType = ReflectionUtil.invokeNoArg(instance, "getEffectType");
            if (effectType == null) {
                return "Effect";
            }
            // RegistryEntry wrapper — unwrap with value().
            Object value = ReflectionUtil.invokeNoArg(effectType, "value", "getValue", "rawValue");
            if (value == null) {
                value = effectType;
            }
            // String-returning candidates on StatusEffect.
            for (String method : new String[]{"getTranslationKey", "getName", "getDescription", "getEffectTypeName"}) {
                Object result = ReflectionUtil.invokeNoArg(value, method);
                String text = stringify(result);
                if (text != null && text.length() > 0 && !text.startsWith("container.")) {
                    return prettify(text);
                }
            }
        } catch (Throwable ignored) {
        }
        return "Effect";
    }

    private static String stringify(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof CharSequence) {
            return o.toString();
        }
        // Text/Component-like objects — try getString()/getText().
        Object text = ReflectionUtil.invokeNoArg(o, "getString", "getText", "getPlainText", "asString");
        if (text instanceof CharSequence) {
            return text.toString();
        }
        return o.toString();
    }

    /** "effect.minecraft.speed" -> "Speed". */
    private static String prettify(String raw) {
        String key = raw.trim();
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
        return out.toString();
    }
}
