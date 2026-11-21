package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.TypedValue;
import android.view.ViewGroup;

import com.liskovsoft.smartyoutubetv2.common.hunys.HunysData;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.complexcardview.ComplexImageCardView;

/**
 * Shorts mari: cardurile ocupă tot ecranul de sub titlul „Shorts” (~3,5 pe ecran).<br/>
 * Shorts-ul selectat pornește în card după 1 s, cu sunet, și se reia la final; OK deschide playerul.<br/>
 * Big Shorts: the cards fill the screen below the "Shorts" title (~3.5 on screen).<br/>
 * The selected Short starts in its card after 1 s, with sound, and starts over at the end; OK opens the player.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public class HunysBigShortsCardPresenter extends ShortsCardPresenter {
    // Titlul rândului + spațiile rândului + scrisul de sub card / row title + row paddings + text below the card
    private static final int ROW_TITLE_DP = 38;
    private static final int ROW_PADDINGS_DP = 44;
    private static final int CARD_TEXT_DP = 54;
    private static final int BOTTOM_AIR_DP = 6;
    private static Pair<Integer, Integer> sDimens;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        ViewHolder holder = super.onCreateViewHolder(parent);

        if (holder.view instanceof ComplexImageCardView) {
            ((ComplexImageCardView) holder.view).setHunysBigShort(true);
        }

        return holder;
    }

    @Override
    protected Pair<Integer, Integer> getCardDimensPx(Context context) {
        if (sDimens == null) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int screenHeight = Math.min(metrics.heightPixels, metrics.widthPixels);
            int reserved = dp(context, ROW_TITLE_DP + ROW_PADDINGS_DP + CARD_TEXT_DP + BOTTOM_AIR_DP);
            int height = Math.max(dp(context, 200), screenHeight - reserved);
            int width = Math.round(height * 9 / 16f); // format vertical / vertical format
            sDimens = new Pair<>(width, height);
        }

        return sDimens;
    }

    @Override
    protected int getCardPreviewType(Context context) {
        if (HunysData.isHunysBuild(context)) {
            HunysData data = HunysData.instance(context);
            // Fără pornire în card în modul ușor sau cu previzualizarea oprită / no in-card start in light mode or with preview off
            if (data.getPreviewSeconds() <= 0 || data.isLightModeEnabled()) {
                return MainUIData.CARD_PREVIEW_DISABLED;
            }
            return MainUIData.CARD_PREVIEW_FULL; // cu sunet / with sound
        }

        return super.getCardPreviewType(context);
    }

    private static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }
}
