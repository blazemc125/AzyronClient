package com.jiggneshgaming.azyron.util;

import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * AZYRON CLIENT — resolve the local player UUID defensively.
 *
 * <p>yarn renames the Session accessor between versions
 * (getUuid / getProfile().getId()); reflection keeps this working either way.</p>
 */
public final class Sessions {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/session");

    private Sessions() {
    }

    public static UUID selfUuid() {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            Object session = client.getSession();
            if (session == null) {
                return null;
            }
            // Primary: session.getUuid()
            Object uuid = ReflectionUtil.invokeNoArg(session, "getUuid", "uuid", "getUuidOrNull");
            if (uuid instanceof UUID) {
                return (UUID) uuid;
            }
            // Fallback: session.getProfile().getId()
            Object profile = ReflectionUtil.invokeNoArg(session, "getProfile", "profile");
            if (profile != null) {
                Object id = ReflectionUtil.invokeNoArg(profile, "getId", "id");
                if (id instanceof UUID) {
                    return (UUID) id;
                }
            }
            LOGGER.warn("Could not resolve the player UUID — ping HUD will show a dash.");
            return null;
        } catch (Throwable t) {
            LOGGER.debug("Session lookup failed: {}", t.toString());
            return null;
        }
    }
}
