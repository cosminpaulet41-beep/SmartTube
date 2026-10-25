package com.liskovsoft.smartyoutubetv2.common.hunys;

import android.os.Handler;
import android.os.Looper;

import com.liskovsoft.mediaserviceinterfaces.data.ChapterItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reperele (capitolele) clipului curent, pentru rândul de repere din player.<br/>
 * The current video's chapters, for the player's chapter row.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public final class HunysChapters {
    public static final class Chapter {
        public final String title;
        public final long startMs;
        public final String imageUrl;

        Chapter(String title, long startMs, String imageUrl) {
            this.title = title;
            this.startMs = startMs;
            this.imageUrl = imageUrl;
        }
    }

    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static final List<Runnable> sListeners = new ArrayList<>();
    private static List<Chapter> sChapters = Collections.emptyList();
    private static int sVersion;

    private HunysChapters() {
    }

    public static void set(List<ChapterItem> items) {
        List<Chapter> chapters = new ArrayList<>();

        if (items != null) {
            for (ChapterItem item : items) {
                if (item != null) {
                    chapters.add(new Chapter(item.getTitle(), item.getStartTimeMs(), item.getCardImageUrl()));
                }
            }
        }

        sHandler.post(() -> {
            sChapters = Collections.unmodifiableList(chapters);
            sVersion++;
            for (Runnable listener : new ArrayList<>(sListeners)) {
                listener.run();
            }
        });
    }

    public static List<Chapter> get() {
        return sChapters;
    }

    /** Se schimbă la fiecare listă nouă / changes with every new list */
    public static int getVersion() {
        return sVersion;
    }

    public static void addListener(Runnable listener) {
        if (!sListeners.contains(listener)) {
            sListeners.add(listener);
        }
    }

    public static void removeListener(Runnable listener) {
        sListeners.remove(listener);
    }

    /**
     * Indexul capitolului care se redă la poziția dată / index of the chapter playing at the given position
     */
    public static int indexAt(List<Chapter> chapters, long positionMs) {
        int index = -1;

        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).startMs <= positionMs) {
                index = i;
            } else {
                break;
            }
        }

        return Math.max(index, chapters.isEmpty() ? -1 : 0);
    }
}
