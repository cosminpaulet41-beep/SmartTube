package com.liskovsoft.smartyoutubetv2.common.hunys;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;

import java.lang.ref.WeakReference;
import java.util.Calendar;

/**
 * Întunecă ecranul în toată aplicația (0-80%), manual sau automat după oră.<br/>
 * Dims the screen across the whole app (0-80%), manually or on a schedule.<br/>
 * Fără apăsări: după un timp fără nicio apăsare (nu în timpul unui clip), ecranul se întunecă 10-100%;
 * primul buton apăsat doar reaprinde ecranul.<br/>
 * No key presses: after a while without any key press (not while a video plays), the screen dims 10-100%;
 * the first key pressed only brings the screen back.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public class HunysDimmer {
    private static final long TICK_MS = 60_000;
    private static final long IDLE_PLAYING_CHECK_MS = 15_000;
    private static final long IDLE_DIMMED_CHECK_MS = 3_000;
    private static final long IDLE_HINT_MS = 6_000;
    @SuppressLint("StaticFieldLeak")
    private static HunysDimmer sInstance;
    private final HunysData mData;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private WeakReference<Activity> mActivity;
    private View mOverlay;
    private final Runnable mTick = this::tick;
    private final Runnable mDataListener = this::onDataChanged;
    // Fără apăsări / no key presses
    private WeakReference<Activity> mTopActivity; // și dialogurile / dialogs too
    private View mIdleOverlay; // peste un dialog / above a dialog
    private long mLastActivityMs = SystemClock.uptimeMillis();
    private boolean mIdleDimmed;
    private int mSwallowKey = -1;
    private final Runnable mIdleCheck = this::checkIdle;
    private final Runnable mHideHint = this::hideIdleHint;

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
        if (activity == null || !HunysData.isHunysBuild(activity)) {
            return;
        }

        removeIdleOverlay();
        mTopActivity = new WeakReference<>(activity);
        mData.removeListener(mDataListener);
        mData.addListener(mDataListener);

        if (isDialogActivity(activity)) {
            // Dialogurile: doar stratul „fără apăsări” / dialogs: only the "no key presses" layer
            updateIdleOverlay();
            scheduleIdleCheck(1_000);
            return;
        }

        removeOverlay();
        mActivity = new WeakReference<>(activity);
        HunysTraffic.sample(activity);
        update();
        mHandler.removeCallbacks(mTick);
        mHandler.postDelayed(mTick, TICK_MS);
        scheduleIdleCheck(1_000);
    }

    public void detach(Activity activity) {
        if (mTopActivity != null && mTopActivity.get() == activity) {
            removeIdleOverlay();
            mTopActivity = null;
        }

        if (mActivity != null && mActivity.get() == activity) {
            removeOverlay();
            mActivity = null;
            mHandler.removeCallbacks(mTick);
            mHandler.removeCallbacks(mIdleCheck);
            mData.removeListener(mDataListener);
        }
    }

    /**
     * Apelat pentru fiecare apăsare. Când ecranul e întunecat de „fără apăsări”, prima apăsare doar îl reaprinde.<br/>
     * Called for every key press. When the screen is dimmed by "no key presses", the first press only brings it back.
     * @return true dacă apăsarea a fost consumată / true if the key press was consumed
     */
    public static boolean onKeyEvent(Context context, KeyEvent event) {
        if (sInstance == null || event == null || context == null || !HunysData.isHunysBuild(context)) {
            return false;
        }

        return sInstance.handleKey(event);
    }

    private boolean handleKey(KeyEvent event) {
        mLastActivityMs = SystemClock.uptimeMillis();
        int keyCode = event.getKeyCode();

        if (mSwallowKey != -1 && keyCode == mSwallowKey) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                mSwallowKey = -1;
            }
            return true; // restul apăsării care a reaprins ecranul / the rest of the press that woke the screen
        }

        if (!mIdleDimmed || event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }

        wakeFromIdle();

        // Volumul merge mai departe / volume still works
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            return false;
        }

        mSwallowKey = keyCode;
        return true;
    }

    public boolean isIdleDimmed() {
        return mIdleDimmed;
    }

    private void wakeFromIdle() {
        mIdleDimmed = false;
        mLastActivityMs = SystemClock.uptimeMillis();
        update();
        updateIdleOverlay();
        scheduleIdleCheck(1_000);
    }

    private void onDataChanged() {
        if (!mData.isIdleDimEnabled() && mIdleDimmed) {
            wakeFromIdle();
            return;
        }

        update();
        updateIdleOverlay();
        scheduleIdleCheck(1_000);
    }

    private void scheduleIdleCheck(long delayMs) {
        mHandler.removeCallbacks(mIdleCheck);

        if (mData.isIdleDimEnabled() || mIdleDimmed) {
            mHandler.postDelayed(mIdleCheck, delayMs);
        }
    }

    private void checkIdle() {
        Activity activity = getTopActivity();

        if (activity == null || !mData.isIdleDimEnabled()) {
            if (mIdleDimmed) {
                wakeFromIdle();
            }
            return;
        }

        long now = SystemClock.uptimeMillis();

        // În timpul unui clip nu se întunecă; timpul se numără de la oprire
        // Doesn't dim while a video plays; the time counts from when it stops
        if (isVideoPlaying(activity)) {
            mLastActivityMs = now;
            if (mIdleDimmed) {
                wakeFromIdle();
            } else {
                scheduleIdleCheck(IDLE_PLAYING_CHECK_MS);
            }
            return;
        }

        if (mIdleDimmed) {
            scheduleIdleCheck(IDLE_DIMMED_CHECK_MS);
            return;
        }

        long timeoutMs = mData.getIdleDimMinutes() * 60_000L;
        long left = mLastActivityMs + timeoutMs - now;

        if (left > 0) {
            scheduleIdleCheck(Math.min(left, IDLE_PLAYING_CHECK_MS));
            return;
        }

        mIdleDimmed = true;
        update();
        updateIdleOverlay();
        showIdleHint();
        scheduleIdleCheck(IDLE_DIMMED_CHECK_MS);
    }

    private static boolean isVideoPlaying(Context context) {
        try {
            PlaybackPresenter presenter = PlaybackPresenter.instance(context);
            return presenter.isPlaying() && !presenter.isRunningInBackground();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private Activity getTopActivity() {
        Activity activity = mTopActivity != null ? mTopActivity.get() : null;
        if (activity == null || activity.isFinishing()) {
            activity = getCurrentActivity();
        }
        return activity;
    }

    /**
     * Stratul „fără apăsări” peste un dialog (activitatea de dedesubt e întunecată de stratul obișnuit).<br/>
     * The "no key presses" layer above a dialog (the activity below is dimmed by the usual layer).
     */
    private void updateIdleOverlay() {
        Activity top = mTopActivity != null ? mTopActivity.get() : null;

        if (!mIdleDimmed || top == null || !isDialogActivity(top) || top.getWindow() == null
                || !(top.getWindow().getDecorView() instanceof ViewGroup)) {
            removeIdleOverlay();
            return;
        }

        ViewGroup root = (ViewGroup) top.getWindow().getDecorView();

        if (mIdleOverlay == null || mIdleOverlay.getParent() != root) {
            removeIdleOverlay();
            mIdleOverlay = createOverlay(top);
            root.addView(mIdleOverlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        mIdleOverlay.setAlpha(mData.getIdleDimLevel() / 100f);
        mIdleOverlay.bringToFront();
    }

    private void removeIdleOverlay() {
        if (mIdleOverlay != null && mIdleOverlay.getParent() instanceof ViewGroup) {
            ((ViewGroup) mIdleOverlay.getParent()).removeView(mIdleOverlay);
        }
        mIdleOverlay = null;
    }

    private TextView mIdleHint;

    /**
     * „Apasă orice buton” jos, câteva secunde / "Press any button" at the bottom, for a few seconds
     */
    private void showIdleHint() {
        Activity top = getTopActivity();

        if (top == null || top.getWindow() == null || !(top.getWindow().getDecorView() instanceof ViewGroup)) {
            return;
        }

        hideIdleHint();
        ViewGroup root = (ViewGroup) top.getWindow().getDecorView();
        float density = top.getResources().getDisplayMetrics().density;
        TextView hint = new TextView(top);
        hint.setText(top.getString(R.string.hunys_idle_dim_wake));
        hint.setTextColor(0x99FFFFFF);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        hint.setPadding(Math.round(16 * density), Math.round(7 * density), Math.round(16 * density), Math.round(7 * density));
        android.graphics.drawable.GradientDrawable pill = new android.graphics.drawable.GradientDrawable();
        pill.setColor(0xFF141414);
        pill.setCornerRadius(20 * density);
        hint.setBackground(pill);
        HunysFonts.apply(hint);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = Math.round(36 * density);
        root.addView(hint, lp);
        hint.bringToFront();
        mIdleHint = hint;
        mHandler.postDelayed(mHideHint, IDLE_HINT_MS);
    }

    private void hideIdleHint() {
        mHandler.removeCallbacks(mHideHint);
        if (mIdleHint != null && mIdleHint.getParent() instanceof ViewGroup) {
            ((ViewGroup) mIdleHint.getParent()).removeView(mIdleHint);
        }
        mIdleHint = null;
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

    /** Ecranul curent al aplicației (pentru bara de întunecare) / the app's current screen (for the dimming bar) */
    public Activity getCurrentActivity() {
        Activity activity = mActivity != null ? mActivity.get() : null;
        return activity != null && !activity.isFinishing() ? activity : null;
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
        Activity activity = mActivity != null ? mActivity.get() : null;
        if (activity != null) {
            HunysTraffic.sample(activity);
        }
        update();
        mHandler.postDelayed(mTick, TICK_MS);
    }

    private void update() {
        Activity activity = mActivity != null ? mActivity.get() : null;

        if (activity == null || activity.getWindow() == null) {
            return;
        }

        int level = getEffectiveLevel();

        if (mIdleDimmed) {
            level = Math.max(level, mData.getIdleDimLevel());
        }

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
            mOverlay = createOverlay(activity);
            root.addView(mOverlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        mOverlay.setAlpha(level / 100f);
        mOverlay.bringToFront();

        if (mIdleHint != null && mIdleHint.getParent() == root) {
            mIdleHint.bringToFront();
        }
    }

    private static View createOverlay(Activity activity) {
        View overlay = new View(activity);
        overlay.setBackgroundColor(Color.BLACK);
        overlay.setClickable(false);
        overlay.setFocusable(false);
        if (Build.VERSION.SDK_INT >= 16) {
            overlay.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        return overlay;
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
