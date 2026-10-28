package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.complexcardview;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.TypedValue;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.hunys.HunysData;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.embedplayer.EmbedPlayerView;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.lang.ref.WeakReference;

public class ComplexImageView extends RelativeLayout {
    private static final long PLAYER_START_DELAY_MS = 2_000;
    private ImageView mMainImage;
    private ImageView mPreviewImage;
    private EmbedPlayerView mPreviewPlayer;
    private FrameLayout mPreviewContainer;
    private ProgressBar mProgressBar;
    private TextView mBadgeText;
    private ViewGroup mProgressContainer;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private Runnable mCreateAndStartPlayer;
    private WeakReference<Video> mVideo;
    private boolean mIsPreviewUrlEnabled;
    private boolean mMute;
    // Hunys mod: buton de sunet și oprire după 60 s / sound button and stop after 60 s
    private ImageView mHunysSound;
    private boolean mHunysSoundFocused;
    private Runnable mHunysStopPreview;

    public ComplexImageView(Context context) {
        super(context);
        init();
    }

    public ComplexImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ComplexImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.text_badge_image_view, this);
        mMainImage = findViewById(R.id.main_image);
        mBadgeText = findViewById(R.id.extra_text_badge);
        mProgressBar = findViewById(R.id.clip_progress);
        mProgressContainer = findViewById(R.id.clip_info);
        mPreviewContainer = findViewById(R.id.preview_container);
    }

    /**
     * Main trick is to apply visibility to child image views<br/>
     * See: androidx.leanback.widget.BaseCardView#findChildrenViews()
     */
    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (mMainImage != null) {
            mMainImage.setVisibility(visibility);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        if (!hasWindowFocus) {
            stopPlayback();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        stopPlayback();
    }

    /**
     * Sets the badge text.
     */
    public void setBadgeText(String text) {
        if (mBadgeText == null) {
            return;
        }
        if (text != null) {
            mBadgeText.setText(text);
            mBadgeText.setVisibility(View.VISIBLE);
        } else {
            mBadgeText.setVisibility(View.INVISIBLE);
        }
    }

    public void setBadgeColor(int color) {
        if (mBadgeText == null) {
            return;
        }

        mBadgeText.setBackgroundColor(color);
    }

    /**
     * Sets the progress.
     */
    public void setProgress(int percent) {
        if (mProgressBar == null) {
            return;
        }
        if (percent > 0) {
            mProgressBar.setProgress(percent);
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    public void setPreviewUrlEnabled(boolean enabled) {
        mIsPreviewUrlEnabled = enabled;
    }

    public void setPreview(Video video) {
        if (video != null) {
            mVideo = new WeakReference<>(video);
        }
    }

    public void setMute(boolean mute) {
        mMute = mute;
    }

    public void startPlayback() {
        if (getVideo() == null) {
            return;
        }

        if (mIsPreviewUrlEnabled) {
            if (getVideo().previewUrl == null)
                return;

            if (mPreviewImage == null) {
                mPreviewImage = new ImageView(getContext());
                mPreviewImage.setScaleType(ScaleType.CENTER_CROP);
                mPreviewImage.setAdjustViewBounds(true);
                mPreviewContainer.addView(mPreviewImage, new FrameLayout.LayoutParams(mPreviewWidth, mPreviewHeight));
                mPreviewContainer.setVisibility(View.VISIBLE);
            }

            Glide.with(getContext().getApplicationContext()) // FIX: "You cannot start a load for a destroyed activity"
                    .load(getVideo().previewUrl)
                    .apply(ViewUtil.glideOptions())
                    .into(mPreviewImage);
        } else if (getVideo().videoId != null) {
            if (mCreateAndStartPlayer == null) {
                mCreateAndStartPlayer = this::createAndStartPlayer;
            }

            Utils.postDelayed(mCreateAndStartPlayer, PLAYER_START_DELAY_MS);
        }
    }

    private void createAndStartPlayer() {
        if (getVideo() == null) {
            return;
        }

        boolean isHunys = HunysData.isHunysBuild(getContext());

        if (isHunys) {
            mMute = HunysData.instance(getContext()).isPreviewMuted();
        }

        if (mPreviewPlayer == null) {
            mPreviewPlayer = new EmbedPlayerView(getContext());
            mPreviewPlayer.setQuality(Math.min(mPreviewWidth, mPreviewHeight) < 300 ? EmbedPlayerView.QUALITY_LOW : EmbedPlayerView.QUALITY_NORMAL);
            mPreviewPlayer.setUseController(false);
            mPreviewPlayer.setMute(mMute);
            mPreviewPlayer.setBackgroundColor(Color.BLACK);
            mPreviewContainer.addView(mPreviewPlayer, new FrameLayout.LayoutParams(mPreviewWidth, mPreviewHeight));
            mPreviewContainer.setVisibility(View.VISIBLE);
        }

        mPreviewPlayer.openVideo(getVideo());

        if (isHunys) {
            // Butonul de sunet de pe card a fost scos: telecomanda are deja buton de oprit sonorul
            // The card sound button was removed: the remote already has a mute button

            int seconds = HunysData.instance(getContext()).getPreviewSeconds();

            if (seconds > 0) {
                if (mHunysStopPreview == null) {
                    mHunysStopPreview = () -> stopPlayback(false);
                }
                Utils.removeCallbacks(mHunysStopPreview);
                Utils.postDelayed(mHunysStopPreview, seconds * 1_000L);
            }
        }
    }

    // ---- Hunys mod: butonul de sunet / sound button

    public boolean isHunysPreviewActive() {
        return mPreviewPlayer != null && mHunysSound != null && mHunysSound.getVisibility() == View.VISIBLE;
    }

    public boolean isHunysSoundFocused() {
        return mHunysSoundFocused;
    }

    public void setHunysSoundFocused(boolean focused) {
        mHunysSoundFocused = focused;
        updateHunysSound();
    }

    public void toggleHunysMute() {
        mMute = !mMute;
        HunysData.instance(getContext()).setPreviewMuted(mMute);

        if (mPreviewPlayer != null) {
            mPreviewPlayer.setMute(mMute);
        }

        updateHunysSound();
    }

    private void showHunysSound(boolean show) {
        if (!show) {
            mHunysSoundFocused = false;
            if (mHunysSound != null) {
                mHunysSound.setVisibility(View.GONE);
            }
            return;
        }

        if (mHunysSound == null) {
            int size = dp(34);
            mHunysSound = new ImageView(getContext());
            mHunysSound.setScaleType(ScaleType.CENTER_INSIDE);
            mHunysSound.setPadding(dp(7), dp(7), dp(7), dp(7));
            mHunysSound.setFocusable(false);
            LayoutParams params = new LayoutParams(size, size);
            params.addRule(ALIGN_PARENT_BOTTOM);
            params.addRule(ALIGN_PARENT_LEFT);
            params.leftMargin = dp(8);
            params.bottomMargin = dp(10);
            addView(mHunysSound, params);
        }

        mHunysSound.setVisibility(View.VISIBLE);
        mHunysSound.bringToFront();
        updateHunysSound();
    }

    private void updateHunysSound() {
        if (mHunysSound == null) {
            return;
        }

        mHunysSound.setImageResource(mMute ? R.drawable.hunys_ic_volume_off : R.drawable.hunys_ic_volume_on);
        mHunysSound.setBackgroundResource(mHunysSoundFocused ? R.drawable.hunys_sound_badge_focused : R.drawable.hunys_sound_badge);
        mHunysSound.setColorFilter(mHunysSoundFocused ? Color.BLACK : Color.WHITE, PorterDuff.Mode.SRC_IN);
        float scale = mHunysSoundFocused ? 1.15f : 1f;
        mHunysSound.setScaleX(scale);
        mHunysSound.setScaleY(scale);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    public void stopPlayback() {
        stopPlayback(false);
    }

    public void stopPlayback(boolean stopImmediately) {
        if (mHunysStopPreview != null) {
            Utils.removeCallbacks(mHunysStopPreview);
        }
        showHunysSound(false);

        if (getVideo() == null) {
            return;
        }

        if (mIsPreviewUrlEnabled) {
            if (getVideo().previewUrl == null)
                return;

            if (mPreviewImage != null) {
                mPreviewContainer.removeView(mPreviewImage);
                mPreviewContainer.setVisibility(View.GONE);
                mPreviewImage.setImageDrawable(null);
                Glide.with(getContext().getApplicationContext()).clear(mPreviewImage);
                mPreviewImage = null;
            }
        } else if (getVideo().videoId != null) {
            Utils.removeCallbacks(mCreateAndStartPlayer);

            if (mPreviewPlayer != null) {
                mPreviewContainer.setVisibility(View.GONE);
                if (stopImmediately) {
                    mPreviewPlayer.finish();
                    mPreviewContainer.removeView(mPreviewPlayer);
                } else {
                    EmbedPlayerView epv = mPreviewPlayer;
                    epv.setMute(true);
                    Utils.postDelayed(() -> {
                        epv.finish();
                        mPreviewContainer.removeView(epv);
                    }, 500);
                }
                mPreviewPlayer = null;
            }
        }
    }

    public void setMainImageAdjustViewBounds(boolean adjustViewBounds) {
        if (mPreviewImage != null) {
            mPreviewImage.setAdjustViewBounds(adjustViewBounds);
        }
    }

    public void setMainImageScaleType(ScaleType scaleType) {
        if (mPreviewImage != null) {
            mPreviewImage.setScaleType(scaleType);
        }
    }

    public void setMainImageDimensions(int width, int height) {
        setPreviewDimensions(width, height);
        setProgressDimensions(width, height);
    }

    private void setProgressDimensions(int width, int height) {
        ViewGroup.LayoutParams lp = mProgressContainer.getLayoutParams();
        lp.width = width;
        mProgressContainer.setLayoutParams(lp);
    }

    private void setPreviewDimensions(int width, int height) {
        mPreviewWidth = width;
        mPreviewHeight = height;
        //ViewGroup.LayoutParams lp = mPreviewImage.getLayoutParams();
        //lp.width = width;
        //lp.height = height;
        //mPreviewImage.setLayoutParams(lp);
    }

    private Video getVideo() {
        return mVideo != null ? mVideo.get() : null;
    }
}
