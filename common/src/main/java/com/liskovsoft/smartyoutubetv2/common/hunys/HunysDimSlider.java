package com.liskovsoft.smartyoutubetv2.common.hunys;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.liskovsoft.smartyoutubetv2.common.R;

import java.util.Locale;

/**
 * Panoul mic de întunecare: nivelul pe o bară și întunecarea automată, toate într-un singur loc.<br/>
 * ↑ ↓ alegi rândul, ← → schimbi valoarea, OK sau Înapoi închide.<br/>
 * Small dimming panel: the level on a bar plus automatic dimming, all in one place.<br/>
 * Up/down choose the row, left/right change the value, OK or Back closes.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public final class HunysDimSlider {
    private static final int STEPS = HunysData.DIM_LEVEL_MAX / HunysData.DIM_LEVEL_STEP; // 8
    private static final int TIME_STEP_MIN = 30;
    private static final int ROW_LEVEL = 0;
    private static final int ROW_AUTO = 1;
    private static final int ROW_START = 2;
    private static final int ROW_END = 3;
    private static final int ROW_NIGHT = 4;
    private static Dialog sDialog;

    private HunysDimSlider() {
    }

    /**
     * @return false dacă nu se poate arăta (atunci se folosește dialogul vechi) / false if it can't be shown
     */
    public static boolean show(Context context) {
        Activity activity = HunysDimmer.instance(context).getCurrentActivity();

        if (activity == null) {
            return false;
        }

        dismiss();

        try {
            sDialog = new Panel(activity).dialog;
            sDialog.show();
            return true;
        } catch (RuntimeException e) {
            sDialog = null;
            return false;
        }
    }

    private static void dismiss() {
        if (sDialog != null) {
            try {
                sDialog.dismiss();
            } catch (RuntimeException e) {
                // activity already gone
            }
            sDialog = null;
        }
    }

    private static final class Panel {
        final Dialog dialog;
        private final Activity mActivity;
        private final HunysData mData;
        private final HunysDimmer mDimmer;
        private final int mAccent;
        private final View[] mRows = new View[5];
        private final TextView[] mValues = new TextView[5];
        private final GradientDrawable[] mTicks = new GradientDrawable[STEPS];
        private TextView mLevelValue;
        private int mSelected = ROW_LEVEL;
        private int mLevel;

        Panel(Activity activity) {
            mActivity = activity;
            mData = HunysData.instance(activity);
            mDimmer = HunysDimmer.instance(activity);
            mAccent = themeColor(activity, "hunysAccent", 0xFF8FB4F5);
            mLevel = mDimmer.getEffectiveLevel();

            int panel = (themeColor(activity, "hunysPanel", 0xFF1B1E26) & 0x00FFFFFF) | 0xF2000000;

            LinearLayout root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(8), dp(10), dp(8), dp(8));
            GradientDrawable background = new GradientDrawable();
            background.setColor(panel);
            background.setCornerRadius(dp(16));
            background.setStroke(dp(1), 0x1AFFFFFF);
            root.setBackground(background);

            // Rândul 1: titlu + procent, iar dedesubt bara / row 1: title + percent, bar below
            LinearLayout levelRow = new LinearLayout(activity);
            levelRow.setOrientation(LinearLayout.VERTICAL);
            levelRow.setPadding(dp(8), dp(6), dp(8), dp(8));
            LinearLayout header = new LinearLayout(activity);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.addView(text(activity.getString(R.string.hunys_dimming), 11.5f, true, 0xDDFFFFFF),
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            mLevelValue = text("", 13, true, Color.WHITE);
            header.addView(mLevelValue);
            levelRow.addView(header);

            LinearLayout ticks = new LinearLayout(activity);
            ticks.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams ticksParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(5));
            ticksParams.topMargin = dp(8);
            levelRow.addView(ticks, ticksParams);

            for (int i = 0; i < STEPS; i++) {
                View tick = new View(activity);
                GradientDrawable shape = new GradientDrawable();
                shape.setCornerRadius(dp(3));
                tick.setBackground(shape);
                mTicks[i] = shape;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
                lp.leftMargin = i == 0 ? 0 : dp(3);
                ticks.addView(tick, lp);
            }

            mRows[ROW_LEVEL] = levelRow;
            root.addView(levelRow);

            // Separator
            View separator = new View(activity);
            separator.setBackgroundColor(0x1AFFFFFF);
            LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
            sepParams.setMargins(dp(8), dp(4), dp(8), dp(4));
            root.addView(separator, sepParams);

            addValueRow(root, ROW_AUTO, R.string.hunys_auto_dim);
            addValueRow(root, ROW_START, R.string.hunys_auto_dim_start);
            addValueRow(root, ROW_END, R.string.hunys_auto_dim_end);
            addValueRow(root, ROW_NIGHT, R.string.hunys_auto_dim_level);

            TextView hint = text(activity.getString(R.string.hunys_dim_hint), 9.5f, false, 0x8CFFFFFF);
            hint.setPadding(dp(8), dp(6), dp(8), dp(2));
            root.addView(hint);

            dialog = new Dialog(activity, R.style.HunysDimPanel);
            dialog.setContentView(root, new LinearLayout.LayoutParams(dp(236), LinearLayout.LayoutParams.WRAP_CONTENT));
            dialog.setCanceledOnTouchOutside(true);

            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                // Fără umbră, ca să vezi întunecarea pe loc / no shade, so you see the dimming live
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setGravity(Gravity.TOP | Gravity.END);
                WindowManager.LayoutParams attrs = window.getAttributes();
                attrs.x = dp(20);
                attrs.y = dp(20);
                attrs.width = dp(236);
                attrs.height = WindowManager.LayoutParams.WRAP_CONTENT;
                window.setAttributes(attrs);
            }

            dialog.setOnKeyListener((d, keyCode, event) -> onKey(keyCode, event));
            dialog.setOnDismissListener(d -> {
                if (sDialog == d) {
                    sDialog = null;
                }
            });

            render();
        }

        private void addValueRow(LinearLayout root, int row, int titleRes) {
            LinearLayout line = new LinearLayout(mActivity);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(dp(8), 0, dp(8), 0);
            line.addView(text(mActivity.getString(titleRes), 11, false, 0xE6FFFFFF),
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            TextView value = text("", 11, true, Color.WHITE);
            line.addView(value);
            mValues[row] = value;
            mRows[row] = line;
            root.addView(line, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(28)));
        }

        private boolean onKey(int keyCode, KeyEvent event) {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_ESCAPE;
            }

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    move(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    move(1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    change(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    change(1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    if (mSelected == ROW_AUTO) {
                        change(1); // OK pornește/oprește / OK toggles
                    } else {
                        dialog.dismiss();
                    }
                    return true;
                default:
                    return false; // Înapoi închide / Back closes
            }
        }

        private void move(int direction) {
            int last = mData.isAutoDimEnabled() ? ROW_NIGHT : ROW_AUTO;
            mSelected = Math.max(ROW_LEVEL, Math.min(last, mSelected + direction));
            render();
        }

        private void change(int direction) {
            switch (mSelected) {
                case ROW_LEVEL:
                    mLevel = HunysData.clampDim(mLevel + direction * HunysData.DIM_LEVEL_STEP);
                    mDimmer.setLevelByUser(mLevel);
                    break;
                case ROW_AUTO:
                    mData.setAutoDimEnabled(!mData.isAutoDimEnabled());
                    mLevel = mDimmer.getEffectiveLevel();
                    break;
                case ROW_START:
                    mData.setAutoDimStartMin(mData.getAutoDimStartMin() + direction * TIME_STEP_MIN);
                    mLevel = mDimmer.getEffectiveLevel();
                    break;
                case ROW_END:
                    mData.setAutoDimEndMin(mData.getAutoDimEndMin() + direction * TIME_STEP_MIN);
                    mLevel = mDimmer.getEffectiveLevel();
                    break;
                case ROW_NIGHT:
                    int night = Math.max(HunysData.DIM_LEVEL_STEP,
                            HunysData.clampDim(mData.getAutoDimLevel() + direction * HunysData.DIM_LEVEL_STEP));
                    mData.setAutoDimLevel(night);
                    mLevel = mDimmer.getEffectiveLevel();
                    break;
            }

            render();
        }

        private void render() {
            boolean auto = mData.isAutoDimEnabled();

            mLevelValue.setText(levelText(mLevel));
            int filled = mLevel / HunysData.DIM_LEVEL_STEP;
            for (int i = 0; i < STEPS; i++) {
                mTicks[i].setColor(i < filled ? mAccent : 0x26FFFFFF);
            }

            mValues[ROW_AUTO].setText(mActivity.getString(auto ? R.string.hunys_on : R.string.hunys_off));
            mValues[ROW_AUTO].setTextColor(auto ? mAccent : 0x99FFFFFF);
            mValues[ROW_START].setText("‹ " + time(mData.getAutoDimStartMin()) + " ›");
            mValues[ROW_END].setText("‹ " + time(mData.getAutoDimEndMin()) + " ›");
            mValues[ROW_NIGHT].setText("‹ " + levelText(mData.getAutoDimLevel()) + " ›");

            // Orele apar doar când întunecarea automată e pornită / hours show only when automatic dimming is on
            for (int row = ROW_START; row <= ROW_NIGHT; row++) {
                mRows[row].setVisibility(auto ? View.VISIBLE : View.GONE);
            }

            if (!auto && mSelected > ROW_AUTO) {
                mSelected = ROW_AUTO;
            }

            for (int row = 0; row < mRows.length; row++) {
                if (row == mSelected) {
                    GradientDrawable pill = new GradientDrawable();
                    pill.setColor((mAccent & 0x00FFFFFF) | 0x4D000000);
                    pill.setCornerRadius(dp(10));
                    mRows[row].setBackground(pill);
                } else {
                    mRows[row].setBackground(null);
                }
            }
        }

        private String levelText(int level) {
            return level <= 0 ? mActivity.getString(R.string.hunys_off) : level + "%";
        }

        private static String time(int minutes) {
            return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60);
        }

        private TextView text(String value, float sizeSp, boolean bold, int color) {
            TextView view = new TextView(mActivity);
            view.setText(value);
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
            view.setTextColor(color);
            view.setIncludeFontPadding(false);
            view.setSingleLine(true);
            if (bold) {
                view.setTypeface(Typeface.DEFAULT_BOLD);
            }
            HunysFonts.apply(view);
            return view;
        }

        private int dp(float value) {
            return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, mActivity.getResources().getDisplayMetrics()));
        }
    }

    private static int themeColor(Context context, String attrName, int fallback) {
        int attr = context.getResources().getIdentifier(attrName, "attr", context.getPackageName());

        if (attr == 0) {
            return fallback;
        }

        TypedValue value = new TypedValue();

        if (!context.getTheme().resolveAttribute(attr, value, true)) {
            return fallback;
        }

        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return value.data;
        }

        return fallback;
    }
}
