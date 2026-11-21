package com.liskovsoft.smartyoutubetv2.common.hunys;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Volum egalizat: aduce toate clipurile la aproximativ aceeași tărie.<br/>
 * Loudness normalization: brings every video to roughly the same loudness.<br/>
 * YouTube trimite pentru fiecare clip „loudnessDb” = cu câți dB e mai tare decât nivelul de referință.
 * YouTube sends a per-video "loudnessDb" = how many dB louder it is than the reference level.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public final class HunysLoudness {
    /**
     * Rezervă: clipurile normale sunt coborâte cu 5 dB, ca cele încete să poată fi ridicate până la același nivel.<br/>
     * Headroom: normal videos are lowered by 5 dB so quiet ones can be raised to the same level.
     */
    private static final float HEADROOM_DB = -5f;
    private static final float MIN_GAIN_DB = -20f;
    private static final Map<Class<?>, Accessor> sAccessors = new ConcurrentHashMap<>();
    private static final Accessor NONE = new Accessor(null, null, false);

    private HunysLoudness() {
    }

    /**
     * Cum se citește valoarea pentru o anumită clasă / How to read the value for a given class
     */
    private static final class Accessor {
        final Field field;
        final Method method;
        final boolean isLazy; // kotlin.Lazy: valoarea se cere cu getValue() / value is read with getValue()

        Accessor(Field field, Method method, boolean isLazy) {
            this.field = field;
            this.method = method;
            this.isLazy = isLazy;
        }
    }

    /**
     * @return loudnessDb al clipului sau NaN dacă nu e cunoscut / the video's loudnessDb or NaN when unknown
     */
    public static float readLoudnessDb(Object formatInfo) {
        if (formatInfo == null) {
            return Float.NaN;
        }

        Class<?> clazz = formatInfo.getClass();
        Accessor accessor = sAccessors.get(clazz);

        if (accessor == null) {
            accessor = findAccessor(clazz);
            sAccessors.put(clazz, accessor);
        }

        if (accessor == NONE) {
            return Float.NaN;
        }

        try {
            Object value;

            if (accessor.method != null) {
                value = accessor.method.invoke(formatInfo);
            } else {
                value = accessor.field.get(formatInfo);
                if (accessor.isLazy && value != null) {
                    // Clasa nouă (Kotlin) ține valoarea într-un „by lazy” / The new (Kotlin) class keeps the value in a "by lazy"
                    Method getValue = value.getClass().getMethod("getValue");
                    getValue.setAccessible(true);
                    value = getValue.invoke(value);
                }
            }

            if (!(value instanceof Number)) {
                return Float.NaN;
            }

            float result = ((Number) value).floatValue();
            // 0 = transmisiune live sau lipsă date / live stream or missing data
            return result == 0 || Float.isNaN(result) || Float.isInfinite(result) ? Float.NaN : result;
        } catch (Throwable e) {
            return Float.NaN;
        }
    }

    private static Accessor findAccessor(Class<?> clazz) {
        // 1. Clasa veche (Java) / Old (Java) class
        Field field = findField(clazz, "mLoudnessDb");
        if (field != null) {
            return new Accessor(field, null, false);
        }

        // 2. Clasa nouă (Kotlin): „private val _loudnessDb by lazy” / New (Kotlin) class
        field = findField(clazz, "_loudnessDb$delegate");
        if (field != null) {
            return new Accessor(field, null, true);
        }

        // 3. O metodă getLoudnessDb() / A getLoudnessDb() method
        Method method = findMethod(clazz, "getLoudnessDb");
        if (method != null) {
            return new Accessor(null, method, false);
        }

        // 4. Orice câmp cu „loudness” în nume / Any field with "loudness" in its name
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!f.getName().toLowerCase(Locale.US).contains("loudness")) {
                    continue;
                }
                Class<?> type = f.getType();
                boolean isNumber = type == float.class || type == double.class || Number.class.isAssignableFrom(type);
                boolean isLazy = type.getName().equals("kotlin.Lazy");
                if (isNumber || isLazy) {
                    try {
                        f.setAccessible(true);
                        return new Accessor(f, null, isLazy);
                    } catch (Throwable e) {
                        // try next
                    }
                }
            }
        }

        return NONE;
    }

    /**
     * Multiplicatorul de volum pentru clip (0.1 - 1.0) / Volume multiplier for the video (0.1 - 1.0)
     */
    public static float gainFor(float loudnessDb) {
        float gainDb = Float.isNaN(loudnessDb) ? HEADROOM_DB : HEADROOM_DB - loudnessDb;
        gainDb = Math.max(MIN_GAIN_DB, Math.min(0f, gainDb));
        return (float) Math.pow(10.0, gainDb / 20.0);
    }

    private static Field findField(Class<?> clazz, String name) {
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException | SecurityException e) {
                clazz = clazz.getSuperclass();
            }
        }

        return null;
    }

    private static Method findMethod(Class<?> clazz, String name) {
        while (clazz != null && clazz != Object.class) {
            try {
                Method method = clazz.getDeclaredMethod(name);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException | SecurityException e) {
                clazz = clazz.getSuperclass();
            }
        }

        return null;
    }
}
