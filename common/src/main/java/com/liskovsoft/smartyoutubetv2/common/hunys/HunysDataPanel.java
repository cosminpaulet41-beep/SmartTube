package com.liskovsoft.smartyoutubetv2.common.hunys;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
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

import androidx.annotation.Nullable;

import com.liskovsoft.smartyoutubetv2.common.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Panoul „Contor date”: graficul ultimelor 30 de zile + setările contorului.<br/>
 * ← → alegi ziua din grafic, ↓ mergi la setări, Înapoi închide.<br/>
 * The "Data usage" panel: a chart of the last 30 days + the counter settings.<br/>
 * Left/right choose the day on the chart, down goes to the settings, Back closes.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public final class HunysDataPanel {
    private static final int[] LIMITS_GB = {0, 5, 10, 15, 20, 30, 50, 100};
    private static final int FOCUS_CHART = -1;
    private static final int ROW_LIMIT = 0;
    private static final int ROW_WARN = 1;
    private static final int ROW_RESET = 2;
    private static final int GB = 1024 * 1024 * 1024;
    private static Dialog sDialog;

    private HunysDataPanel() {
    }

    /**
     * @param onBack rulat când se închide cu Înapoi (ex: redeschide setările) / run when closed with Back (e.g. reopen settings)
     * @return false dacă nu se poate arăta / false if it can't be shown
     */
    public static boolean show(Context context, @Nullable Runnable onBack) {
        Activity activity = HunysDimmer.instance(context).getCurrentActivity();

        if (activity == null) {
            return false;
        }

        dismiss();

        try {
            HunysTraffic.sample(activity);
            sDialog = new Panel(activity, onBack).dialog;
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
        private final Runnable mOnBack;
        private final int mAccent;
        private final Locale mLocale;
        private final Chart mChart;
        private final TextView mDayLabel;
        private final TextView mToday;
        private final TextView mMonth;
        private final TextView mAverage;
        private final View[] mRows = new View[3];
        private final TextView[] mTitles = new TextView[3];
        private final TextView[] mValues = new TextView[3];
        private int mFocus = FOCUS_CHART;
        private boolean mResetArmed;

        Panel(Activity activity, @Nullable Runnable onBack) {
            mActivity = activity;
            mData = HunysData.instance(activity);
            mOnBack = onBack;
            mAccent = themeColor(activity, "hunysAccent", 0xFF8FB4F5);
            mLocale = activity.getResources().getConfiguration().locale;

            int panel = (themeColor(activity, "hunysPanel", 0xFF1B1E26) & 0x00FFFFFF) | 0xF7000000;

            LinearLayout root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(22), dp(24), dp(22), dp(14));
            GradientDrawable background = new GradientDrawable();
            background.setColor(panel);
            float r = dp(18);
            background.setCornerRadii(new float[] {r, r, 0, 0, 0, 0, r, r});
            root.setBackground(background);

            TextView title = text(activity.getString(R.string.hunys_data_counter), 19, 700, Color.WHITE);
            title.setPadding(dp(4), 0, 0, 0);
            root.addView(title);

            mChart = new Chart(activity, mAccent);
            LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(168));
            chartParams.topMargin = dp(14);
            root.addView(mChart, chartParams);

            TextView hint = text(activity.getString(R.string.hunys_data_hint), 10, 400, 0x8CFFFFFF);
            hint.setPadding(dp(4), dp(7), 0, 0);
            root.addView(hint);

            mDayLabel = text("", 12.5f, 600, mAccent);
            mDayLabel.setPadding(dp(4), dp(10), 0, dp(4));
            root.addView(mDayLabel);

            mToday = infoLine(root);
            mMonth = infoLine(root);
            mAverage = infoLine(root);

            View separator = new View(activity);
            separator.setBackgroundColor(0x1AFFFFFF);
            LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
            sepParams.setMargins(dp(4), dp(6), dp(4), dp(6));
            root.addView(separator, sepParams);

            addRow(root, ROW_LIMIT, R.string.hunys_data_limit);
            addRow(root, ROW_WARN, R.string.hunys_data_warn);
            addRow(root, ROW_RESET, R.string.hunys_data_reset);

            dialog = new Dialog(activity, R.style.HunysDimPanel);
            dialog.setContentView(root, new LinearLayout.LayoutParams(dp(300), LinearLayout.LayoutParams.MATCH_PARENT));
            dialog.setCanceledOnTouchOutside(true);

            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setGravity(Gravity.TOP | Gravity.END);
                WindowManager.LayoutParams attrs = window.getAttributes();
                attrs.x = 0;
                attrs.y = 0;
                attrs.width = dp(300);
                attrs.height = WindowManager.LayoutParams.MATCH_PARENT;
                attrs.dimAmount = 0.35f;
                window.setAttributes(attrs);
            }

            dialog.setOnKeyListener((d, keyCode, event) -> onKey(keyCode, event));
            dialog.setOnDismissListener(d -> {
                if (sDialog == d) {
                    sDialog = null;
                }
            });

            reload();
        }

        private TextView infoLine(LinearLayout root) {
            TextView view = text("", 13.5f, 500, 0xF2FFFFFF);
            view.setGravity(Gravity.CENTER_VERTICAL);
            view.setPadding(dp(6), 0, dp(6), 0);
            root.addView(view, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(29)));
            return view;
        }

        private void addRow(LinearLayout root, int row, int titleRes) {
            LinearLayout line = new LinearLayout(mActivity);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(dp(6), 0, dp(10), 0);
            TextView title = text(mActivity.getString(titleRes), 13.5f, 500, 0xF2FFFFFF);
            line.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            TextView value = text("", 13, 700, Color.WHITE);
            line.addView(value);
            mTitles[row] = title;
            mValues[row] = value;
            mRows[row] = line;
            root.addView(line, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(36)));
        }

        private boolean onKey(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    dialog.dismiss();
                    if (mOnBack != null) {
                        mOnBack.run();
                    }
                }
                return true;
            }

            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return true;
            }

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    setFocus(mFocus == FOCUS_CHART ? FOCUS_CHART : mFocus - 1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    setFocus(Math.min(ROW_RESET, mFocus + 1));
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
                    press();
                    return true;
                default:
                    return false;
            }
        }

        private void setFocus(int focus) {
            if (focus != mFocus) {
                mResetArmed = false;
            }
            mFocus = focus;
            render();
        }

        private void change(int direction) {
            switch (mFocus) {
                case FOCUS_CHART:
                    mChart.moveSelection(direction);
                    break;
                case ROW_LIMIT:
                    int index = Math.max(0, Math.min(LIMITS_GB.length - 1, limitIndex() + direction));
                    mData.setDataLimitMb(LIMITS_GB[index] * 1024);
                    break;
                case ROW_WARN:
                    mData.setDataWarnEnabled(!mData.isDataWarnEnabled());
                    break;
                default:
                    return;
            }

            render();
        }

        private void press() {
            switch (mFocus) {
                case ROW_LIMIT:
                    int index = (limitIndex() + 1) % LIMITS_GB.length;
                    mData.setDataLimitMb(LIMITS_GB[index] * 1024);
                    break;
                case ROW_WARN:
                    mData.setDataWarnEnabled(!mData.isDataWarnEnabled());
                    break;
                case ROW_RESET:
                    if (mResetArmed) {
                        HunysTraffic.reset(mActivity);
                        mResetArmed = false;
                        reload();
                        return;
                    }
                    mResetArmed = true; // al doilea OK resetează / the second OK resets
                    break;
                default:
                    return;
            }

            render();
        }

        /**
         * Limita aleasă (sau cea mai apropiată din listă) / the chosen limit (or the closest one in the list)
         */
        private int limitIndex() {
            int limitGb = Math.round(mData.getDataLimitMb() / 1024f);
            int best = 0;

            for (int i = 0; i < LIMITS_GB.length; i++) {
                if (Math.abs(LIMITS_GB[i] - limitGb) < Math.abs(LIMITS_GB[best] - limitGb)) {
                    best = i;
                }
            }

            return best;
        }

        private void reload() {
            mChart.setData(HunysTraffic.getLastDays(mActivity), HunysTraffic.chartEndDay(mActivity), mLocale,
                    mActivity.getString(R.string.hunys_today));
            mToday.setText(mActivity.getString(R.string.hunys_data_today, HunysTraffic.format(HunysTraffic.getTodayBytes(mActivity))));
            mMonth.setText(mActivity.getString(R.string.hunys_data_month, HunysTraffic.format(HunysTraffic.getMonthBytes(mActivity))));
            mAverage.setText(mActivity.getString(R.string.hunys_data_average,
                    HunysTraffic.format(HunysTraffic.getMonthlyAverageBytes(mActivity))));
            render();
        }

        private void render() {
            mChart.setFocusedLook(mFocus == FOCUS_CHART);
            mDayLabel.setText(dayLabel(mChart.getSelectedDay()));

            int limitGb = LIMITS_GB[limitIndex()];
            mValues[ROW_LIMIT].setText("‹ " + (limitGb == 0 ? mActivity.getString(R.string.hunys_data_no_limit) : limitGb + " GB") + " ›");
            boolean warn = mData.isDataWarnEnabled();
            mValues[ROW_WARN].setText(mActivity.getString(warn ? R.string.hunys_on : R.string.hunys_off));
            mValues[ROW_WARN].setTextColor(warn ? mAccent : 0x99FFFFFF);
            mTitles[ROW_RESET].setText(mActivity.getString(mResetArmed ? R.string.hunys_data_reset_confirm : R.string.hunys_data_reset));
            mTitles[ROW_RESET].setTextColor(mResetArmed ? 0xFFFF9C8F : 0xF2FFFFFF);

            for (int row = 0; row < mRows.length; row++) {
                if (row == mFocus) {
                    GradientDrawable pill = new GradientDrawable();
                    pill.setColor((mAccent & 0x00FFFFFF) | 0x4D000000);
                    pill.setCornerRadius(dp(10));
                    mRows[row].setBackground(pill);
                } else {
                    mRows[row].setBackground(null);
                }
            }
        }

        /**
         * Ex: „Joi, 20 noiembrie” / "Thursday, 20 November"
         */
        private String dayLabel(Calendar day) {
            String text = new SimpleDateFormat("EEEE, d MMMM", mLocale).format(day.getTime());
            return text.isEmpty() ? text : text.substring(0, 1).toUpperCase(mLocale) + text.substring(1);
        }

        private TextView text(String value, float sizeSp, int weight, int color) {
            TextView view = new TextView(mActivity);
            view.setText(value);
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
            view.setTextColor(color);
            view.setIncludeFontPadding(false);
            view.setSingleLine(true);
            if (weight >= 600) {
                view.setTypeface(Typeface.DEFAULT_BOLD);
            }
            Typeface inter = HunysFonts.typeface(mActivity, weight);
            if (inter != null) {
                view.setTypeface(inter);
            }
            return view;
        }

        private int dp(float value) {
            return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, mActivity.getResources().getDisplayMetrics()));
        }
    }

    /**
     * Graficul cu bare: o bară pe zi, ziua aleasă e mai deschisă, valoarea ei apare sus în dreapta.<br/>
     * Liniile merg de la 1 la 3 GB și cresc până la 10 după cea mai mare zi.<br/>
     * The bar chart: one bar per day, the chosen day is lighter, its value shows top right.<br/>
     * Lines go from 1 to 3 GB and grow up to 10 with the biggest day.
     */
    @SuppressLint("ViewConstructor")
    private static final class Chart extends View {
        private final int mAccent;
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mText = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF mRect = new RectF();
        private final float mDensity;
        private final float mScaled;
        private long[] mDays = new long[HunysTraffic.CHART_DAYS];
        private final String[] mLabels = new String[HunysTraffic.CHART_DAYS];
        private Calendar mEndDay = Calendar.getInstance();
        private int mSelected = HunysTraffic.CHART_DAYS - 1;
        private boolean mFocused = true;
        private final Typeface mRegular;
        private final Typeface mBold;

        Chart(Context context, int accent) {
            super(context);
            mAccent = accent;
            mDensity = context.getResources().getDisplayMetrics().density;
            mScaled = context.getResources().getDisplayMetrics().scaledDensity;
            Typeface regular = HunysFonts.typeface(context, 500);
            Typeface bold = HunysFonts.typeface(context, 700);
            mRegular = regular != null ? regular : Typeface.DEFAULT;
            mBold = bold != null ? bold : Typeface.DEFAULT_BOLD;
        }

        void setData(long[] days, Calendar endDay, Locale locale, String todayLabel) {
            mDays = days;
            mEndDay = endDay;
            mSelected = Math.min(mSelected, days.length - 1);

            // Etichete la fiecare 5 zile + „Azi” la final; luna apare la prima și când se schimbă
            // Labels every 5 days + "Today" at the end; the month shows on the first one and when it changes
            SimpleDateFormat month = new SimpleDateFormat("MMM", locale);
            int lastMonth = -1;
            for (int i = 0; i < mLabels.length; i++) {
                mLabels[i] = null;
                boolean isLast = i == mLabels.length - 1;
                if (isLast) {
                    mLabels[i] = todayLabel;
                } else if (i % 5 == 0) {
                    Calendar day = dayAt(i);
                    int m = day.get(Calendar.MONTH);
                    String label = String.valueOf(day.get(Calendar.DAY_OF_MONTH));
                    if (m != lastMonth) {
                        label += " " + month.format(day.getTime()).replace(".", "").toLowerCase(locale);
                    }
                    lastMonth = m;
                    mLabels[i] = label;
                }
            }

            invalidate();
        }

        void moveSelection(int direction) {
            mSelected = Math.max(0, Math.min(mDays.length - 1, mSelected + direction));
            invalidate();
        }

        void setFocusedLook(boolean focused) {
            if (mFocused != focused) {
                mFocused = focused;
                invalidate();
            }
        }

        Calendar getSelectedDay() {
            return dayAt(mSelected);
        }

        private Calendar dayAt(int index) {
            Calendar day = (Calendar) mEndDay.clone();
            day.add(Calendar.DAY_OF_MONTH, index - (mDays.length - 1));
            return day;
        }

        private float dp(float value) {
            return value * mDensity;
        }

        private float sp(float value) {
            return value * mScaled;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();

            // Chenarul / the frame
            mRect.set(dp(1), dp(1), w - dp(1), h - dp(1));
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(0x14FFFFFF);
            canvas.drawRoundRect(mRect, dp(12), dp(12), mPaint);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(dp(1.5f));
            mPaint.setColor(mFocused ? (mAccent & 0x00FFFFFF) | 0xB3000000 : 0x26FFFFFF);
            canvas.drawRoundRect(mRect, dp(12), dp(12), mPaint);
            mPaint.setStyle(Paint.Style.FILL);

            float left = dp(24);
            float right = w - dp(9);
            float top = dp(40);
            float bottom = h - dp(24);
            float plotH = bottom - top;

            // Câte linii: 1–3 mereu, apoi până la 10 după cea mai mare zi / how many lines: 1–3 always, then up to 10
            double maxGb = 0;
            for (long bytes : mDays) {
                maxGb = Math.max(maxGb, bytes / (double) GB);
            }
            int lines = (int) Math.max(3, Math.min(10, Math.ceil(maxGb)));

            mText.setTypeface(mRegular);
            mText.setTextSize(sp(lines > 6 ? 8 : 9));
            mText.setColor(0x99FFFFFF);
            mText.setTextAlign(Paint.Align.RIGHT);
            float textMid = (mText.descent() + mText.ascent()) / 2;

            for (int i = 0; i <= lines; i++) {
                float y = bottom - plotH * i / lines;
                mPaint.setColor(i == 0 ? 0x29FFFFFF : 0x17FFFFFF);
                canvas.drawRect(left, y - dp(0.5f), right, y + dp(0.5f), mPaint);
                canvas.drawText(String.valueOf(i), left - dp(6), y - textMid, mText);
            }

            // „GB” o singură dată, sus / "GB" once, at the top
            mText.setTextAlign(Paint.Align.LEFT);
            mText.setTextSize(sp(8));
            canvas.drawText("GB", dp(7), top - dp(9), mText);

            // Barele / the bars
            int count = mDays.length;
            float slot = (right - left) / count;
            float barW = slot * 0.62f;

            for (int i = 0; i < count; i++) {
                if (mDays[i] <= 0) {
                    continue;
                }
                float ratio = (float) Math.min(1.0, mDays[i] / (double) GB / lines);
                float barH = Math.max(dp(1.5f), plotH * ratio);
                float x = left + i * slot + (slot - barW) / 2;
                mRect.set(x, bottom - barH, x + barW, bottom);
                mPaint.setColor(i == mSelected ? mAccent : (mAccent & 0x00FFFFFF) | 0x80000000);
                canvas.drawRoundRect(mRect, dp(1.5f), dp(1.5f), mPaint);
            }

            // Ziua aleasă fără date: un semn mic, ca să se vadă unde ești / chosen day without data: a small mark
            if (mDays[mSelected] <= 0) {
                float x = left + mSelected * slot + (slot - barW) / 2;
                mRect.set(x, bottom - dp(2), x + barW, bottom);
                mPaint.setColor(mAccent);
                canvas.drawRect(mRect, mPaint);
            }

            // Etichetele de jos / bottom labels
            mText.setTextSize(sp(9));
            mText.setColor(0x99FFFFFF);
            float labelY = h - dp(9);
            for (int i = 0; i < count; i++) {
                if (mLabels[i] == null) {
                    continue;
                }
                float center = left + i * slot + slot / 2;
                if (i == 0) {
                    mText.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText(mLabels[i], left - dp(2), labelY, mText);
                } else if (i == count - 1) {
                    mText.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText(mLabels[i], right + dp(2), labelY, mText);
                } else {
                    mText.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(mLabels[i], center, labelY, mText);
                }
            }

            // Bula cu valoarea zilei alese, sus în dreapta / bubble with the chosen day's value, top right
            String value = HunysTraffic.format(mDays[mSelected]);
            mText.setTypeface(mBold);
            mText.setTextSize(sp(11.5f));
            mText.setColor(Color.WHITE);
            mText.setTextAlign(Paint.Align.CENTER);
            float pillW = mText.measureText(value) + dp(22);
            float pillH = dp(23);
            float pillRight = w - dp(8);
            float pillTop = dp(7);
            mRect.set(pillRight - pillW, pillTop, pillRight, pillTop + pillH);
            mPaint.setColor(0x24FFFFFF);
            canvas.drawRoundRect(mRect, pillH / 2, pillH / 2, mPaint);
            float textY = mRect.centerY() - (mText.descent() + mText.ascent()) / 2;
            canvas.drawText(value, mRect.centerX(), textY, mText);
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
