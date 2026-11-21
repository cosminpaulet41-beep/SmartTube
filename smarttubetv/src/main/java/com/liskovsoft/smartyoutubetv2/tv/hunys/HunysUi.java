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
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.app.HeadersSupportFragment;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.leanback.widget.SearchOrbView;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

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
    // Lista începe sub butoanele de sus, la fel în ambele stări (iconițele nu sar)
    // The list starts below the top buttons, the same in both states (icons don't jump)
    private static final int LIST_TOP_DP = 82;
    // Bara deschisă: cât e acum (172dp) sau mai îngustă, după cel mai lung cuvânt
    // Open sidebar: as now (172dp) or narrower, after the longest word
    private static final int PANEL_MAX_DP = 172;
    private static final int PANEL_MIN_DP = 152;
    private static final int CONTENT_GAP_DP = 23;
    // Butoanele de sus (căutare, cont, lună): cercuri de ~35dp / top buttons (search, account, moon): ~35dp circles
    private static final float ORB_SCALE = 0.68f;
    private static final int ORBS_LEFT_OPEN_DP = 23;
    private static final int ORBS_LEFT_RAIL_DP = 60;
    private static final int ORB_GAP_DP = 8;
    private static int sPanelWidthPx;
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
            lp.width = sRail ? ViewGroup.LayoutParams.MATCH_PARENT : openPanelWidth();
            lp.leftMargin = dp(context, 10);
            lp.topMargin = dp(context, 10);
            lp.bottomMargin = dp(context, 10);
            grid.setLayoutParams(lp);
        }

        grid.setPadding(dp(context, 8), dp(context, LIST_TOP_DP), dp(context, 8), dp(context, 14));
        grid.setVerticalSpacing(dp(context, 3));

        if (Build.VERSION.SDK_INT >= 21) {
            grid.setClipToOutline(true);
        }

        // Cercul albastru transparent pe secțiunea curentă, în bara strânsă
        // Translucent blue circle on the current section, in the collapsed rail
        grid.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(RecyclerView parent, RecyclerView.ViewHolder child, int position, int subposition) {
                updateRailSelection(grid);
            }
        });
        grid.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@androidx.annotation.NonNull View view) {
                updateRailSelection(grid);
            }

            @Override
            public void onChildViewDetachedFromWindow(@androidx.annotation.NonNull View view) {
                // NOP
            }
        });
    }

    private static int openPanelWidth() {
        return sPanelWidthPx > 0 ? sPanelWidthPx : ViewGroup.LayoutParams.MATCH_PARENT;
    }

    /**
     * Lățimea barei deschise după cel mai lung nume de secțiune (în limba aplicației).<br/>
     * The open sidebar width after the longest section name (in the app language).
     * @return true dacă s-a schimbat / true if it changed
     */
    public static boolean updatePanelWidth(Context context, List<CharSequence> labels) {
        if (context == null || !isHunys(context) || labels == null || labels.isEmpty()) {
            return false;
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13, context.getResources().getDisplayMetrics()));
        Typeface inter = com.liskovsoft.smartyoutubetv2.common.hunys.HunysFonts.typeface(context, 500);
        paint.setTypeface(inter != null ? inter : Typeface.create("sans-serif-medium", Typeface.NORMAL));

        float maxText = 0;
        for (CharSequence label : labels) {
            if (label != null) {
                maxText = Math.max(maxText, paint.measureText(label.toString()));
            }
        }
        maxText = Math.min(maxText, dp(context, 120)); // ca maxWidth din layout / like maxWidth in the layout

        // spațiu bară 8 + element 11 + iconiță 18 + 11 + text + 10 + 8 + puțin aer
        // panel 8 + item 11 + icon 18 + 11 + text + 10 + 8 + a bit of air
        int width = Math.round(maxText) + dp(context, 72);
        width = Math.max(dp(context, PANEL_MIN_DP), Math.min(dp(context, PANEL_MAX_DP), width));

        if (width == sPanelWidthPx) {
            return false;
        }

        sPanelWidthPx = width;
        BrowseSupportFragment.sHunysExpandedContentMargin = dp(context, PANEL_MARGIN_DP) + width + dp(context, CONTENT_GAP_DP);
        return true;
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
        HeadersSupportFragment.sHunysFixedList = true;
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
            lp.width = rail ? ViewGroup.LayoutParams.MATCH_PARENT : openPanelWidth();
            grid.setLayoutParams(lp);
        }

        int side = dp(context, rail ? 7 : 8);
        grid.setPadding(side, dp(context, LIST_TOP_DP), side, dp(context, 14));

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

        updateRailSelection(grid);
        grid.requestLayout();

        // Butoanele de sus: în bară când e deschisă, lângă bară când e strânsă
        // Top buttons: inside the sidebar when open, next to it when collapsed
        View title = root.getRootView() != null ? root.getRootView().findViewById(R.id.browse_title_group) : null;
        layoutTitleOrbs(title, rail, true);
    }

    /**
     * Bara strânsă: secțiunea curentă are un cerc albastru transparent; bara deschisă: pastila la focus.<br/>
     * Collapsed rail: the current section gets a translucent blue circle; open sidebar: the pill on focus.
     */
    public static void updateRailSelection(VerticalGridView grid) {
        if (grid == null) {
            return;
        }

        Context context = grid.getContext();
        int selected = grid.getSelectedPosition();

        for (int i = 0; i < grid.getChildCount(); i++) {
            View wrapper = grid.getChildAt(i);
            View item = wrapper instanceof ViewGroup && ((ViewGroup) wrapper).getChildCount() > 0
                    ? ((ViewGroup) wrapper).getChildAt(0) : wrapper;

            if (item == null || item.findViewById(R.id.header_label) == null) {
                continue;
            }

            int position = grid.getChildAdapterPosition(wrapper);
            String state = sRail ? (position == selected ? "rail_on" : "rail_off") : "open";

            if (state.equals(item.getTag(R.id.header_icon))) {
                continue;
            }

            item.setTag(R.id.header_icon, state);
            int left = item.getPaddingLeft();
            int top = item.getPaddingTop();
            int right = item.getPaddingRight();
            int bottom = item.getPaddingBottom();

            if ("open".equals(state)) {
                item.setBackground(focusPill(context, 11));
            } else if ("rail_on".equals(state)) {
                GradientDrawable circle = new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);
                circle.setColor((accent(context) & 0x00FFFFFF) | 0x61000000); // ~38%
                item.setBackground(circle);
            } else {
                item.setBackground(null);
            }

            item.setPadding(left, top, right, bottom);
        }
    }

    /**
     * Culorile butoanelor rotunde de sus: sticlă, mai deschis la focus, iconiță albă.<br/>
     * Colors of the round top buttons: glass, lighter on focus, white icon.
     */
    public static SearchOrbView.Colors orbColors(Context context) {
        int base = themeColor(context, R.attr.hunysSidebarTop, 0xFF183A70);
        int glass = blend(base, Color.WHITE, 0.17f);
        int bright = blend(glass, accent(context), 0.55f);
        return new SearchOrbView.Colors(glass, bright, Color.WHITE);
    }

    private static int blend(int from, int to, float amount) {
        int r = Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * amount);
        int g = Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * amount);
        int b = Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * amount);
        return Color.rgb(r, g, b);
    }

    /**
     * Butoanele de sus (căutare, cont, lună) în același stil și la aceeași mărime.<br/>
     * The top buttons (search, account, moon) in the same style and size.
     */
    public static void styleTitleOrbs(View title) {
        if (title == null || !isHunys(title.getContext())) {
            return;
        }

        Context context = title.getContext();
        SearchOrbView.Colors colors = orbColors(context);

        for (int id : new int[] {R.id.title_orb, R.id.account_orb, R.id.hunys_dim_orb}) {
            View view = title.findViewById(id);
            if (view instanceof SearchOrbView) {
                ((SearchOrbView) view).setOrbColors(colors);
            }
        }

        for (int id : new int[] {R.id.account_orb, R.id.hunys_dim_orb, R.id.language_orb, R.id.exit_pip, R.id.pip_title}) {
            View view = title.findViewById(id);
            if (view != null && view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                lp.setMarginStart(dp(context, ORB_GAP_DP));
                view.setLayoutParams(lp);
            }
        }

        layoutTitleOrbs(title, sRail, false);
    }

    private static void layoutTitleOrbs(View title, boolean rail, boolean animate) {
        if (title == null || !isHunys(title.getContext())) {
            return;
        }

        View account = title.findViewById(R.id.account_orb);
        View container = account != null ? (View) account.getParent() : null;

        if (container == null) {
            return;
        }

        if (container.getWidth() == 0) {
            // încă nu e așezat / not laid out yet
            container.post(() -> layoutTitleOrbs(title, sRail, false));
            return;
        }

        // Ecranele fără bară laterală: butoanele stau ca lângă bara strânsă / screens without a sidebar: buttons sit as next to the rail
        if (!rail && title.getRootView().findViewById(R.id.browse_headers) == null) {
            rail = true;
        }

        Context context = title.getContext();
        container.setPivotX(0);
        container.setPivotY(container.getHeight() / 2f);
        container.setScaleX(ORB_SCALE);
        container.setScaleY(ORB_SCALE);

        float target = dp(context, rail ? ORBS_LEFT_RAIL_DP : ORBS_LEFT_OPEN_DP) - container.getLeft();

        if (animate) {
            container.animate().translationX(target).setDuration(180).start();
        } else {
            container.setTranslationX(target);
        }
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
        // Deschisă: pastila pe toată lățimea barei / open: the pill spans the sidebar width
        item.setMinimumWidth(rail ? dp(context, RAIL_PANEL_DP - 14)
                : (sPanelWidthPx > 0 ? sPanelWidthPx - dp(context, 16) : dp(context, PANEL_MAX_DP - 16)));
        item.setPadding(dp(context, rail ? 8 : 11), item.getPaddingTop(), dp(context, rail ? 8 : 10), item.getPaddingBottom());
    }

    /**
     * Meniuri: elementul cu focus devine o pastilă în culoarea temei, scrisul rămâne alb (varianta C aleasă).<br/>
     * Menus: the focused item becomes a theme-colored pill, text stays white (chosen variant C).
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
        // Culoarea temei la ~35%, peste panoul închis / theme color at ~35%, over the dark panel
        pill.setColor((accent(context) & 0x00FFFFFF) | 0x5A000000);
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
     * Contur în jurul cardului selectat (varianta C aleasă), restul cardului rămâne la fel.<br/>
     * Outline around the selected card (chosen variant C), the rest of the card stays the same.
     */
    public static Drawable cardOutline(Context context) {
        GradientDrawable outline = new GradientDrawable();
        outline.setColor(Color.TRANSPARENT);
        outline.setStroke(dp(context, 3), accent(context));
        outline.setCornerRadius(dp(context, 6));
        return outline;
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
