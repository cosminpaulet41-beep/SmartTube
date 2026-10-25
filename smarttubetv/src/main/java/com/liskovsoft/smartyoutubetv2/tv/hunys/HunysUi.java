package com.liskovsoft.smartyoutubetv2.tv.hunys;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.app.HeadersSupportFragment;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;

import com.liskovsoft.smartyoutubetv2.common.hunys.HunysData;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Aspectul modului Hunys: bara laterală rotunjită, pastile, culori din temă.<br/>
 * Hunys mod look: rounded floating sidebar, pills, theme colors.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public final class HunysUi {
    private static final int TEXT_NORMAL = 0xE6FFFFFF;
    private static boolean sMemoryCategorySet;
    private static boolean sRail;
    // Bara strânsă: panou de 48dp, la 6dp de margine / collapsed rail: 48dp panel, 6dp from the edge
    private static final int RAIL_PANEL_DP = 48;
    private static final int RAIL_GAP_DP = 6;
    private static final int PANEL_MARGIN_DP = 10;
    private static final BrowseSupportFragment.HunysHeadersListener RAIL_LISTENER =
            (headersView, shown) -> setSidebarRail(headersView, !shown);

    private HunysUi() {
    }

    public static boolean isHunys(Context context) {
        return HunysData.isHunysBuild(context);
    }

    public static boolean isLightMode(Context context) {
        return isHunys(context) && HunysData.instance(context).isLightModeEnabled();
    }

    /**
     * Mod ușor: imagini pe 16 biți și cache mai mic în memorie (proiector cu 1 GB RAM).<br/>
     * Light mode: 16-bit images and a smaller memory cache (projector with 1 GB RAM).
     */
    public static RequestOptions lightOptions(Context context) {
        if (!isLightMode(context)) {
            return new RequestOptions();
        }

        if (!sMemoryCategorySet) {
            sMemoryCategorySet = true;
            try {
                Glide.get(context.getApplicationContext()).setMemoryCategory(MemoryCategory.LOW);
            } catch (RuntimeException e) {
                // keep default memory category
            }
        }

        return new RequestOptions().format(DecodeFormat.PREFER_RGB_565);
    }

    public static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }

    public static int themeColor(Context context, int attr, int fallback) {
        TypedValue value = new TypedValue();

        if (context.getTheme().resolveAttribute(attr, value, true)) {
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return value.data;
            }

            if (value.resourceId != 0) {
                try {
                    return context.getResources().getColor(value.resourceId);
                } catch (Resources.NotFoundException e) {
                    return fallback;
                }
            }
        }

        return fallback;
    }

    public static int pill(Context context) {
        return themeColor(context, R.attr.hunysPill, 0xFFCCD9F3);
    }

    public static int ink(Context context) {
        return themeColor(context, R.attr.hunysInk, 0xFF183D7A);
    }

    public static int accent(Context context) {
        return themeColor(context, R.attr.hunysAccent, 0xFF8FB4F5);
    }

    public static int panel(Context context) {
        return themeColor(context, R.attr.hunysPanel, 0xFF0E203F);
    }

    /**
     * Pastilă colorată când elementul are focus / colored pill when the item is focused.
     */
    public static Drawable focusPill(Context context, float radiusDp) {
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(pill(context));
        focused.setCornerRadius(dp(context, radiusDp));

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, focused);
        states.addState(new int[] {android.R.attr.state_selected, android.R.attr.state_pressed}, focused);
        states.addState(new int[] {}, new ColorDrawable(Color.TRANSPARENT));
        return states;
    }

    /**
     * Pastilă de sticlă (fundal semi-transparent) care devine colorată la focus.<br/>
     * Glass pill (semi-transparent) that turns colored on focus.
     */
    public static Drawable glassPill(Context context, float radiusDp, int normalColor) {
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(pill(context));
        focused.setCornerRadius(dp(context, radiusDp));

        GradientDrawable normal = new GradientDrawable();
        normal.setColor(normalColor);
        normal.setCornerRadius(dp(context, radiusDp));

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, focused);
        states.addState(new int[] {}, normal);
        return states;
    }

    /**
     * Text/iconiță: închis pe pastilă, alb altfel / text/icon: dark on the pill, white otherwise.
     */
    public static ColorStateList focusInk(Context context) {
        return new ColorStateList(
                new int[][] {{android.R.attr.state_focused}, {}},
                new int[] {ink(context), TEXT_NORMAL});
    }

    /**
     * Bara laterală plutitoare cu colțuri rotunjite / floating sidebar with rounded corners.
     */
    public static void styleSidebar(View root) {
        if (root == null || !isHunys(root.getContext())) {
            return;
        }

        Context context = root.getContext();

        View fade = root.findViewById(R.id.fade_out_edge);
        if (fade != null) {
            fade.setVisibility(View.GONE);
        }

        View list = root.findViewById(R.id.browse_headers);
        if (!(list instanceof VerticalGridView)) {
            return;
        }

        VerticalGridView grid = (VerticalGridView) list;

        int top = themeColor(context, R.attr.hunysSidebarTop, 0xFF183A70);
        int bottom = themeColor(context, R.attr.brandColor, 0xFF0F2447);
        GradientDrawable panel = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {top, bottom});
        panel.setCornerRadius(dp(context, 20));
        panel.setStroke(dp(context, 1), 0x14FFFFFF);
        grid.setBackground(panel);

        ViewGroup.LayoutParams params = grid.getLayoutParams();
        if (params instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) params;
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.leftMargin = dp(context, 10);
            lp.topMargin = dp(context, 10);
            lp.bottomMargin = dp(context, 10);
            grid.setLayoutParams(lp);
        }

        grid.setPadding(dp(context, 8), dp(context, 14), dp(context, 8), dp(context, 14));
        grid.setVerticalSpacing(dp(context, 3));

        if (Build.VERSION.SDK_INT >= 21) {
            grid.setClipToOutline(true);
        }
    }

    /**
     * Pornește bara strânsă cu iconițe. Se apelează înainte de crearea ecranului principal.<br/>
     * Enables the collapsed icon rail. Call before the browse screen is created.
     */
    public static void installRail(Context context) {
        if (!isHunys(context)) {
            return;
        }

        int headersWidth = context.getResources().getDimensionPixelSize(R.dimen.lb_browse_headers_width);
        int paddingEnd = context.getResources().getDimensionPixelSize(R.dimen.lb_browse_header_padding_end);
        int panelLeft = headersWidth - paddingEnd - dp(context, RAIL_PANEL_DP);
        // Partea vizibilă începe cu RAIL_GAP_DP înainte de panou / visible part starts RAIL_GAP_DP before the panel
        BrowseSupportFragment.sHunysRailWidth = headersWidth - panelLeft + dp(context, RAIL_GAP_DP);
        BrowseSupportFragment.sHunysRailContentMargin = dp(context, 30);
        HeadersSupportFragment.sHunysKeepChildrenVisible = true;
        BrowseSupportFragment.sHunysHeadersListener = RAIL_LISTENER;
    }

    public static boolean isRail() {
        return sRail;
    }

    /**
     * Comută între bara lată (cu texte) și bara strânsă (doar iconițe).<br/>
     * Switches between the wide sidebar (with labels) and the collapsed rail (icons only).
     */
    public static void setSidebarRail(View root, boolean rail) {
        if (root == null || !isHunys(root.getContext())) {
            return;
        }

        sRail = rail;
        Context context = root.getContext();
        View list = root.findViewById(R.id.browse_headers);

        if (!(list instanceof VerticalGridView)) {
            return;
        }

        VerticalGridView grid = (VerticalGridView) list;
        ViewGroup.LayoutParams params = grid.getLayoutParams();

        if (params instanceof RelativeLayout.LayoutParams) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) params;
            int headersWidth = context.getResources().getDimensionPixelSize(R.dimen.lb_browse_headers_width);
            int paddingEnd = context.getResources().getDimensionPixelSize(R.dimen.lb_browse_header_padding_end);
            lp.leftMargin = rail ? headersWidth - paddingEnd - dp(context, RAIL_PANEL_DP) : dp(context, PANEL_MARGIN_DP);
            grid.setLayoutParams(lp);
        }

        int side = dp(context, rail ? 7 : 8);
        grid.setPadding(side, dp(context, 14), side, dp(context, 14));

        if (rail) {
            // Iconițele din bara strânsă nu primesc focus; ← deschide bara / rail icons aren't focusable; ← opens the bar
            grid.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        }

        for (int i = 0; i < grid.getChildCount(); i++) {
            View wrapper = grid.getChildAt(i);
            View item = wrapper instanceof ViewGroup && ((ViewGroup) wrapper).getChildCount() > 0
                    ? ((ViewGroup) wrapper).getChildAt(0) : wrapper;
            applyRailToItem(item, rail);
        }

        grid.requestLayout();
    }

    /**
     * Element din bară: doar iconița, centrată, sau iconiță + text.<br/>
     * Sidebar item: centered icon only, or icon + label.
     */
    public static void applyRailToItem(View item, boolean rail) {
        if (item == null) {
            return;
        }

        Context context = item.getContext();
        TextView label = item.findViewById(R.id.header_label);

        if (label == null) {
            return;
        }

        label.setVisibility(rail ? View.GONE : View.VISIBLE);
        item.setMinimumWidth(dp(context, rail ? RAIL_PANEL_DP - 14 : 156));
        item.setPadding(dp(context, rail ? 8 : 11), item.getPaddingTop(), dp(context, rail ? 8 : 10), item.getPaddingBottom());
    }

    /**
     * Meniuri: elementul cu focus devine o pastilă albă cu text închis (ca în machetă).<br/>
     * Menus: the focused item becomes a white pill with dark text (like the mockup).
     */
    public static void stylePreferenceList(RecyclerView list) {
        if (list == null || !isHunys(list.getContext())) {
            return;
        }

        list.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@androidx.annotation.NonNull View view) {
                stylePreferenceItem(view);
            }

            @Override
            public void onChildViewDetachedFromWindow(@androidx.annotation.NonNull View view) {
                // NOP
            }
        });

        for (int i = 0; i < list.getChildCount(); i++) {
            stylePreferenceItem(list.getChildAt(i));
        }
    }

    private static void stylePreferenceItem(View item) {
        View target = item.isFocusable() ? item : findFocusable(item);

        if (target == null || target.getTag(R.id.action_hunys_spacer) != null) {
            return;
        }

        target.setTag(R.id.action_hunys_spacer, Boolean.TRUE); // deja stilizat / already styled
        Context context = target.getContext();

        GradientDrawable pill = new GradientDrawable();
        pill.setColor(Color.WHITE);
        pill.setCornerRadius(dp(context, 14));
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, new InsetDrawable(pill, dp(context, 6), dp(context, 2), dp(context, 6), dp(context, 2)));
        states.addState(new int[] {}, new ColorDrawable(Color.TRANSPARENT));
        // InsetDrawable are padding propriu; păstrăm padding-ul original / InsetDrawable has its own padding; keep the original
        int left = target.getPaddingLeft();
        int top = target.getPaddingTop();
        int right = target.getPaddingRight();
        int bottom = target.getPaddingBottom();
        target.setBackground(states);
        target.setPadding(left, top, right, bottom);

        styleTexts(target);
    }

    private static void styleTexts(View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            int normal = text.getCurrentTextColor();
            // Titlul închis, descrierea gri / dark title, grey description
            int focused = text.getId() == android.R.id.summary ? 0xFF555555 : 0xFF111111;
            text.setDuplicateParentStateEnabled(true);
            text.setTextColor(new ColorStateList(new int[][] {{android.R.attr.state_focused}, {}}, new int[] {focused, normal}));
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof ViewGroup) {
                    // copiii primesc starea de focus de la părinte / children get the focus state from the parent
                    ((ViewGroup) child).setDuplicateParentStateEnabled(true);
                }
                styleTexts(child);
            }
        }
    }

    private static View findFocusable(View view) {
        if (!(view instanceof ViewGroup)) {
            return null;
        }

        ViewGroup group = (ViewGroup) view;

        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child.isFocusable()) {
                return child;
            }
            View found = findFocusable(child);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    /**
     * Iconițe rotunjite (Lucide) în locul celor originale / rounded line icons (Lucide) instead of the originals.
     */
    public static int mapSectionIcon(Context context, int resId) {
        if (resId <= 0 || !isHunys(context)) {
            return resId;
        }

        try {
            String name = context.getResources().getResourceEntryName(resId);
            String mapped = null;

            switch (name) {
                case "icon_home": mapped = "hunys_ic_home"; break;
                case "icon_shorts": mapped = "hunys_ic_shorts"; break;
                case "icon_subscriptions": mapped = "hunys_ic_subscriptions"; break;
                case "icon_history": mapped = "hunys_ic_history"; break;
                case "icon_playlist": mapped = "hunys_ic_playlist"; break;
                case "icon_channels": mapped = "hunys_ic_channels"; break;
                case "icon_trending": mapped = "hunys_ic_trending"; break;
                case "icon_sports": mapped = "hunys_ic_sports"; break;
                case "icon_news": mapped = "hunys_ic_news"; break;
                case "icon_music": mapped = "hunys_ic_music"; break;
                case "icon_live": mapped = "hunys_ic_live"; break;
                case "icon_gaming": mapped = "hunys_ic_gaming"; break;
                case "icon_kids_home": mapped = "hunys_ic_kids"; break;
                case "icon_settings": mapped = "hunys_ic_settings"; break;
                case "icon_notification": mapped = "hunys_ic_notification"; break;
                case "icon_queue": mapped = "hunys_ic_queue"; break;
                case "icon_pin": mapped = "hunys_ic_pin"; break;
                case "icon_blocked_channels": mapped = "hunys_ic_blocked"; break;
            }

            if (mapped != null) {
                int id = context.getResources().getIdentifier(mapped, "drawable", context.getPackageName());
                return id != 0 ? id : resId;
            }
        } catch (Resources.NotFoundException e) {
            // keep original icon
        }

        return resId;
    }
}
