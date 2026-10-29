package com.liskovsoft.smartyoutubetv2.tv.hunys;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.PlaybackControlsRow;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.hunys.HunysDimmer;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Butoanele din player în stilul aprobat: ⏮ ▶ ⏭ în cercuri, pastile text, iconițe rotunjite pe sticlă.<br/>
 * Player buttons in the approved style: prev/play/next circles, text pills, rounded icons on glass.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public final class HunysControls {
    private static final int DARK = 0xFF111111;
    private static final int GLASS = 0x1FFFFFFF;
    private static final int PILL_GLASS = 0x29FFFFFF;

    private HunysControls() {
    }

    public static void bind(View root, View button, ImageView icon, TextView label, Action action) {
        Context context = root.getContext();
        int id = (int) action.getId();
        int index = action instanceof PlaybackControlsRow.MultiAction ? ((PlaybackControlsRow.MultiAction) action).getIndex() : 0;

        // Stare de bază / base state (views can be reused)
        setWeight(root, 0);
        root.setPadding(dp(context, 3), 0, dp(context, 3), 0);
        button.setVisibility(View.VISIBLE);
        button.setFocusable(true);
        button.setPadding(dp(context, 6), 0, dp(context, 6), 0);
        icon.setVisibility(View.VISIBLE);
        icon.setPadding(0, 0, 0, 0);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        label.setVisibility(View.GONE);
        label.setText(null);
        setLabelMargin(label, dp(context, 6));

        if (id == R.id.action_hunys_spacer) {
            setWeight(root, 1);
            button.setVisibility(View.INVISIBLE);
            button.setFocusable(false);
            button.setBackground(null);
            icon.setImageDrawable(null);
            return;
        }

        if (id == androidx.leanback.R.id.lb_control_play_pause) {
            boolean showPlay = index == PlaybackControlsRow.PlayPauseAction.INDEX_PLAY;
            root.setPadding(dp(context, 7), 0, dp(context, 7), 0);
            circle(button, dp(context, 46));
            // Se aprinde doar când e selectat, ca ⏮ și ⏭ / lights up only when focused, like prev and next
            button.setBackground(states(oval(context, GLASS, dp(context, 1), 0x1AFFFFFF), oval(context, Color.WHITE, 0, 0)));
            setIcon(icon, ContextCompat.getDrawable(context, showPlay ? R.drawable.hunys_ic_play_filled : R.drawable.hunys_ic_pause_filled),
                    dp(context, 20), whiteToDark());
            if (showPlay) {
                icon.setPadding(dp(context, 2), 0, 0, 0); // triunghiul arată centrat / visually centered triangle
            }
            return;
        }

        if (id == androidx.leanback.R.id.lb_control_skip_previous || id == androidx.leanback.R.id.lb_control_skip_next) {
            boolean prev = id == androidx.leanback.R.id.lb_control_skip_previous;
            root.setPadding(dp(context, 7), 0, dp(context, 7), 0);
            circle(button, dp(context, 34));
            button.setBackground(states(oval(context, GLASS, dp(context, 1), 0x1AFFFFFF), oval(context, Color.WHITE, 0, 0)));
            setIcon(icon, ContextCompat.getDrawable(context, prev ? R.drawable.hunys_ic_prev_filled : R.drawable.hunys_ic_next_filled),
                    dp(context, 14), whiteToDark());
            return;
        }

        if (id == R.id.action_channel) {
            // Avatarul canalului / channel avatar
            root.setPadding(0, 0, dp(context, 6), 0);
            circle(button, dp(context, 34));
            button.setBackground(states(oval(context, Color.TRANSPARENT, dp(context, 2), 0x40FFFFFF), oval(context, Color.TRANSPARENT, dp(context, 3), HunysUi.pill(context))));
            Drawable avatar = action.getIcon();
            setIcon(icon, avatar, dp(context, 30), null);
            icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return;
        }

        if (id == R.id.action_info || id == R.id.action_subscribe) {
            boolean subscribed = id == R.id.action_subscribe && index == 1;
            String text = context.getString(id == R.id.action_info ? R.string.hunys_description
                    : subscribed ? R.string.hunys_subscribed : R.string.hunys_subscribe);
            root.setPadding(dp(context, 4), 0, dp(context, 4), 0);
            setHeight(button, dp(context, 30));
            button.setMinimumWidth(0);
            button.setPadding(dp(context, 13), 0, dp(context, 13), 0);
            button.setBackground(HunysUi.glassPill(context, 15, PILL_GLASS));
            icon.setVisibility(View.GONE);
            label.setVisibility(View.VISIBLE);
            label.setText(text);
            setLabelMargin(label, 0);
            label.setTextColor(new ColorStateList(new int[][] {{android.R.attr.state_focused}, {}},
                    new int[] {HunysUi.ink(context), Color.WHITE}));
            return;
        }

        // Iconițe din dreapta / right-side icons
        root.setPadding(dp(context, 1), 0, dp(context, 1), 0);
        setHeight(button, dp(context, 30));
        button.setMinimumWidth(dp(context, 30));
        button.setBackground(states(null, rounded(context, Color.WHITE, 15)));

        boolean on = index == 1;
        int iconRes = 0;
        ColorStateList tint = whiteToDark();

        if (id == R.id.action_thumbs_up) {
            iconRes = on ? R.drawable.hunys_ic_like_filled : R.drawable.hunys_ic_like;
            String likes = likeCount(context);
            if (!TextUtils.isEmpty(likes)) {
                label.setVisibility(View.VISIBLE);
                label.setText(likes);
                label.setTextColor(whiteToDark());
                button.setPadding(dp(context, 8), 0, dp(context, 10), 0);
            }
        } else if (id == R.id.action_thumbs_down) {
            iconRes = on ? R.drawable.hunys_ic_dislike_filled : R.drawable.hunys_ic_dislike;
        } else if (id == R.id.action_player_settings) {
            iconRes = R.drawable.hunys_ic_settings;
        } else if (id == R.id.action_hunys_dim) {
            iconRes = R.drawable.hunys_ic_moon;
            if (HunysDimmer.instance(context).getEffectiveLevel() > 0) {
                // luna colorată când ecranul e întunecat / colored moon when the screen is dimmed
                tint = new ColorStateList(new int[][] {{android.R.attr.state_focused}, {}}, new int[] {DARK, HunysUi.accent(context)});
            }
        } else if (id == R.id.action_chat) {
            iconRes = R.drawable.hunys_ic_comments;
        } else if (id == R.id.action_playlist_add) {
            iconRes = on ? R.drawable.hunys_ic_playlist_added : R.drawable.hunys_ic_playlist_add;
        } else if (id == R.id.lb_control_closed_captioning) {
            iconRes = R.drawable.hunys_ic_cc;
            if (on) {
                tint = new ColorStateList(new int[][] {{android.R.attr.state_focused}, {}}, new int[] {DARK, HunysUi.accent(context)});
            }
        }

        Drawable drawable = iconRes != 0 ? ContextCompat.getDrawable(context, iconRes) : action.getIcon();
        setIcon(icon, drawable, dp(context, 18), tint);
    }

    private static String likeCount(Context context) {
        try {
            Video video = PlaybackPresenter.instance(context).getVideo();
            return video != null ? video.likeCount : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static ColorStateList whiteToDark() {
        return new ColorStateList(new int[][] {{android.R.attr.state_focused}, {}}, new int[] {DARK, Color.WHITE});
    }

    private static void setIcon(ImageView icon, Drawable drawable, int size, ColorStateList tint) {
        icon.setImageDrawable(drawable);
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        if (lp != null && (lp.width != size || lp.height != size)) {
            lp.width = size;
            lp.height = size;
            icon.setLayoutParams(lp);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            icon.setImageTintList(tint);
        }
    }

    private static void circle(View button, int size) {
        ViewGroup.LayoutParams lp = button.getLayoutParams();
        if (lp != null) {
            lp.width = size;
            lp.height = size;
            button.setLayoutParams(lp);
        }
        button.setMinimumWidth(size);
        button.setPadding(0, 0, 0, 0);
    }

    private static void setHeight(View button, int height) {
        ViewGroup.LayoutParams lp = button.getLayoutParams();
        if (lp != null) {
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.height = height;
            button.setLayoutParams(lp);
        }
    }

    private static void setWeight(View root, float weight) {
        ViewGroup.LayoutParams params = root.getLayoutParams();
        if (params instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) params;
            if (lp.weight != weight) {
                lp.weight = weight;
                lp.width = weight > 0 ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
                root.setLayoutParams(lp);
            }
        }
    }

    private static void setLabelMargin(TextView label, int margin) {
        ViewGroup.LayoutParams params = label.getLayoutParams();
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) params;
            if (lp.getMarginStart() != margin) {
                lp.setMarginStart(margin);
                label.setLayoutParams(lp);
            }
        }
    }

    private static Drawable oval(Context context, int color, int strokeWidth, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private static Drawable rounded(Context context, int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    private static Drawable states(Drawable normal, Drawable focused) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, focused);
        if (normal != null) {
            states.addState(new int[] {}, normal);
        }
        return states;
    }

    private static int dp(Context context, float value) {
        return HunysUi.dp(context, value);
    }
}
