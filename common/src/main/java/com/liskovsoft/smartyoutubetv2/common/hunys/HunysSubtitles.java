package com.liskovsoft.smartyoutubetv2.common.hunys;

import android.content.Context;

import com.liskovsoft.smartyoutubetv2.common.exoplayer.other.SubtitleManager.SubtitleStyle;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;

import java.util.List;

/**
 * Subtitrări mai vizibile: alb pe fundal semi-transparent, contur, puțin mai mari.<br/>
 * More visible subtitles: white on a semi-transparent box, outlined, slightly larger.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public final class HunysSubtitles {
    private static final int STYLE_WHITE_SEMI_TRANSPARENT = 1;
    private static final float VISIBLE_SCALE = 1.2f;

    private HunysSubtitles() {
    }

    public static void applyVisibleStyle(Context context) {
        PlayerData playerData = PlayerData.instance(context);
        List<SubtitleStyle> styles = playerData.getSubtitleStyles();

        if (styles != null && styles.size() > STYLE_WHITE_SEMI_TRANSPARENT) {
            playerData.setSubtitleStyle(styles.get(STYLE_WHITE_SEMI_TRANSPARENT));
        }

        playerData.setSubtitleScale(VISIBLE_SCALE);
    }

    /**
     * Setări implicite la prima pornire / defaults on first start.
     */
    public static void applyDefaultsOnce(Context context) {
        if (context == null || !HunysData.isHunysBuild(context)) {
            return;
        }

        HunysData data = HunysData.instance(context);

        if (data.isDefaultsApplied()) {
            return;
        }

        try {
            applyVisibleStyle(context);
        } catch (RuntimeException e) {
            // keep the original subtitle settings
        }

        data.setDefaultsApplied();
    }
}
