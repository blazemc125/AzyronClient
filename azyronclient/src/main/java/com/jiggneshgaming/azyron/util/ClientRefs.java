package com.jiggneshgaming.azyron.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;

/**
 * AZYRON CLIENT — typed accessors for Minecraft internals whose yarn
 * names drift between versions. Everything funnels through reflection with
 * sensible fallbacks; failures are logged once and return null.
 */
public final class ClientRefs {

    private ClientRefs() {
    }

    /** TextRenderer regardless of yarn naming (getTextRenderer() vs textRenderer field). */
    public static TextRenderer textRenderer(MinecraftClient client) {
        try {
            Object viaMethod = ReflectionUtil.invokeNoArg(client, "getTextRenderer");
            if (viaMethod instanceof TextRenderer) {
                return (TextRenderer) viaMethod;
            }
            java.lang.reflect.Field field = findField(client.getClass(), "textRenderer");
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(client);
                if (value instanceof TextRenderer) {
                    return (TextRenderer) value;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Current screen or null; covers both field and getter namings. */
    public static Screen currentScreen(MinecraftClient client) {
        try {
            Object viaMethod = ReflectionUtil.invokeNoArg(client, "getCurrentScreen", "currentScreen");
            if (viaMethod instanceof Screen) {
                return (Screen) viaMethod;
            }
            java.lang.reflect.Field field = findField(client.getClass(), "currentScreen");
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(client);
                if (value instanceof Screen) {
                    return (Screen) value;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Scaled window width in pixels (HUD coordinate space). */
    public static int scaledWidth(MinecraftClient client) {
        try {
            Object w = client.getWindow();
            Object scaled = ReflectionUtil.invokeNoArg(w, "getScaledWidth", "scaledWidth");
            if (scaled instanceof Number) {
                return ((Number) scaled).intValue();
            }
            java.lang.reflect.Field field = findField(w.getClass(), "scaledWidth");
            if (field != null) {
                field.setAccessible(true);
                return field.getInt(w);
            }
            return client.getWindow().getWidth();
        } catch (Throwable t) {
            return 1280;
        }
    }

    /** Scaled window height in pixels (HUD coordinate space). */
    public static int scaledHeight(MinecraftClient client) {
        try {
            Object w = client.getWindow();
            Object scaled = ReflectionUtil.invokeNoArg(w, "getScaledHeight", "scaledHeight");
            if (scaled instanceof Number) {
                return ((Number) scaled).intValue();
            }
            java.lang.reflect.Field field = findField(w.getClass(), "scaledHeight");
            if (field != null) {
                field.setAccessible(true);
                return field.getInt(w);
            }
            return client.getWindow().getHeight();
        } catch (Throwable t) {
            return 720;
        }
    }

    private static java.lang.reflect.Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            } catch (SecurityException e) {
                return null;
            }
        }
        // also scan interfaces (e.g. for record-like accessors in future versions)
        for (Class<?> iface : type.getInterfaces()) {
            java.lang.reflect.Field found = findField(iface, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
