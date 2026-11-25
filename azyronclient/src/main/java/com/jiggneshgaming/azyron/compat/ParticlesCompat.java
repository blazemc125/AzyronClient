package com.jiggneshgaming.azyron.compat;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;

/**
 * Cross-version particles-option shim (shared by every profile).
 *
 * <p>The particles enum moved packages between releases:</p>
 * <ul>
 *     <li>&le;1.21.1 — {@code net.minecraft.client.option.ParticlesMode}</li>
 *     <li>&ge;1.21.2 — {@code net.minecraft.particle.ParticlesMode}</li>
 * </ul>
 * Both enums carry the same constants in the same yarn order
 * ({@code ALL}, {@code DECREASED}, {@code MINIMAL}) and
 * {@code GameOptions.getParticles()} exists with the same erased signature
 * everywhere, so a reflective lookup keeps the shared code compilable on the
 * whole 1.21 line.</p>
 */
public final class ParticlesCompat {

    /** UI labels, indexed by profile constant (PARTICLES_ALL/DECREASED/MINIMAL). */
    public static final String[] LABELS = {"ALL", "DECREASED", "MINIMAL"};

    private static volatile Class<?> enumClass;

    private ParticlesCompat() {
    }

    public static String label(int index) {
        return LABELS[Math.max(0, Math.min(LABELS.length - 1, index))];
    }

    /**
     * Set the particles option to the enum constant at {@code index}
     * (yarn order: 0=All, 1=Decreased, 2=Minimal).
     *
     * @return true when the option was found and written
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean setLevel(GameOptions options, int index) {
        if (options == null) {
            return false;
        }
        Class<?> cls = enumClass();
        if (cls == null) {
            return false;
        }
        try {
            Object[] values = (Object[]) cls.getMethod("values").invoke(null);
            if (values.length == 0) {
                return false;
            }
            int clamped = Math.max(0, Math.min(values.length - 1, index));
            SimpleOption option = options.getParticles();
            if (option == null) {
                return false;
            }
            option.setValue(values[clamped]);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Class<?> enumClass() {
        Class<?> cls = enumClass;
        if (cls == null) {
            for (String name : new String[]{
                    "net.minecraft.particle.ParticlesMode",
                    "net.minecraft.client.option.ParticlesMode"}) {
                try {
                    cls = Class.forName(name);
                    enumClass = cls;
                    break;
                } catch (Throwable ignored) {
                    // Try the other package.
                }
            }
        }
        return cls;
    }
}
