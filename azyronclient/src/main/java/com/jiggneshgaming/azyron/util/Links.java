package com.jiggneshgaming.azyron.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT — open external links (Discord invite, etc.) without
 * taking a compile-time dependency on Minecraft's Util class.
 */
public final class Links {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/links");

    private Links() {
    }

    public static void open(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        // 1) Minecraft's Util.getOperatingSystem().open(URI)
        try {
            Class<?> uriClass = Class.forName("java.net.URI");
            Object uri = uriClass.getMethod("create", String.class).invoke(null, url);
            Class<?> utilClass = Class.forName("net.minecraft.util.Util");
            Object os = utilClass.getMethod("getOperatingSystem").invoke(null);
            for (java.lang.reflect.Method method : os.getClass().getDeclaredMethods()) {
                if ("open".equals(method.getName()) && method.getParameterCount() == 1) {
                    method.setAccessible(true);
                    try {
                        method.invoke(os, uri);
                        return;
                    } catch (IllegalArgumentException ignored) {
                        // parameter type mismatch — try next overload
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Util.getOperatingSystem().open failed: {}", t.toString());
        }
        // 2) AWT Desktop fallback (desktop installs only)
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                return;
            }
        } catch (Throwable t) {
            LOGGER.warn("Could not open the link {}: {}", url, t.toString());
        }
    }
}
