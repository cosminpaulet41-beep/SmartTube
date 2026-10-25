package com.liskovsoft.smartyoutubetv2.common.hunys;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.os.Process;
import android.widget.Toast;

import com.liskovsoft.smartyoutubetv2.common.R;

import java.util.Calendar;
import java.util.Locale;

/**
 * Contor de date: cât internet a folosit aplicația azi și luna aceasta.<br/>
 * Data counter: how much internet the app used today and this month.<br/>
 * Folosește contoarele sistemului pentru aplicația noastră (se resetează la repornirea proiectorului,
 * de aceea adunăm diferențele). / Uses the system counters for our app (they reset on reboot, so we add up the deltas).<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public final class HunysTraffic {
    private static final String K_LAST_RAW = "traffic_last_raw";
    private static final String K_DAY_KEY = "traffic_day_key";
    private static final String K_DAY_BYTES = "traffic_day_bytes";
    private static final String K_MONTH_KEY = "traffic_month_key";
    private static final String K_MONTH_BYTES = "traffic_month_bytes";
    private static final String K_WARNED_MONTH = "traffic_warned_month";

    private HunysTraffic() {
    }

    /**
     * Adună traficul nou. Apelat la fiecare minut și la revenirea în aplicație.<br/>
     * Adds the new traffic. Called every minute and when the app resumes.
     */
    public static synchronized void sample(Context context) {
        if (context == null || !HunysData.isHunysBuild(context)) {
            return;
        }

        HunysData data = HunysData.instance(context);
        SharedPreferences prefs = data.getPrefs();

        long raw = readRaw();

        if (raw < 0) {
            return; // not supported on this device
        }

        long last = prefs.getLong(K_LAST_RAW, -1);
        long delta;

        if (last < 0) {
            delta = 0; // first sample: nothing to add yet
        } else if (raw >= last) {
            delta = raw - last;
        } else {
            delta = raw; // counters were reset by a reboot
        }

        String dayKey = dayKey();
        String monthKey = monthKey();

        long dayBytes = dayKey.equals(prefs.getString(K_DAY_KEY, "")) ? prefs.getLong(K_DAY_BYTES, 0) : 0;
        long monthBytes = monthKey.equals(prefs.getString(K_MONTH_KEY, "")) ? prefs.getLong(K_MONTH_BYTES, 0) : 0;

        dayBytes += delta;
        monthBytes += delta;

        prefs.edit()
                .putLong(K_LAST_RAW, raw)
                .putString(K_DAY_KEY, dayKey)
                .putLong(K_DAY_BYTES, dayBytes)
                .putString(K_MONTH_KEY, monthKey)
                .putLong(K_MONTH_BYTES, monthBytes)
                .apply();

        checkLimit(context, data, monthBytes, monthKey);
    }

    public static long getTodayBytes(Context context) {
        SharedPreferences prefs = HunysData.instance(context).getPrefs();
        return dayKey().equals(prefs.getString(K_DAY_KEY, "")) ? prefs.getLong(K_DAY_BYTES, 0) : 0;
    }

    public static long getMonthBytes(Context context) {
        SharedPreferences prefs = HunysData.instance(context).getPrefs();
        return monthKey().equals(prefs.getString(K_MONTH_KEY, "")) ? prefs.getLong(K_MONTH_BYTES, 0) : 0;
    }

    public static void reset(Context context) {
        HunysData.instance(context).getPrefs().edit()
                .putLong(K_DAY_BYTES, 0)
                .putLong(K_MONTH_BYTES, 0)
                .putString(K_WARNED_MONTH, "")
                .apply();
    }

    /**
     * Ex: 350 MB, 1,25 GB
     */
    public static String format(long bytes) {
        double mb = bytes / (1024.0 * 1024.0);

        if (mb < 1000) {
            return String.format(Locale.getDefault(), "%.0f MB", mb);
        }

        return String.format(Locale.getDefault(), "%.2f GB", mb / 1024.0);
    }

    private static void checkLimit(Context context, HunysData data, long monthBytes, String monthKey) {
        int limitMb = data.getDataLimitMb();

        if (!data.isDataWarnEnabled() || limitMb <= 0) {
            return;
        }

        SharedPreferences prefs = data.getPrefs();

        if (monthBytes >= limitMb * 1024L * 1024L && !monthKey.equals(prefs.getString(K_WARNED_MONTH, ""))) {
            prefs.edit().putString(K_WARNED_MONTH, monthKey).apply();
            String message = context.getString(R.string.hunys_data_warning, format(monthBytes), format(limitMb * 1024L * 1024L));
            try {
                Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_LONG).show();
            } catch (RuntimeException e) {
                // no looper: skip the toast
            }
        }
    }

    private static long readRaw() {
        int uid = Process.myUid();
        long rx = TrafficStats.getUidRxBytes(uid);
        long tx = TrafficStats.getUidTxBytes(uid);

        if (rx == TrafficStats.UNSUPPORTED || tx == TrafficStats.UNSUPPORTED) {
            return -1;
        }

        return rx + tx;
    }

    private static String dayKey() {
        Calendar cal = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    private static String monthKey() {
        Calendar cal = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
    }
}
