package com.jiggneshgaming.azyron.util;

import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * AZYRON CLIENT — resolve the local player UUID defensively across yarn
 * releases (Session.getUuid() has been both String and UUID depending on
 * the mappings build).
 */
public final class Sessions {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/session");
    private static boolean warned;

    private Sessions() {
    }

    public static UUID selfUuid() {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            // Session is private on MinecraftClient — read it reflectively.
            Object session = ReflectionUtil.getField(client, "session");
            if (session == null) {
                // Fallback: getSession() accessor if present.
                session = ReflectionUtil.invokeNoArg(client, "getSession");
            }
            if (session == null) {
                return warnOnce();
            }
            // 1) getUuidOrNull() -> UUID (1.20.2+ mappings)
            Object nullableUuid = ReflectionUtil.invokeNoArg(session, "getUuidOrNull");
            if (nullableUuid instanceof UUID) {
                return (UUID) nullableUuid;
            }
            // 2) getUuid() -> UUID or String depending on mappings
            Object raw = ReflectionUtil.invokeNoArg(session, "getUuid", "uuid");
            if (raw instanceof UUID) {
                return (UUID) raw;
            }
            if (raw instanceof String) {
                return parseUuid((String) raw);
            }
            // 3) getProfile().getId() (authlib GameProfile)
            Object profile = ReflectionUtil.invokeNoArg(session, "getProfile", "profile");
            if (profile != null) {
                Object id = ReflectionUtil.invokeNoArg(profile, "getId", "id");
                if (id instanceof UUID) {
                    return (UUID) id;
                }
                if (id instanceof String) {
                    return parseUuid((String) id);
                }
                // Newer authlib: GameProfile.id() record accessor
                Object recordId = ReflectionUtil.invokeNoArg(profile, "id");
                if (recordId instanceof UUID) {
                    return (UUID) recordId;
                }
            }
            return warnOnce();
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                LOGGER.warn("Session lookup failed — ping HUD will show a dash. ({})", t.toString());
            }
            return null;
        }
    }

    private static UUID parseUuid(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            // Undashed 32-char form
            String compact = raw.trim().replace("-", "");
            if (compact.length() == 32) {
                try {
                    return UUID.fromString(
                            compact.substring(0, 8) + "-" + compact.substring(8, 12) + "-"
                                    + compact.substring(12, 16) + "-" + compact.substring(16, 20) + "-"
                                    + compact.substring(20));
                } catch (IllegalArgumentException ignored) {
                    return warnOnce();
                }
            }
            return warnOnce();
        }
    }

    private static UUID warnOnce() {
        if (!warned) {
            warned = true;
            LOGGER.warn("Could not resolve the player UUID — ping HUD will show a dash.");
        }
        return null;
    }
}
