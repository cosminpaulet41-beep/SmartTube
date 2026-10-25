package com.liskovsoft.smartyoutubetv2.common.hunys;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.liskovsoft.smartyoutubetv2.common.R;

/**
 * Pune fontul Inter pe toate textele din aplicație.<br/>
 * Applies the Inter font to every text in the app.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT). Inter: SIL Open Font License.
 */
public final class HunysFonts {
    private static final String[] PREFIXES = {"android.widget.", "android.view.", "android.webkit.", "android.app."};
    private static Typeface sRegular;
    private static Typeface sMedium;
    private static Typeface sSemiBold;
    private static Typeface sBold;
    private static boolean sLoadFailed;

    private HunysFonts() {
    }

    /**
     * Trebuie apelat înainte de super.onCreate().<br/>
     * Must be called before super.onCreate().
     */
    public static void install(Activity activity) {
        if (Build.VERSION.SDK_INT < 29 || !HunysData.isHunysBuild(activity)) {
            return;
        }

        try {
            LayoutInflater inflater = activity.getLayoutInflater();
            if (inflater.getFactory2() == null) {
                inflater.setFactory2(new Factory(inflater));
            }
        } catch (Exception e) {
            // Factory already set or not supported: keep system font
        }
    }

    private static void load(Context context) {
        if (sRegular != null || sLoadFailed) {
            return;
        }

        try {
            sRegular = ResourcesCompat.getFont(context, R.font.inter_regular);
            sMedium = ResourcesCompat.getFont(context, R.font.inter_medium);
            sSemiBold = ResourcesCompat.getFont(context, R.font.inter_semibold);
            sBold = ResourcesCompat.getFont(context, R.font.inter_bold);
        } catch (Exception e) {
            sLoadFailed = true;
        }

        if (sRegular == null) {
            sLoadFailed = true;
        }
    }

    public static void apply(TextView view) {
        if (view == null) {
            return;
        }

        load(view.getContext());

        if (sLoadFailed) {
            return;
        }

        Typeface current = view.getTypeface();
        Typeface target = sRegular;

        if (current != null) {
            int weight = 400;
            if (Build.VERSION.SDK_INT >= 28) {
                weight = current.getWeight();
            }
            if (current.isBold() || weight >= 700) {
                target = sBold;
            } else if (weight >= 600) {
                target = sSemiBold;
            } else if (weight >= 500) {
                target = sMedium;
            }
        }

        if (target != null) {
            view.setTypeface(target);
        }
    }

    private static class Factory implements LayoutInflater.Factory2 {
        private final LayoutInflater mInflater;

        Factory(LayoutInflater inflater) {
            mInflater = inflater;
        }

        @Nullable
        @Override
        public View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
            if (Build.VERSION.SDK_INT < 29 || isSpecialTag(name)) {
                return null;
            }

            View view = null;

            try {
                if (name.indexOf('.') == -1) {
                    for (String prefix : PREFIXES) {
                        try {
                            view = mInflater.createView(context, name, prefix, attrs);
                        } catch (ClassNotFoundException e) {
                            // try next prefix
                        }
                        if (view != null) {
                            break;
                        }
                    }
                } else {
                    view = mInflater.createView(context, name, null, attrs);
                }
            } catch (Throwable e) {
                return null; // let the system inflate it
            }

            if (view instanceof TextView) {
                apply((TextView) view);
            }

            return view;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
            return onCreateView(null, name, context, attrs);
        }

        private static boolean isSpecialTag(String name) {
            return "fragment".equals(name) || "include".equals(name) || "merge".equals(name) || "blink".equals(name)
                    || "requestFocus".equals(name) || "tag".equals(name) || "view".equals(name)
                    || "androidx.fragment.app.FragmentContainerView".equals(name);
        }
    }
}
