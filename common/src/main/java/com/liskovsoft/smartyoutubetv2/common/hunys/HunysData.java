package com.liskovsoft.smartyoutubetv2.common.hunys;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Setările modului Hunys / Hunys mod settings.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public class HunysData {
    private static final String PREFS_NAME = "hunys_prefs";

    public static final float FONT_SCALE_DEFAULT = 1.12f;
    public static final int DIM_LEVEL_MAX = 80;
    public static final int DIM_LEVEL_STEP = 10;

    private static final String K_FONT_SCALE = "font_scale";
    private static final String K_DIM_LEVEL = "dim_level";
    private static final String K_AUTO_DIM = "auto_dim";
    private static final String K_AUTO_DIM_START = "auto_dim_start";
    private static final String K_AUTO_DIM_END = "auto_dim_end";
    private static final String K_AUTO_DIM_LEVEL = "auto_dim_level";
    private static final String K_DIM_MANUAL_UNTIL = "dim_manual_until";
    private static final String K_LOUDNESS = "loudness";
    private static final String K_PREVIEW_SECONDS = "preview_seconds";
    private static final String K_PREVIEW_MUTED = "preview_muted";
    private static final String K_LIGHT_MODE = "light_mode";
    private static final String K_DATA_LIMIT_MB = "data_limit_mb";
    private static final String K_DATA_WARN = "data_warn";
    private static final String K_DEFAULTS_APPLIED = "defaults_applied_v1";
    private static final String K_START_QUALITY = "start_quality";
    private static final String K_MENU_ORDER_APPLIED = "menu_order_v1";
    private static final String K_PLAYER_DEFAULTS_APPLIED = "player_defaults_v1";
    public static final int START_QUALITY_DEFAULT = 720;

    @SuppressLint("StaticFieldLeak")
    private static HunysData sInstance;
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final List<Runnable> mListeners = new ArrayList<>();

    private HunysData(Context context) {
        mContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        mPrefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static HunysData instance(Context context) {
        if (sInstance == null) {
            sInstance = new HunysData(context);
        }

        return sInstance;
    }

    public static boolean isHunysBuild(Context context) {
        return context != null && context.getPackageName() != null && context.getPackageName().endsWith(".hunys");
    }

    public void addListener(Runnable listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void removeListener(Runnable listener) {
        mListeners.remove(listener);
    }

    private void notifyChanged() {
        for (Runnable listener : new ArrayList<>(mListeners)) {
            listener.run();
        }
    }

    // ---- Defaults applied once / setări implicite aplicate o singură dată

    public boolean isDefaultsApplied() {
        return mPrefs.getBoolean(K_DEFAULTS_APPLIED, false);
    }

    public void setDefaultsApplied() {
        mPrefs.edit().putBoolean(K_DEFAULTS_APPLIED, true).apply();
    }

    // ---- Calitate la pornire / start quality (480, 720, 1080)

    public int getStartQuality() {
        return mPrefs.getInt(K_START_QUALITY, START_QUALITY_DEFAULT);
    }

    public void setStartQuality(int height) {
        mPrefs.edit().putInt(K_START_QUALITY, height).apply();
        notifyChanged();
    }

    // ---- Meniul de pe clip: „Nu mă interesează” primele / video menu: "Not interested" first

    public boolean isMenuOrderApplied() {
        return mPrefs.getBoolean(K_MENU_ORDER_APPLIED, false);
    }

    public void setMenuOrderApplied() {
        mPrefs.edit().putBoolean(K_MENU_ORDER_APPLIED, true).apply();
    }

    // ---- Player: derulare cu confirmare, fără denumiri la butoane (o singură dată)
    // ---- Player: seek with confirmation, no button labels (once)

    public boolean isPlayerDefaultsApplied() {
        return mPrefs.getBoolean(K_PLAYER_DEFAULTS_APPLIED, false);
    }

    public void setPlayerDefaultsApplied() {
        mPrefs.edit().putBoolean(K_PLAYER_DEFAULTS_APPLIED, true).apply();
    }

    // ---- Text / scris

    public float getFontScale() {
        return mPrefs.getFloat(K_FONT_SCALE, FONT_SCALE_DEFAULT);
    }

    public void setFontScale(float scale) {
        mPrefs.edit().putFloat(K_FONT_SCALE, scale).apply();
        notifyChanged();
    }

    // ---- Întunecare ecran / screen dimming

    /** Nivelul ales manual (0-80%) / Manually chosen level (0-80%) */
    public int getDimLevel() {
        return clampDim(mPrefs.getInt(K_DIM_LEVEL, 0));
    }

    public void setDimLevel(int level) {
        mPrefs.edit().putInt(K_DIM_LEVEL, clampDim(level)).apply();
        notifyChanged();
    }

    public boolean isAutoDimEnabled() {
        return mPrefs.getBoolean(K_AUTO_DIM, false);
    }

    public void setAutoDimEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(K_AUTO_DIM, enabled).apply();
        notifyChanged();
    }

    /** Minute de la miezul nopții / Minutes from midnight */
    public int getAutoDimStartMin() {
        return mPrefs.getInt(K_AUTO_DIM_START, 22 * 60);
    }

    public void setAutoDimStartMin(int minutes) {
        mPrefs.edit().putInt(K_AUTO_DIM_START, normalizeMinutes(minutes)).apply();
        notifyChanged();
    }

    public int getAutoDimEndMin() {
        return mPrefs.getInt(K_AUTO_DIM_END, 7 * 60);
    }

    public void setAutoDimEndMin(int minutes) {
        mPrefs.edit().putInt(K_AUTO_DIM_END, normalizeMinutes(minutes)).apply();
        notifyChanged();
    }

    public int getAutoDimLevel() {
        return clampDim(mPrefs.getInt(K_AUTO_DIM_LEVEL, 50));
    }

    public void setAutoDimLevel(int level) {
        mPrefs.edit().putInt(K_AUTO_DIM_LEVEL, clampDim(level)).apply();
        notifyChanged();
    }

    /**
     * Dacă utilizatorul schimbă nivelul noaptea, îl păstrăm până se termină intervalul.<br/>
     * If the user changes the level at night, keep it until the interval ends.
     */
    public long getDimManualUntil() {
        return mPrefs.getLong(K_DIM_MANUAL_UNTIL, 0);
    }

    public void setDimManualUntil(long timeMs) {
        mPrefs.edit().putLong(K_DIM_MANUAL_UNTIL, timeMs).apply();
    }

    public static int clampDim(int level) {
        level = Math.round(level / (float) DIM_LEVEL_STEP) * DIM_LEVEL_STEP;
        return Math.max(0, Math.min(DIM_LEVEL_MAX, level));
    }

    private static int normalizeMinutes(int minutes) {
        int day = 24 * 60;
        return ((minutes % day) + day) % day;
    }

    // ---- Volum egalizat / loudness normalization

    public boolean isLoudnessEnabled() {
        return mPrefs.getBoolean(K_LOUDNESS, true);
    }

    public void setLoudnessEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(K_LOUDNESS, enabled).apply();
        notifyChanged();
    }

    // ---- Previzualizare / card preview

    public int getPreviewSeconds() {
        return mPrefs.getInt(K_PREVIEW_SECONDS, 60);
    }

    public void setPreviewSeconds(int seconds) {
        mPrefs.edit().putInt(K_PREVIEW_SECONDS, seconds).apply();
        notifyChanged();
    }

    public boolean isPreviewMuted() {
        return mPrefs.getBoolean(K_PREVIEW_MUTED, false);
    }

    public void setPreviewMuted(boolean muted) {
        mPrefs.edit().putBoolean(K_PREVIEW_MUTED, muted).apply();
        notifyChanged();
    }

    // ---- Mod ușor / light mode

    public boolean isLightModeEnabled() {
        return mPrefs.getBoolean(K_LIGHT_MODE, false);
    }

    public void setLightModeEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(K_LIGHT_MODE, enabled).apply();
        notifyChanged();
    }

    // ---- Contor date / data counter

    public int getDataLimitMb() {
        return mPrefs.getInt(K_DATA_LIMIT_MB, 15_000);
    }

    public void setDataLimitMb(int limitMb) {
        mPrefs.edit().putInt(K_DATA_LIMIT_MB, limitMb).apply();
        notifyChanged();
    }

    public boolean isDataWarnEnabled() {
        return mPrefs.getBoolean(K_DATA_WARN, true);
    }

    public void setDataWarnEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(K_DATA_WARN, enabled).apply();
        notifyChanged();
    }

    SharedPreferences getPrefs() {
        return mPrefs;
    }

    Context getContext() {
        return mContext;
    }
}
