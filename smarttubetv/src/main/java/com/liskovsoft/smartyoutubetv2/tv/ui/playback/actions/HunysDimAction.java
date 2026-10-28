package com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions;

import android.content.Context;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Action;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Butonul cu lună din player: deschide întunecarea ecranului (modul de noapte).<br/>
 * The moon button in the player: opens screen dimming (night mode).<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public class HunysDimAction extends Action {
    public HunysDimAction(Context context) {
        super(R.id.action_hunys_dim);
        setIcon(ContextCompat.getDrawable(context, R.drawable.hunys_ic_moon));
        setLabel1(context.getString(R.string.hunys_dimming));
    }
}
