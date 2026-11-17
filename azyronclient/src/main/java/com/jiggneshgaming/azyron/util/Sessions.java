package com.jiggneshgaming.azyron.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * AZYRON CLIENT — resolve the local player UUID (cached).
 *
 * <p>Direct compile-time access: the {@code session} field is opened by the
 * access widener and {@code getUuidOrNull()} is a public yarn-1.21.11
 * method. Cached so the ping HUD does zero work per frame.</p>
 */
public final class Sessions {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/session");

    private static UUID cached;
    private static boolean resolved;
    private static boolean warned;

    private Sessions() {
    }

    public static UUID selfUuid() {
        if (resolved) {
            return cached;
        }
        resolved = true; // one attempt — no per-frame retry churn
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            Session session = client != null ? client.session : null;
            if (session != null) {
                UUID id = session.getUuidOrNull();
                if (id != null) {
                    cached = id;
                    return cached;
                }
            }
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                LOGGER.warn("Could not resolve the player UUID — ping HUD will show a dash. ({})", t.toString());
            }
        }
        return null;
    }

    /** Test hook / re-resolve after re-login. */
    public static void reset() {
        cached = null;
        resolved = false;
    }
}
