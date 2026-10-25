package com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions;

import androidx.leanback.widget.Action;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Spațiu flexibil între grupul din stânga și cel din dreapta (nu primește focus).<br/>
 * Flexible gap between the left and right button groups (never focused).<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public class HunysSpacerAction extends Action {
    public HunysSpacerAction() {
        super(R.id.action_hunys_spacer);
    }
}
