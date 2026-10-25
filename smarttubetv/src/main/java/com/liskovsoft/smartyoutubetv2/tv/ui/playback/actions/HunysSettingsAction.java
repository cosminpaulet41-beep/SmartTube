package com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions;

import android.content.Context;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Action;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Butonul ⚙ din player: Calitate, Limbă audio, Viteză, Repetare, PiP, Doar sunet, Căutare, Statistici.<br/>
 * The player ⚙ button: Quality, Audio language, Speed, Repeat, PiP, Audio only, Search, Stats.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public class HunysSettingsAction extends Action {
    public HunysSettingsAction(Context context) {
        super(R.id.action_player_settings);
        setIcon(ContextCompat.getDrawable(context, R.drawable.hunys_ic_settings));
        setLabel1(context.getString(R.string.hunys_player_settings));
    }
}
