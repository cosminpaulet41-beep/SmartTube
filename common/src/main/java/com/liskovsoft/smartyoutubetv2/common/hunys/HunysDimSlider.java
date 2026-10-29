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

/**
 * Bara de întunecare: se deschide direct de la butonul cu lună; ← → schimbă nivelul cu 10%.<br/>
 * Dimming bar: opens straight from the moon button; left/right change the level by 10%.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public final class HunysDimSlider {
    private static final int STEPS = HunysData.DIM_LEVEL_MAX / HunysData.DIM_LEVEL_STEP; // 8
    private static Dialog sDialog;

    private HunysDimSlider() {
    }

    /**
     * @param onMoreSettings deschis cu ↓ (întunecare automată) / opened with down (automatic dimming)
     * @return false dacă nu se poate arăta (atunci se folosește dialogul vechi) / false if it can't be shown
     */
    public static boolean show(Context context, Runnable onMoreSettings) {
        Activity activity = HunysDimmer.instance(context).getCurrentActivity();

        if (activity == null) {
            return false;
        }

        dismiss();

        try {
            sDialog = create(activity, onMoreSettings);
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

    private static Dialog create(Activity activity, Runnable onMoreSettings) {
        HunysDimmer dimmer = HunysDimmer.instance(activity);
        int accent = themeColor(activity, "hunysAccent", 0xFF8FB4F5);
        int panel = (themeColor(activity, "hunysPanel", 0xFF1B1E26) & 0x00FFFFFF) | 0xF2000000;

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 12));
        GradientDrawable background = new GradientDrawable();
        background.setColor(panel);
        background.setCornerRadius(dp(activity, 20));
        background.setStroke(dp(activity, 1), 0x1AFFFFFF);
        root.setBackground(background);
        root.setMinimumWidth(dp(activity, 290));

        // Titlu + valoare / title + value
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(activity, activity.getString(R.string.hunys_dimming), 13, true, 0xCCFFFFFF);
        TextView value = text(activity, "", 16, true, Color.WHITE);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        header.addView(value);
        root.addView(header);

        // Cele 8 trepte (10% ... 80%) / the 8 steps (10% ... 80%)
        LinearLayout ticks = new LinearLayout(activity);
        ticks.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ticksParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 8));
        ticksParams.topMargin = dp(activity, 12);
        ticksParams.bottomMargin = dp(activity, 10);
        root.addView(ticks, ticksParams);

        GradientDrawable[] tickDrawables = new GradientDrawable[STEPS];
        for (int i = 0; i < STEPS; i++) {
            View tick = new View(activity);
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(dp(activity, 4));
            tick.setBackground(shape);
            tickDrawables[i] = shape;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
            lp.leftMargin = i == 0 ? 0 : dp(activity, 4);
            ticks.addView(tick, lp);
        }

        root.addView(text(activity, activity.getString(R.string.hunys_dim_hint), 10.5f, false, 0xA6FFFFFF));

        final int[] level = {dimmer.getEffectiveLevel()};
        Runnable render = () -> {
            value.setText(level[0] <= 0 ? activity.getString(R.string.hunys_off) : level[0] + "%");
            int filled = level[0] / HunysData.DIM_LEVEL_STEP;
            for (int i = 0; i < STEPS; i++) {
                tickDrawables[i].setColor(i < filled ? accent : 0x26FFFFFF);
            }
        };
        render.run();

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(root);
        dialog.setCanceledOnTouchOutside(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Fără umbră peste ecran, ca să vezi întunecarea pe loc / no screen shade, so you see the dimming live
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.y = dp(activity, 40);
            attrs.width = WindowManager.LayoutParams.WRAP_CONTENT;
            attrs.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(attrs);
        }

        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_ESCAPE;
            }

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    int delta = keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? HunysData.DIM_LEVEL_STEP : -HunysData.DIM_LEVEL_STEP;
                    level[0] = HunysData.clampDim(level[0] + delta);
                    dimmer.setLevelByUser(level[0]);
                    render.run();
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    d.dismiss();
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    d.dismiss();
                    if (onMoreSettings != null) {
                        onMoreSettings.run();
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    return true;
                default:
                    return false; // BACK închide / BACK closes
            }
        });

        dialog.setOnDismissListener(d -> {
            if (sDialog == d) {
                sDialog = null;
            }
        });

        return dialog;
    }

    private static TextView text(Context context, String value, float sizeSp, boolean bold, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT_BOLD);
        }
        HunysFonts.apply(view);
        return view;
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

    private static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }
}
