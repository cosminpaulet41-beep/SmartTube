package com.liskovsoft.smartyoutubetv2.common.hunys;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.widget.Toast;

import com.liskovsoft.smartyoutubetv2.common.R;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Contor de date: cât internet a folosit aplicația, pe fiecare zi.<br/>
 * Data counter: how much internet the app used, for every day.<br/>
 * Folosește contoarele sistemului pentru aplicația noastră (se resetează la repornirea proiectorului,
 * de aceea adunăm diferențele). / Uses the system counters for our app (they reset on reboot, so we add up the deltas).<br/>
 * Nu pierde date când proiectorul e scos din priză: dacă data sistemului e greșită (ceasul pornește din urmă
 * până vine ora de pe internet), traficul stă deoparte și se adaugă când data e din nou corectă.<br/>
 * Doesn't lose data when the projector is unplugged: if the system date is wrong (the clock starts in the past
 * until the internet time arrives), traffic is held aside and added once the date is right again.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public final class HunysTraffic {
    private static final String PREFS_NAME = "hunys_traffic";
    private static final String K_LAST_RAW = "last_raw";
    private static final String K_DAYS = "days"; // "20261120=123456,20261121=..."
    private static final String K_TOTAL = "total";
    private static final String K_FIRST_DAY = "first_day";
    private static final String K_PENDING = "pending";
    private static final String K_LEGACY_MONTH = "legacy_month";
    private static final String K_LEGACY_BYTES = "legacy_bytes";
    private static final String K_WARNED_MONTH = "warned_month";
    private static final String K_MIGRATED = "migrated_v1";
    // Vechile chei (în fișierul HunysData) / the old keys (in the HunysData file)
    private static final String OLD_LAST_RAW = "traffic_last_raw";
    private static final String OLD_DAY_KEY = "traffic_day_key";
    private static final String OLD_DAY_BYTES = "traffic_day_bytes";
    private static final String OLD_MONTH_KEY = "traffic_month_key";
    private static final String OLD_MONTH_BYTES = "traffic_month_bytes";
    private static final int MIN_VALID_YEAR = 2025;
    private static final int KEEP_DAYS = 400;
    private static final long COMMIT_INTERVAL_MS = 5 * 60_000;
    public static final int CHART_DAYS = 30;
    private static TreeMap<Integer, Long> sDays;
    private static long sLastCommitMs;

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

        SharedPreferences prefs = prefs(context);
        migrate(context, prefs);

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

        TreeMap<Integer, Long> days = days(prefs);
        long pending = prefs.getLong(K_PENDING, 0);
        int today = todayIfValid(days);
        boolean dayAdded = false;

        SharedPreferences.Editor editor = prefs.edit().putLong(K_LAST_RAW, raw);

        if (today == 0) {
            // Data e greșită: ține deoparte / the date is wrong: hold it aside
            pending += delta;
            editor.putLong(K_PENDING, pending);
        } else {
            long add = delta + pending;

            if (add > 0 || !days.containsKey(today)) {
                Long current = days.get(today);
                dayAdded = current == null;
                days.put(today, (current != null ? current : 0) + add);
                prune(days, today);
                editor.putString(K_DAYS, encode(days));
            }

            if (pending > 0) {
                editor.putLong(K_PENDING, 0);
            }

            if (prefs.getInt(K_FIRST_DAY, 0) == 0) {
                editor.putInt(K_FIRST_DAY, today);
            }
        }

        if (delta > 0) {
            editor.putLong(K_TOTAL, prefs.getLong(K_TOTAL, 0) + delta);
        }

        // Scriem pe loc din când în când, ca să nu se piardă la scoaterea din priză
        // Write synchronously now and then, so nothing is lost when unplugged
        long now = SystemClock.elapsedRealtime();
        if (dayAdded || sLastCommitMs == 0 || now - sLastCommitMs >= COMMIT_INTERVAL_MS) {
            editor.commit();
            sLastCommitMs = now;
        } else {
            editor.apply();
        }

        checkLimit(context, prefs);
    }

    public static synchronized long getTodayBytes(Context context) {
        SharedPreferences prefs = prefs(context);
        TreeMap<Integer, Long> days = days(prefs);
        int today = todayIfValid(days);

        if (today == 0) {
            // data greșită: ultima zi + ce stă deoparte / wrong date: last day + what's held aside
            Long last = days.isEmpty() ? null : days.lastEntry().getValue();
            return (last != null ? last : 0) + prefs.getLong(K_PENDING, 0);
        }

        Long value = days.get(today);
        return value != null ? value : 0;
    }

    public static synchronized long getMonthBytes(Context context) {
        SharedPreferences prefs = prefs(context);
        TreeMap<Integer, Long> days = days(prefs);
        int today = todayIfValid(days);
        long extra = 0;

        if (today == 0) {
            today = days.isEmpty() ? 0 : days.lastKey();
            extra = prefs.getLong(K_PENDING, 0);
        }

        int month = today / 100;
        long sum = extra;

        for (Map.Entry<Integer, Long> entry : days.subMap(month * 100, month * 100 + 100).entrySet()) {
            sum += entry.getValue();
        }

        if (month != 0 && prefs.getInt(K_LEGACY_MONTH, 0) == month) {
            sum += prefs.getLong(K_LEGACY_BYTES, 0);
        }

        return sum;
    }

    /**
     * Media lunară: totalul de la început ÷ zilele de la prima zi × 30.<br/>
     * Monthly average: the total since start ÷ days since the first day × 30.
     */
    public static synchronized long getMonthlyAverageBytes(Context context) {
        SharedPreferences prefs = prefs(context);
        TreeMap<Integer, Long> days = days(prefs);
        long total = prefs.getLong(K_TOTAL, 0);
        int first = prefs.getInt(K_FIRST_DAY, 0);
        int today = todayIfValid(days);

        if (today == 0) {
            today = days.isEmpty() ? 0 : days.lastKey();
        }

        if (total <= 0 || first == 0 || today == 0) {
            return 0;
        }

        long spanDays = Math.max(1, daysBetween(first, today) + 1);
        return Math.round(total / (double) spanDays * 30);
    }

    /**
     * Ultimele {@link #CHART_DAYS} zile, ultima fiind azi.<br/>
     * The last {@link #CHART_DAYS} days, the last one being today.
     */
    public static synchronized long[] getLastDays(Context context) {
        SharedPreferences prefs = prefs(context);
        TreeMap<Integer, Long> days = days(prefs);
        long[] result = new long[CHART_DAYS];
        Calendar cal = chartEndDay(context);

        for (int i = CHART_DAYS - 1; i >= 0; i--) {
            Long value = days.get(key(cal));
            result[i] = value != null ? value : 0;
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }

        if (todayIfValid(days) == 0) {
            result[CHART_DAYS - 1] += prefs.getLong(K_PENDING, 0);
        }

        return result;
    }

    /**
     * Ziua cu care se termină graficul (azi sau, cu data greșită, ultima zi știută).<br/>
     * The day the chart ends with (today or, with a wrong date, the last known day).
     */
    public static synchronized Calendar chartEndDay(Context context) {
        TreeMap<Integer, Long> days = days(prefs(context));
        int today = todayIfValid(days);

        if (today == 0 && !days.isEmpty()) {
            return fromKey(days.lastKey());
        }

        return Calendar.getInstance();
    }

    public static synchronized void reset(Context context) {
        prefs(context).edit()
                .putString(K_DAYS, "")
                .putLong(K_TOTAL, 0)
                .putInt(K_FIRST_DAY, 0)
                .putLong(K_PENDING, 0)
                .putInt(K_LEGACY_MONTH, 0)
                .putLong(K_LEGACY_BYTES, 0)
                .putInt(K_WARNED_MONTH, 0)
                .commit();
        sDays = new TreeMap<>();
    }

    /**
     * Ex: 486 MB, 1,86 GB, 11,3 GB
     */
    public static String format(long bytes) {
        double mb = bytes / (1024.0 * 1024.0);

        if (mb < 1000) {
            return String.format(Locale.getDefault(), "%.0f MB", mb);
        }

        double gb = mb / 1024.0;

        return String.format(Locale.getDefault(), gb < 10 ? "%.2f GB" : "%.1f GB", gb);
    }

    private static void checkLimit(Context context, SharedPreferences prefs) {
        HunysData data = HunysData.instance(context);
        int limitMb = data.getDataLimitMb();

        if (!data.isDataWarnEnabled() || limitMb <= 0) {
            return;
        }

        int today = todayIfValid(days(prefs));

        if (today == 0) {
            return;
        }

        int month = today / 100;
        long monthBytes = getMonthBytes(context);

        if (monthBytes >= limitMb * 1024L * 1024L && prefs.getInt(K_WARNED_MONTH, 0) != month) {
            prefs.edit().putInt(K_WARNED_MONTH, month).apply();
            String message = context.getString(R.string.hunys_data_warning, format(monthBytes), format(limitMb * 1024L * 1024L));
            try {
                Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_LONG).show();
            } catch (RuntimeException e) {
                // no looper: skip the toast
            }
        }
    }

    /**
     * Mută contorul vechi (doar azi + luna) în istoricul nou / moves the old counter (only today + month) into the new history
     */
    private static void migrate(Context context, SharedPreferences prefs) {
        if (prefs.getBoolean(K_MIGRATED, false)) {
            return;
        }

        SharedPreferences old = HunysData.instance(context).getPrefs();
        SharedPreferences.Editor editor = prefs.edit().putBoolean(K_MIGRATED, true);

        long lastRaw = old.getLong(OLD_LAST_RAW, -1);
        if (lastRaw >= 0) {
            editor.putLong(K_LAST_RAW, lastRaw);
        }

        int day = parseKey(old.getString(OLD_DAY_KEY, ""));
        long dayBytes = old.getLong(OLD_DAY_BYTES, 0);
        int month = parseKey(old.getString(OLD_MONTH_KEY, "") + "-01") / 100;
        long monthBytes = old.getLong(OLD_MONTH_BYTES, 0);
        long total = 0;

        if (day != 0 && dayBytes > 0 && fromKey(day).get(Calendar.YEAR) >= MIN_VALID_YEAR) {
            TreeMap<Integer, Long> days = days(prefs);
            days.put(day, dayBytes);
            editor.putString(K_DAYS, encode(days));
            editor.putInt(K_FIRST_DAY, day);
            total += dayBytes;
        }

        if (month != 0 && monthBytes > 0 && month / 100 >= MIN_VALID_YEAR) {
            // restul lunii, fără zile cunoscute: intră la lună, nu în grafic
            // the rest of the month, with unknown days: counts for the month, not in the chart
            long rest = day / 100 == month ? Math.max(0, monthBytes - dayBytes) : monthBytes;
            if (rest > 0) {
                editor.putInt(K_LEGACY_MONTH, month);
                editor.putLong(K_LEGACY_BYTES, rest);
                total += rest;
                if (prefs.getInt(K_FIRST_DAY, 0) == 0 && day == 0) {
                    editor.putInt(K_FIRST_DAY, month * 100 + 1);
                }
            }
        }

        editor.putLong(K_TOTAL, total);
        editor.commit();
    }

    /**
     * @return ziua de azi (aaaallzz) sau 0 dacă data sistemului pare greșită / today (yyyymmdd) or 0 if the system date looks wrong
     */
    private static int todayIfValid(TreeMap<Integer, Long> days) {
        Calendar now = Calendar.getInstance();

        if (now.get(Calendar.YEAR) < MIN_VALID_YEAR) {
            return 0;
        }

        // Ceasul pornit de la data firmware-ului / clock started from the firmware date
        if (Build.TIME > 0 && now.getTimeInMillis() < Build.TIME + 36L * 3_600_000) {
            return 0;
        }

        int today = key(now);

        // Ceasul a dat înapoi față de ultima zi înregistrată (până la o săptămână)
        // The clock went back from the last recorded day (up to a week)
        if (!days.isEmpty() && today < days.lastKey() && daysBetween(today, days.lastKey()) <= 7) {
            return 0;
        }

        return today;
    }

    private static void prune(TreeMap<Integer, Long> days, int today) {
        Calendar cal = fromKey(today);
        cal.add(Calendar.DAY_OF_MONTH, -KEEP_DAYS);
        int oldest = key(cal);

        while (!days.isEmpty() && days.firstKey() < oldest) {
            days.pollFirstEntry();
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static TreeMap<Integer, Long> days(SharedPreferences prefs) {
        if (sDays == null) {
            sDays = decode(prefs.getString(K_DAYS, ""));
        }

        return sDays;
    }

    private static TreeMap<Integer, Long> decode(String value) {
        TreeMap<Integer, Long> result = new TreeMap<>();

        if (value == null || value.isEmpty()) {
            return result;
        }

        for (String item : value.split(",")) {
            int eq = item.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            try {
                result.put(Integer.parseInt(item.substring(0, eq)), Long.parseLong(item.substring(eq + 1)));
            } catch (NumberFormatException e) {
                // skip
            }
        }

        return result;
    }

    private static String encode(TreeMap<Integer, Long> days) {
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<Integer, Long> entry : days.entrySet()) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }

        return builder.toString();
    }

    private static int key(Calendar cal) {
        return cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH);
    }

    public static Calendar fromKey(int key) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(key / 10000, (key / 100) % 100 - 1, key % 100, 12, 0, 0);
        return cal;
    }

    /**
     * "2026-11-20" → 20261120 (0 dacă nu se poate / 0 if it can't)
     */
    private static int parseKey(String value) {
        if (value == null) {
            return 0;
        }

        String[] parts = value.split("-");

        if (parts.length < 3) {
            return 0;
        }

        try {
            return Integer.parseInt(parts[0]) * 10000 + Integer.parseInt(parts[1]) * 100 + Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long daysBetween(int fromKey, int toKey) {
        long from = fromKey(fromKey).getTimeInMillis();
        long to = fromKey(toKey).getTimeInMillis();
        return Math.round((to - from) / 86_400_000.0);
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
}
