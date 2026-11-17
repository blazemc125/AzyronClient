package com.jiggneshgaming.azyron.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT — tiny reflection helper.
 *
 * <p>Used for integration points whose signatures drift between Minecraft /
 * Fabric versions (session access, optional game-option types, Util.open).
 * Everything here degrades gracefully: failures are logged, never thrown
 * into the game loop.</p>
 */
public final class ReflectionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/refl");

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
                return null;
            }
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Throwable t) {
            LOGGER.debug("Reflection call {} on {} failed: {}", method, target.getClass().getName(), t.toString());
            return null;
        }
    }

    private static java.lang.reflect.Method findMethod(Class<?> type, String name, Class<?>[] parameterTypes) {
        Class<?> current = type;
        while (current != null) {
            try {
                if (parameterTypes == null) {
                    for (java.lang.reflect.Method candidate : current.getDeclaredMethods()) {
                        if (candidate.getName().equals(name) && candidate.getParameterCount() == 0) {
                            return candidate;
                        }
                    }
                } else {
                    return current.getDeclaredMethod(name, parameterTypes);
                }
            } catch (NoSuchMethodException | SecurityException e) {
                // try the super-class / interface chain
            }
            current = current.getSuperclass();
        }
        return null;
    }
}
