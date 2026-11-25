package com.jiggneshgaming.azyron.compat;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cross-version GUI matrix-stack shim (shared by every profile).
 *
 * <p>{@code DrawContext.getMatrices()} changed return type and method set
 * between releases:</p>
 * <ul>
 *     <li>&le;1.21.5 — classic 3D {@code net.minecraft.client.util.math.MatrixStack}:
 *         {@code push()}/{@code pop()}, {@code translate(float,float,float)},
 *         {@code scale(float,float,float)}</li>
 *     <li>&ge;1.21.6 — JOML {@code org.joml.Matrix3x2fStack}:
 *         {@code pushMatrix()}/{@code popMatrix()}, {@code translate(float,float)},
 *         {@code scale(float,float)}</li>
 * </ul>
 * Because the 2D switch happened mid-line (1.21.6), a per-profile source set
 * cannot express it — the resolution is done reflectively at runtime, cached,
 * and never throws: a missing operation is logged once and skipped, so the
 * HUD/menu simply draws without that transform instead of crashing.</p>
 */
public final class MatrixHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("azyronclient/matrix");
    private static final Map<String, Method> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> WARNED = new ConcurrentHashMap<>();

    private MatrixHelper() {
    }

    public static void pushMatrix(Object matrices) {
        invoke(matrices, new String[]{"pushMatrix", "push"}, new Class<?>[0]);
    }

    public static void popMatrix(Object matrices) {
        invoke(matrices, new String[]{"popMatrix", "pop"}, new Class<?>[0]);
    }

    public static void translate(Object matrices, float x, float y) {
        invoke(matrices, new String[]{"translate", "translate"},
                new Class<?>[]{float.class, float.class}, new Class<?>[]{float.class, float.class, float.class},
                new Object[]{x, y}, new Object[]{x, y, 0.0F});
    }

    public static void scale(Object matrices, float x, float y) {
        invoke(matrices, new String[]{"scale", "scale"},
                new Class<?>[]{float.class, float.class}, new Class<?>[]{float.class, float.class, float.class},
                new Object[]{x, y}, new Object[]{x, y, 1.0F});
    }

    /* ---------------------------------------------------------- */

    private static void invoke(Object target, String[] names, Class<?>[] single) {
        invoke(target, names, single, single, new Object[0], new Object[0]);
    }

    /** Try the 2D signature first, then the 3D (z-padded) signature. */
    private static void invoke(Object target, String[] names,
                              Class<?>[] firstArgs, Class<?>[] secondArgs,
                              Object[] firstCall, Object[] secondCall) {
        if (target == null) {
            return;
        }
        Class<?> cls = target.getClass();
        for (int i = 0; i < names.length; i++) {
            Class<?>[] args = i == 0 ? firstArgs : secondArgs;
            Object[] call = i == 0 ? firstCall : secondCall;
            Method m = lookup(cls, names[i], args);
            if (m != null) {
                try {
                    m.invoke(target, call);
                } catch (Throwable t) {
                    warnOnce(cls.getName() + "#" + names[i], t);
                }
                return;
            }
        }
        warnOnce(cls.getName() + "#" + names[0], null);
    }

    private static Method lookup(Class<?> cls, String name, Class<?>[] args) {
        String key = cls.getName() + "#" + name + argsKey(args);
        Method cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            Method m = cls.getMethod(name, args);
            CACHE.put(key, m);
            return m;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static String argsKey(Class<?>[] args) {
        StringBuilder sb = new StringBuilder("(");
        for (Class<?> arg : args) {
            sb.append(arg.getSimpleName()).append(',');
        }
        return sb.append(')').toString();
    }

    private static void warnOnce(String key, Throwable t) {
        if (WARNED.putIfAbsent(key, Boolean.TRUE) == null) {
            if (t != null) {
                LOGGER.warn("Matrix operation unavailable on this version: {} — {}", key, t.toString());
            } else {
                LOGGER.warn("Matrix operation not found on {} — transforms will be skipped.", key);
            }
        }
    }
}
