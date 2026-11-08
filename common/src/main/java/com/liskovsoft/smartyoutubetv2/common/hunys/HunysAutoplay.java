package com.liskovsoft.smartyoutubetv2.common.hunys;

import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerConstants;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;

/**
 * Redare automată: pornit = următorul clip pornește singur, oprit = clipul se oprește la final.<br/>
 * Autoplay: on = the next video starts by itself, off = the video pauses at the end.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public final class HunysAutoplay {
    private HunysAutoplay() {
    }

    public static boolean isOn(PlayerData playerData) {
        int mode = playerData.getPlaybackMode();
        return mode == PlayerConstants.PLAYBACK_MODE_ALL || mode == PlayerConstants.PLAYBACK_MODE_SHUFFLE
                || mode == PlayerConstants.PLAYBACK_MODE_LIST || mode == PlayerConstants.PLAYBACK_MODE_REVERSE_LIST;
    }

    public static void set(PlayerData playerData, boolean on) {
        playerData.setPlaybackMode(on ? PlayerConstants.PLAYBACK_MODE_ALL : PlayerConstants.PLAYBACK_MODE_PAUSE);
    }
}
