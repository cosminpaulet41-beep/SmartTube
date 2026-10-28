package com.liskovsoft.smartyoutubetv2.common.app.presenters.settings;

import android.content.Context;

import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.base.BasePresenter;
import com.liskovsoft.smartyoutubetv2.common.hunys.HunysData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;
import com.liskovsoft.smartyoutubetv2.common.hunys.HunysDimmer;
import com.liskovsoft.smartyoutubetv2.common.hunys.HunysSubtitles;
import com.liskovsoft.smartyoutubetv2.common.hunys.HunysTraffic;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Setările modului Hunys / Hunys mod settings.<br/>
 * SmartTube mod de/by Hunys with Claude AI (MIT).
 */
public class HunysSettingsPresenter extends BasePresenter<Void> {
    private static final int[] DATA_LIMITS_GB = {0, 5, 10, 15, 20, 30, 50, 100};
    private final HunysData mData;

    private HunysSettingsPresenter(Context context) {
        super(context);
        mData = HunysData.instance(context);
    }

    public static HunysSettingsPresenter instance(Context context) {
        return new HunysSettingsPresenter(context);
    }

    public void show() {
        AppDialogPresenter settingsPresenter = AppDialogPresenter.instance(getContext());

        appendDimmingButton(settingsPresenter);
        appendLoudnessSwitch(settingsPresenter);
        appendDataCounterButton(settingsPresenter);
        appendStartQualityCategory(settingsPresenter);
        appendPreviewCategory(settingsPresenter);
        appendTextSizeCategory(settingsPresenter);
        appendSubtitlesButton(settingsPresenter);
        appendLightModeSwitch(settingsPresenter);
        appendAbout(settingsPresenter);

        settingsPresenter.showDialog(getContext().getString(R.string.hunys_settings_title));
    }

    /**
     * Dialogul de întunecare, deschis și din butonul cu lună.<br/>
     * The dimming dialog, also opened from the moon button.
     */
    public void showDimming() {
        AppDialogPresenter presenter = AppDialogPresenter.instance(getContext());
        appendDimmingCategories(presenter);
        presenter.showDialog(getContext().getString(R.string.hunys_dimming));
    }

    private void appendDimmingButton(AppDialogPresenter settingsPresenter) {
        settingsPresenter.appendSingleButton(UiOptionItem.from(getContext().getString(R.string.hunys_dimming),
                option -> showDimming()));
    }

    private void appendDimmingCategories(AppDialogPresenter presenter) {
        HunysDimmer dimmer = HunysDimmer.instance(getContext());

        // Nivel manual / manual level
        List<OptionItem> levels = new ArrayList<>();
        int current = mData.getDimLevel();
        for (int level = 0; level <= HunysData.DIM_LEVEL_MAX; level += HunysData.DIM_LEVEL_STEP) {
            final int value = level;
            levels.add(UiOptionItem.from(levelTitle(value), option -> dimmer.setLevelByUser(value), current == value));
        }
        presenter.appendRadioCategory(getContext().getString(R.string.hunys_dim_level), levels);

        // Automat / automatic
        presenter.appendSingleSwitch(UiOptionItem.from(getContext().getString(R.string.hunys_auto_dim),
                option -> mData.setAutoDimEnabled(option.isSelected()), mData.isAutoDimEnabled()));

        presenter.appendRadioCategory(getContext().getString(R.string.hunys_auto_dim_start),
                timeOptions(mData.getAutoDimStartMin(), true));
        presenter.appendRadioCategory(getContext().getString(R.string.hunys_auto_dim_end),
                timeOptions(mData.getAutoDimEndMin(), false));

        List<OptionItem> nightLevels = new ArrayList<>();
        int night = mData.getAutoDimLevel();
        for (int level = HunysData.DIM_LEVEL_STEP; level <= HunysData.DIM_LEVEL_MAX; level += HunysData.DIM_LEVEL_STEP) {
            final int value = level;
            nightLevels.add(UiOptionItem.from(levelTitle(value), option -> mData.setAutoDimLevel(value), night == value));
        }
        presenter.appendRadioCategory(getContext().getString(R.string.hunys_auto_dim_level), nightLevels);
    }

    private List<OptionItem> timeOptions(int selectedMin, boolean isStart) {
        List<OptionItem> options = new ArrayList<>();

        for (int minutes = 0; minutes < 24 * 60; minutes += 30) {
            final int value = minutes;
            String title = String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60);
            options.add(UiOptionItem.from(title, option -> {
                if (isStart) {
                    mData.setAutoDimStartMin(value);
                } else {
                    mData.setAutoDimEndMin(value);
                }
            }, selectedMin == value));
        }

