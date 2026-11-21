package com.liskovsoft.smartyoutubetv2.common.hunys;

import android.content.Context;
import android.media.audiofx.DynamicsProcessing;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioListener;
import com.liskovsoft.sharedutils.mylogger.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Volum stabil: un compresor pe sunetul clipului care ridică părțile încete și coboară părțile tari,
 * ca „Stable volume” din aplicația YouTube. Se adaugă peste egalizarea de la un clip la altul.<br/>
 * Stable volume: a compressor on the video's audio that raises quiet parts and lowers loud parts,
 * like "Stable volume" in the YouTube app. It comes on top of the video-to-video leveling.<br/>
 * Egalizarea între clipuri se face cu câștigul de intrare, așa că și clipurile încete pot fi ridicate.<br/>
 * Video-to-video leveling uses the input gain, so quiet videos can be raised too.<br/>
 * Merge pe Android 9+ (DynamicsProcessing). / Works on Android 9+ (DynamicsProcessing).<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public final class HunysStableVolume implements AudioListener {
    private static final String TAG = HunysStableVolume.class.getSimpleName();
    public static final int OFF = 0;
    public static final int NORMAL = 1;
    public static final int STRONG = 2;
    private static final List<WeakReference<HunysStableVolume>> sInstances = new ArrayList<>();
    /**
     * Cât trebuie ridicat/coborât clipul curent (dB), după tăria trimisă de YouTube.<br/>
     * How much the current video must be raised/lowered (dB), from the loudness YouTube sends.
     */
    private static volatile float sClipGainDb;
    private static volatile boolean sEffectFailed;
    private final Context mContext;
    private final SimpleExoPlayer mPlayer;
    private Object mEffect; // DynamicsProcessing (API 28+)
    private int mSessionId = -1;
    private int mAppliedLevel = -1;

    public HunysStableVolume(Context context, @Nullable SimpleExoPlayer player) {
        mContext = context.getApplicationContext();
        mPlayer = player;
        synchronized (sInstances) {
            sInstances.add(new WeakReference<>(this));
        }
    }

    @Override
    public void onAudioSessionId(int audioSessionId) {
        mSessionId = audioSessionId;
        apply();
    }

    /**
     * Aplică nivelul ales pe sesiunea audio curentă / Applies the chosen level to the current audio session
     */
    private void apply() {
        if (Build.VERSION.SDK_INT < 28 || mSessionId <= 0) {
            return;
        }

        int level = HunysData.instance(mContext).getStableVolumeLevel();

        if (mEffect != null && level == mAppliedLevel) {
            return; // deja aplicat / already applied
        }

        releaseEffect();

        if (level == OFF) {
            return;
        }

        // 5.1: efectul nu e suportat pe multe aparate / 5.1 isn't supported by the effect on many devices
        if (mPlayer != null && mPlayer.getAudioFormat() != null && mPlayer.getAudioFormat().channelCount > 2) {
            return;
        }

        try {
            mEffect = create(mSessionId, level);
            mAppliedLevel = level;
            applyClipGain();
        } catch (Throwable e) { // efect indisponibil pe acest aparat / effect unavailable on this device
            Log.e(TAG, "Stable volume unavailable: %s", e.getMessage());
            sEffectFailed = true;
            mEffect = null;
            mAppliedLevel = -1;
        }
    }

    private void applyClipGain() {
        if (mEffect == null || Build.VERSION.SDK_INT < 28) {
            return;
        }

        try {
            ((DynamicsProcessing) mEffect).setInputGainAllChannelsTo(sClipGainDb);
        } catch (Throwable e) {
            // ignore
        }
    }

    @RequiresApi(28)
    private static DynamicsProcessing create(int sessionId, int level) {
        boolean strong = level == STRONG;

        DynamicsProcessing.Config config = new DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_TIME_RESOLUTION,
                2, // canale / channels
                false, 0, // fără egalizator înainte / no pre-EQ
                true, 1, // compresor pe o singură bandă / single-band compressor
                false, 0, // fără egalizator după / no post-EQ
                true) // limitator / limiter
                .build();

        DynamicsProcessing effect = new DynamicsProcessing(0, sessionId, config);

        // Peste prag, sunetul e comprimat; câștigul de la final ridică părțile încete.
        // Above the threshold the sound is compressed; the final gain lifts quiet parts.
        DynamicsProcessing.MbcBand band = new DynamicsProcessing.MbcBand(
                true,
                20_000f, // toată gama de frecvențe / full frequency range
                strong ? 10f : 20f, // atac (ms) / attack (ms)
                strong ? 250f : 400f, // revenire (ms) / release (ms)
                strong ? 5f : 3f, // raport / ratio
                strong ? -32f : -30f, // prag (dB) / threshold (dB)
                6f, // genunchi (dB) / knee (dB)
                -60f, // sub -60 dB nu ridicăm zgomotul de fond / below -60 dB background noise isn't raised
                2f, // expandare sub prag / expansion below the gate
                0f, // câștig înainte / pre-gain
                // nivelul obișnuit (~-18 dB) rămâne la fel, părțile încete urcă / usual level (~-18 dB) stays, quiet parts go up
                strong ? 11f : 8f); // câștig după / post-gain (makeup)
        effect.setMbcBandAllChannelsTo(0, band);

        // Limitatorul oprește distorsiunile la vârfuri / The limiter stops clipping on peaks
        DynamicsProcessing.Limiter limiter = new DynamicsProcessing.Limiter(true, true, 0, 1f, 60f, 10f, -2f, 0f);
        effect.setLimiterAllChannelsTo(limiter);
        effect.setInputGainAllChannelsTo(sClipGainDb);
        effect.setEnabled(true);

        return effect;
    }

    private void releaseEffect() {
        if (mEffect != null && Build.VERSION.SDK_INT >= 28) {
            try {
                ((DynamicsProcessing) mEffect).release();
            } catch (RuntimeException e) {
                // ignore
            }
        }
        mEffect = null;
        mAppliedLevel = -1;
    }

    public void release() {
        releaseEffect();
        mSessionId = -1;
        synchronized (sInstances) {
            Iterator<WeakReference<HunysStableVolume>> it = sInstances.iterator();
            while (it.hasNext()) {
                HunysStableVolume item = it.next().get();
                if (item == null || item == this) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Apelat când se schimbă setarea: aplică imediat pe clipurile care rulează.<br/>
     * Called when the setting changes: applies right away to playing videos.
     */
    public static void onLevelChanged() {
        synchronized (sInstances) {
            Iterator<WeakReference<HunysStableVolume>> it = sInstances.iterator();
            while (it.hasNext()) {
                HunysStableVolume item = it.next().get();
                if (item == null) {
                    it.remove();
                } else {
                    item.apply();
                }
            }
        }
    }

    /**
     * Volumul stabil e pornit? / Is stable volume on?
     */
    public static boolean isOn(Context context) {
        return HunysData.instance(context).getStableVolumeLevel() != OFF;
    }

    /**
     * Egalizarea între clipuri se poate face prin efect (Android 9+ și efectul merge pe aparat)?<br/>
     * Can video-to-video leveling be done through the effect (Android 9+ and the effect works on the device)?
     */
    public static boolean canLevelWithEffect() {
        return Build.VERSION.SDK_INT >= 28 && !sEffectFailed;
    }

    /**
     * Setează tăria clipului curent (loudnessDb de la YouTube; NaN = necunoscută).<br/>
     * Sets the current video's loudness (YouTube's loudnessDb; NaN = unknown).
     */
    public static void setClipLoudness(Context context, float loudnessDb) {
        float gainDb = 0;

        if (!Float.isNaN(loudnessDb)) {
            // clipurile tari coboară până la -20 dB; cele încete urcă până la +6 dB (Puternic: +9 dB)
            // loud videos go down to -20 dB; quiet ones go up to +6 dB (Strong: +9 dB)
            float maxRaise = HunysData.instance(context).getStableVolumeLevel() == STRONG ? 9f : 6f;
            gainDb = Math.max(-20f, Math.min(maxRaise, -loudnessDb));
        }

        if (gainDb == sClipGainDb) {
            return;
        }

        sClipGainDb = gainDb;

        synchronized (sInstances) {
            Iterator<WeakReference<HunysStableVolume>> it = sInstances.iterator();
            while (it.hasNext()) {
                HunysStableVolume item = it.next().get();
                if (item == null) {
                    it.remove();
                } else {
                    item.applyClipGain();
                }
            }
        }
    }
}
