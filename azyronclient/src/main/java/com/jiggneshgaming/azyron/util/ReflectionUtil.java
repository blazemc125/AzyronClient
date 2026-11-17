package com.jiggneshgaming.azyron.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT — tiny reflection helper.
 *
 * <p>Used for integration points whose visibility or names drift between
 * Minecraft / Fabric versions (package-private textRenderer, private
 * session, optional game-option types, Util.open). Failures are logged
 * once per kind, never thrown into the game loop.</p>
 */
public final class ReflectionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/refl");
    private static final java.util.Set<String> REPORTED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private ReflectionUtil() {
    }

    public static Object invokeNoArg(Object target, String... methodCandidates) {
        for (String method : methodCandidates) {
            Object result = invoke(target, method);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /** Read a (possibly package-private/private) field anywhere in the class hierarchy. */
    public static Object getField(Object target, String fieldName) {
        if (target == null || fieldName == null) {
            return null;
        }
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                java.lang.reflect.Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            } catch (Throwable t) {
                logOnce(fieldName, target.getClass().getName(), t);
                return null;
            }
        }
        logOnce(fieldName, target.getClass().getName(),
                new NoSuchFieldException("field '" + fieldName + "' not found in hierarchy"));
        return null;
    }

    /** Convenience overload — no parameter types, no arguments. */
    public static Object invoke(Object target, String method) {
        return invoke(target, method, (Class<?>[]) null, new Object[0]);
    }

    public static Object invoke(Object target, String method, Class<?>[] parameterTypes, Object... args) {
        if (target == null || method == null) {
            return null;
        }
        try {
            java.lang.reflect.Method m = findMethod(target.getClass(), method, parameterTypes);
            if (m == null) {
                logOnce(method, target.getClass().getName(),
                        new NoSuchMethodException("'" + method + "' not found in hierarchy"));
                return null;
            }
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Throwable t) {
            logOnce(method, target.getClass().getName(), t);
            return null;
        }
    }

    private static void logOnce(String member, String owner, Throwable t) {
        if (REPORTED.add(member + "@" + owner)) {
            LOGGER.warn("Optional integration '{}' on {} unavailable: {}", member, owner, t.toString());
        } else {
            LOGGER.debug("Integration '{}' on {} unavailable: {}", member, owner, t.toString());
        }
    }

    private static java.lang.reflect.Method findMethod(Class<?> type, String name, Class<?>[] parameterTypes) {
        Class<?> current = type;
        while (current != null) {
            try {
                if (parameterTypes == null) {
                    for (java.lang.reflect.Method candidate : current.getDeclaredMethods()) {
                        if (candidate.getName().equals(name) && candidate.getParameterCount() == 0) {
                            candidate.setAccessible(true);
                            return candidate;
                        }
                    }
                } else {
                    java.lang.reflect.Method found = current.getDeclaredMethod(name, parameterTypes);
                    found.setAccessible(true);
                    return found;
                }
            } catch (NoSuchMethodException | SecurityException e) {
                // try the super-class chain
            }
            current = current.getSuperclass();
        }
        return null;
    }
}
