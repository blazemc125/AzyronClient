package com.jiggneshgaming.azyron.util;

import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * AZYRON CLIENT — open external links (Discord invite, etc.).
 *
 * <p>Util.getOperatingSystem().open(URI) is the vanilla, cross-platform
 * path used by the game itself; a desktop fallback covers environments
 * where Util is unavailable.</p>
 */
public final class Links {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/links");

    private Links() {
    }

    public static void open(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        final URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Refusing to open malformed link '{}': {}", url, e.toString());
            return;
        }
        // 1) Vanilla path — Util.OperatingSystem.open(URI)
        try {
            Util.getOperatingSystem().open(uri);
            return;
        } catch (Throwable t) {
            LOGGER.debug("Util.getOperatingSystem().open(URI) failed: {}", t.toString());
        }
        // 2) Desktop fallback (desktop installs only)
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(uri);
                return;
            }
        } catch (Throwable t) {
            LOGGER.warn("Could not open the link {}: {}", url, t.toString());
        }
    }
}
