package com.liskovsoft.smartyoutubetv2.common.hunys;

import java.lang.reflect.Field;

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
    private static Field sLoudnessField;
    private static boolean sLookupFailed;

    private HunysLoudness() {
    }

    /**
     * @return loudnessDb al clipului sau NaN dacă nu e cunoscut / the video's loudnessDb or NaN when unknown
     */
    public static float readLoudnessDb(Object formatInfo) {
        if (formatInfo == null || sLookupFailed) {
            return Float.NaN;
        }

        try {
            if (sLoudnessField == null || !sLoudnessField.getDeclaringClass().isInstance(formatInfo)) {
                sLoudnessField = findField(formatInfo.getClass(), "mLoudnessDb");
            }

            if (sLoudnessField == null) {
                sLookupFailed = true;
                return Float.NaN;
            }

            float value = sLoudnessField.getFloat(formatInfo);
            // 0 = transmisiune live sau lipsă date / live stream or missing data
            return value == 0 ? Float.NaN : value;
        } catch (Exception e) {
            sLookupFailed = true;
            return Float.NaN;
        }
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
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }

        return null;
    }
}
