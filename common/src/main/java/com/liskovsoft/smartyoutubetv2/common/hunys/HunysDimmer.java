package com.liskovsoft.smartyoutubetv2.common.hunys;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;
import java.util.Calendar;

/**
 * Întunecă ecranul în toată aplicația (0-80%), manual sau automat după oră.<br/>
 * Dims the screen across the whole app (0-80%), manually or on a schedule.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public class HunysDimmer {
    private static final long TICK_MS = 60_000;
    @SuppressLint("StaticFieldLeak")
    private static HunysDimmer sInstance;
    private final HunysData mData;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private WeakReference<Activity> mActivity;
    private View mOverlay;
    private final Runnable mTick = this::tick;
    private final Runnable mDataListener = this::update;

    private HunysDimmer(Context context) {
        mData = HunysData.instance(context);
    }

    public static HunysDimmer instance(Context context) {
        if (sInstance == null) {
            sInstance = new HunysDimmer(context);
        }

        return sInstance;
    }

    public void attach(Activity activity) {
        if (activity == null || !HunysData.isHunysBuild(activity) || isDialogActivity(activity)) {
            return;
        }

        removeOverlay();
        mActivity = new WeakReference<>(activity);
        mData.addListener(mDataListener);
        update();
        mHandler.removeCallbacks(mTick);
        mHandler.postDelayed(mTick, TICK_MS);
    }

    public void detach(Activity activity) {
        if (mActivity != null && mActivity.get() == activity) {
            removeOverlay();
            mActivity = null;
            mHandler.removeCallbacks(mTick);
            mData.removeListener(mDataListener);
        }
    }

    /**
     * Nivel ales din meniul cu lună. Noaptea, alegerea manuală e păstrată până dimineață.<br/>
     * Level chosen from the moon menu. At night, the manual choice is kept until morning.
     */
    public void setLevelByUser(int level) {
        mData.setDimLevel(level);

        if (isAutoIntervalNow()) {
            mData.setDimManualUntil(getIntervalEndMs());
        } else {
            mData.setDimManualUntil(0);
        }

        update();
    }

    public int getEffectiveLevel() {
        if (isAutoIntervalNow() && System.currentTimeMillis() >= mData.getDimManualUntil()) {
            return mData.getAutoDimLevel();
        }

        return mData.getDimLevel();
    }

    public boolean isAutoIntervalNow() {
        if (!mData.isAutoDimEnabled()) {
            return false;
        }

        int now = minutesNow();
        int start = mData.getAutoDimStartMin();
        int end = mData.getAutoDimEndMin();

        if (start == end) {
            return false;
        }

        // Ex: 22:00 → 07:00 trece peste miezul nopții / crosses midnight
        return start < end ? (now >= start && now < end) : (now >= start || now < end);
    }

    private long getIntervalEndMs() {
        Calendar cal = Calendar.getInstance();
        int end = mData.getAutoDimEndMin();
        int now = minutesNow();
        cal.set(Calendar.HOUR_OF_DAY, end / 60);
        cal.set(Calendar.MINUTE, end % 60);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (end <= now) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return cal.getTimeInMillis();
    }

    private static int minutesNow() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    }

    private void tick() {
        update();
        mHandler.postDelayed(mTick, TICK_MS);
    }

    private void update() {
        Activity activity = mActivity != null ? mActivity.get() : null;

        if (activity == null || activity.getWindow() == null) {
            return;
        }

        int level = getEffectiveLevel();

        if (level <= 0) {
            removeOverlay();
            return;
        }

        View decor = activity.getWindow().getDecorView();

        if (!(decor instanceof ViewGroup)) {
            return;
        }

        ViewGroup root = (ViewGroup) decor;

        if (mOverlay == null || mOverlay.getParent() != root) {
            removeOverlay();
            mOverlay = new View(activity);
            mOverlay.setBackgroundColor(Color.BLACK);
            mOverlay.setClickable(false);
            mOverlay.setFocusable(false);
            if (Build.VERSION.SDK_INT >= 16) {
                mOverlay.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            }
            root.addView(mOverlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        mOverlay.setAlpha(level / 100f);
        mOverlay.bringToFront();
    }

    private void removeOverlay() {
        if (mOverlay != null && mOverlay.getParent() instanceof ViewGroup) {
            ((ViewGroup) mOverlay.getParent()).removeView(mOverlay);
        }
        mOverlay = null;
    }

    private static boolean isDialogActivity(Activity activity) {
        // Dialogurile sunt ferestre transparente peste altă activitate deja întunecată.
        // Dialogs are transparent windows above an already dimmed activity.
        return activity.getClass().getSimpleName().startsWith("AppDialogActivity");
    }
}
