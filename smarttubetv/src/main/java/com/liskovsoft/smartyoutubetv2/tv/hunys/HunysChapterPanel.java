package com.liskovsoft.smartyoutubetv2.tv.hunys;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.hunys.HunysChapters;
import com.liskovsoft.smartyoutubetv2.common.hunys.HunysChapters.Chapter;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.misc.SeekBar;

import java.util.List;
import java.util.Locale;

/**
 * Rândul de repere de deasupra barei (varianta A aleasă): ↑ de pe bară îl deschide,
 * ← → alegi reperul, OK sari la el, ↓ înapoi la bară.<br/>
 * Chapter row above the progress bar (chosen variant A): up from the bar opens it,
 * left/right choose a chapter, OK jumps to it, down goes back to the bar.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public final class HunysChapterPanel {
    private static final int TEXT_DARK = 0xFF0B1A33;
    private final View mPanel;
    private final LinearLayout mRow;
    private final View mDimView;
    private final SeekBar mSeekBar;
    private int mBuiltVersion = -1;
    private long mDurationMs;
    private final Runnable mChaptersChanged = this::onChaptersChanged;
    private final Runnable mCheckFocus = this::checkFocus;

    public HunysChapterPanel(View root, SeekBar seekBar) {
        mPanel = root.findViewById(R.id.hunys_chapters_panel);
        mRow = root.findViewById(R.id.hunys_chapters_row);
        mDimView = root.findViewById(R.id.hunys_controls_row);
        mSeekBar = seekBar;

        mPanel.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                HunysChapters.addListener(mChaptersChanged);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                HunysChapters.removeListener(mChaptersChanged);
                close();
            }
        });
    }

    public static boolean isAvailable(View root) {
        return root.findViewById(R.id.hunys_chapters_panel) != null && root.findViewById(R.id.hunys_chapters_row) != null;
    }

    /**
     * Deschide rândul și întoarce reperul care trebuie să primească focus (sau null dacă nu sunt repere).<br/>
     * Opens the row and returns the chapter that should get focus (or null when there are no chapters).
     */
    public View open(long positionMs, long durationMs) {
        List<Chapter> chapters = HunysChapters.get();

        if (chapters.isEmpty() || durationMs <= 0) {
            return null;
        }

        mDurationMs = durationMs;

        if (mBuiltVersion != HunysChapters.getVersion() || mRow.getChildCount() != chapters.size()) {
            build(chapters);
        }

        int current = HunysChapters.indexAt(chapters, positionMs);

        for (int i = 0; i < mRow.getChildCount(); i++) {
            View now = mRow.getChildAt(i).findViewById(R.id.hunys_chapter_now);
            if (now != null) {
                now.setVisibility(i == current ? View.VISIBLE : View.GONE);
            }
        }

        mPanel.setVisibility(View.VISIBLE);
        HunysChapters.setPanelOpen(true); // playerul stă 30 s / the player stays 30 s

        if (mDimView != null) {
            mDimView.setAlpha(0.35f);
        }

        return current >= 0 && current < mRow.getChildCount() ? mRow.getChildAt(current) : null;
    }

    public void close() {
        mPanel.removeCallbacks(mCheckFocus);

        if (mPanel.getVisibility() != View.VISIBLE) {
            return;
        }

        mPanel.setVisibility(View.GONE);
        HunysChapters.setPanelOpen(false);

        if (mDimView != null) {
            mDimView.setAlpha(1f);
        }

        if (mSeekBar != null) {
            mSeekBar.setHunysHighlight(-1, -1);
        }
    }

    private void build(List<Chapter> chapters) {
        Context context = mRow.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        mRow.removeAllViews();

        int accent = HunysUi.accent(context);
        int panel = (HunysUi.panel(context) & 0x00FFFFFF) | 0xE6000000;
        ColorStateList titleColors = new ColorStateList(
                new int[][] {{android.R.attr.state_focused}, {}}, new int[] {TEXT_DARK, 0xFFFFFFFF});

        for (int i = 0; i < chapters.size(); i++) {
            Chapter chapter = chapters.get(i);
            long endMs = i + 1 < chapters.size() ? chapters.get(i + 1).startMs : mDurationMs;
            View card = inflater.inflate(R.layout.hunys_chapter_card, mRow, false);

            card.setBackground(cardBackground(context, panel, accent));
            if (Build.VERSION.SDK_INT >= 21) {
                card.setClipToOutline(true);
            }

            TextView title = card.findViewById(R.id.hunys_chapter_title);
            title.setText(chapter.title);
            title.setTextColor(titleColors);

            TextView time = card.findViewById(R.id.hunys_chapter_time);
            time.setText(formatTime(chapter.startMs));

            ImageView image = card.findViewById(R.id.hunys_chapter_image);
            if (chapter.imageUrl != null) {
                try {
                    Glide.with(context.getApplicationContext()).load(chapter.imageUrl).centerCrop().into(image);
                } catch (RuntimeException e) {
                    // fără miniatură / no thumbnail
                }
            }

            final long startMs = chapter.startMs;
            final float start = mDurationMs > 0 ? startMs / (float) mDurationMs : -1;
            final float end = mDurationMs > 0 ? Math.min(1f, endMs / (float) mDurationMs) : -1;

            card.setOnClickListener(v -> {
                close();
                PlaybackPresenter.instance(v.getContext()).setPosition(startMs);
            });

            card.setOnFocusChangeListener((v, hasFocus) -> {
                v.animate().translationY(hasFocus ? -HunysUi.dp(v.getContext(), 4) : 0).setDuration(120).start();
                if (hasFocus) {
                    mPanel.removeCallbacks(mCheckFocus);
                    HunysChapters.markPanelActivity();
                    if (mSeekBar != null) {
                        mSeekBar.setHunysHighlight(start, end);
                    }
                } else {
                    mPanel.post(mCheckFocus);
                }
            });

            mRow.addView(card);
        }

        mBuiltVersion = HunysChapters.getVersion();
    }

    private void checkFocus() {
        if (!mPanel.hasFocus()) {
            close(); // focusul a plecat din rând / focus left the row
        }
    }

    private void onChaptersChanged() {
        mBuiltVersion = -1;

        if (!mPanel.hasFocus()) {
            close();
        }
    }

    private static StateListDrawable cardBackground(Context context, int normalColor, int focusedColor) {
        float radius = HunysUi.dp(context, 12);

        GradientDrawable normal = new GradientDrawable();
        normal.setColor(normalColor);
        normal.setCornerRadius(radius);
        normal.setStroke(HunysUi.dp(context, 1), 0x1AFFFFFF);

        GradientDrawable focused = new GradientDrawable();
        focused.setColor(focusedColor);
        focused.setCornerRadius(radius);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, focused);
        states.addState(new int[] {}, normal);
        return states;
    }

    private static String formatTime(long ms) {
        long totalSec = Math.max(0, ms / 1000);
        long hours = totalSec / 3600;
        long minutes = (totalSec % 3600) / 60;
        long seconds = totalSec % 60;

        return hours > 0
                ? String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
                : String.format(Locale.US, "%d:%02d", minutes, seconds);
    }
}