        return options;
    }

    private String levelTitle(int level) {
        return level == 0 ? getContext().getString(R.string.hunys_off) : level + "%";
    }

    private void appendLoudnessSwitch(AppDialogPresenter settingsPresenter) {
        settingsPresenter.appendSingleSwitch(UiOptionItem.from(getContext().getString(R.string.hunys_loudness),
                getContext().getString(R.string.hunys_loudness_desc),
                option -> mData.setLoudnessEnabled(option.isSelected()), mData.isLoudnessEnabled()));
    }

    private void appendDataCounterButton(AppDialogPresenter settingsPresenter) {
        HunysTraffic.sample(getContext());
        String title = getContext().getString(R.string.hunys_data_counter) + " · "
                + getContext().getString(R.string.hunys_data_month, HunysTraffic.format(HunysTraffic.getMonthBytes(getContext())));
        settingsPresenter.appendSingleButton(UiOptionItem.from(title, option -> showDataCounter()));
    }

    private void showDataCounter() {
        HunysTraffic.sample(getContext());
        AppDialogPresenter presenter = AppDialogPresenter.instance(getContext());

        presenter.appendSingleButton(UiOptionItem.from(
                getContext().getString(R.string.hunys_data_today, HunysTraffic.format(HunysTraffic.getTodayBytes(getContext()))), option -> {}));
        presenter.appendSingleButton(UiOptionItem.from(
                getContext().getString(R.string.hunys_data_month, HunysTraffic.format(HunysTraffic.getMonthBytes(getContext()))), option -> {}));

        List<OptionItem> limits = new ArrayList<>();
        int currentMb = mData.getDataLimitMb();
        for (int gb : DATA_LIMITS_GB) {
            final int mb = gb * 1024;
            String title = gb == 0 ? getContext().getString(R.string.hunys_data_no_limit) : gb + " GB";
            limits.add(UiOptionItem.from(title, option -> mData.setDataLimitMb(mb), currentMb == mb));
        }
        presenter.appendRadioCategory(getContext().getString(R.string.hunys_data_limit), limits);

        presenter.appendSingleSwitch(UiOptionItem.from(getContext().getString(R.string.hunys_data_warn),
                option -> mData.setDataWarnEnabled(option.isSelected()), mData.isDataWarnEnabled()));

        presenter.appendSingleButton(UiOptionItem.from(getContext().getString(R.string.hunys_data_reset), option -> {
            HunysTraffic.reset(getContext());
            presenter.closeDialog();
        }));

        presenter.showDialog(getContext().getString(R.string.hunys_data_counter));
    }

    /**
     * Calitatea cu care pornește fiecare clip după deschiderea aplicației (implicit 720p).<br/>
     * The quality every video starts with after opening the app (default 720p).
     */
    private void appendStartQualityCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();
        int current = mData.getStartQuality();

        for (int height : new int[] {480, 720, 1080}) {
            options.add(UiOptionItem.from(height + "p", option -> {
                mData.setStartQuality(height);
                PlayerData playerData = PlayerData.instance(getContext());
                playerData.setFormat(playerData.getDefaultVideoFormat());
            }, current == height));
        }

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.hunys_start_quality), options);
    }

    private void appendPreviewCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();
        int current = mData.getPreviewSeconds();

        for (int seconds : new int[] {0, 30, 60, 120}) {
            String title = seconds == 0 ? getContext().getString(R.string.hunys_off)
                    : seconds < 60 ? getContext().getString(R.string.hunys_seconds, seconds)
                    : getContext().getString(R.string.hunys_minutes, seconds / 60);
            options.add(UiOptionItem.from(title, option -> mData.setPreviewSeconds(seconds), current == seconds));
        }

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.hunys_preview), options);

        settingsPresenter.appendSingleSwitch(UiOptionItem.from(getContext().getString(R.string.hunys_preview_sound),
                option -> mData.setPreviewMuted(!option.isSelected()), !mData.isPreviewMuted()));
    }

    private void appendTextSizeCategory(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();
        float current = mData.getFontScale();

        float[] scales = {1.0f, HunysData.FONT_SCALE_DEFAULT, 1.25f};
        int[] titles = {R.string.hunys_text_size_normal, R.string.hunys_text_size_large, R.string.hunys_text_size_xlarge};

        for (int i = 0; i < scales.length; i++) {
            final float scale = scales[i];
            options.add(UiOptionItem.from(getContext().getString(titles[i]), option -> {
                mData.setFontScale(scale);
                MessageHelpers.showLongMessage(getContext(), R.string.hunys_restart_needed);
            }, Math.abs(current - scale) < 0.01f));
        }

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.hunys_text_size), options);
    }

    private void appendSubtitlesButton(AppDialogPresenter settingsPresenter) {
        settingsPresenter.appendSingleButton(UiOptionItem.from(getContext().getString(R.string.hunys_subtitles), option -> {
            HunysSubtitles.applyVisibleStyle(getContext());
            MessageHelpers.showMessage(getContext(), R.string.hunys_subtitles);
        }));
    }

    private void appendLightModeSwitch(AppDialogPresenter settingsPresenter) {
        settingsPresenter.appendSingleSwitch(UiOptionItem.from(getContext().getString(R.string.hunys_light_mode),
                getContext().getString(R.string.hunys_light_mode_desc),
                option -> {
                    mData.setLightModeEnabled(option.isSelected());
                    MessageHelpers.showLongMessage(getContext(), R.string.hunys_restart_needed);
                }, mData.isLightModeEnabled()));
    }

    private void appendAbout(AppDialogPresenter settingsPresenter) {
        String text = getContext().getString(R.string.hunys_about_text) + "\n\n" + getContext().getString(R.string.hunys_source_code);
        settingsPresenter.appendLongTextCategory(getContext().getString(R.string.hunys_about), UiOptionItem.from(text));
    }
}
