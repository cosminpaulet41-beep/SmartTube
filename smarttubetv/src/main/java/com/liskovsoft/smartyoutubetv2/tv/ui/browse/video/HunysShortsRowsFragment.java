package com.liskovsoft.smartyoutubetv2.tv.ui.browse.video;

/**
 * Fila Shorts în modul Hunys: un singur rând de Shorts mari, doar cu titlul „Shorts” deasupra.<br/>
 * The Shorts tab in the Hunys mod: a single row of big Shorts, with only the "Shorts" title above.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public class HunysShortsRowsFragment extends VideoRowsFragment {
    @Override
    protected boolean isHunysShortsOnly() {
        return true;
    }
}
