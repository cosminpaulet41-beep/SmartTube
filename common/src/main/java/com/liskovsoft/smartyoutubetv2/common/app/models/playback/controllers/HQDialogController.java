package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.BasePlayerController;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.utils.AppDialogUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HQDialogController extends BasePlayerController {
    private static final String TAG = HQDialogController.class.getSimpleName();
    private static final int VIDEO_FORMATS_ID = 132;
    private static final int AUDIO_FORMATS_ID = 133;
    // NOTE: using map, because same item could be changed time to time
    private final Map<Integer, OptionCategory> mCategories = new LinkedHashMap<>();
    private final Map<Integer, OptionCategory> mCategoriesInt = new LinkedHashMap<>();
    private final Set<Runnable> mHideListeners = new HashSet<>();
    private AppDialogPresenter mAppDialogPresenter;

    @Override
    public void onInit() {
        mAppDialogPresenter = AppDialogPresenter.instance(getContext());
    }

    @Override
    public void onViewResumed() {
        //updateBackgroundPlayback();
    }

    @Override
    public void onButtonClicked(int buttonId, int buttonState) {
        if (buttonId == R.id.lb_control_high_quality) {
            onHighQualityClicked();
        } else if (buttonId == R.id.action_player_settings) {
            onHunysSettingsClicked();
        } else if (buttonId == R.id.action_hunys_dim) {
            // Hunys mod: modul de noapte din player / night mode from the player
            com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.HunysSettingsPresenter.instance(getContext()).showDimming();
        }
    }

    private void onHighQualityClicked() {
        fitVideoIntoDialog();

        addQualityCategories();
        addAudioLanguage();
        addPresetsCategory();
        addVideoZoomCategory();
        addNetworkEngine();
        addVideoBufferCategory();
        addAudioDelayCategory();
        addPitchEffectCategory();
        addSleepTimerCategory();
        //addBackgroundPlaybackCategory();

        appendOptions(mCategoriesInt);
        appendOptions(mCategories);

        mAppDialogPresenter.showDialog(getContext().getString(R.string.playback_settings), this::onDialogHide);
    }

    // ---- Hunys mod: meniul ⚙ / the ⚙ menu

    /**
     * Ordinea aprobată: Calitate, Limbă audio, Viteză, Repetare, PiP, Doar sunet, Căutare, Statistici.<br/>
     * Approved order: Quality, Audio language, Speed, Repeat, PiP, Audio only, Search, Stats.
     */
    private void onHunysSettingsClicked() {
        if (getPlayer() == null) {
            return;
        }

        AppDialogPresenter dialog = AppDialogPresenter.instance(getContext());

        dialog.appendSingleButton(UiOptionItem.from(
                hunysTitle(R.string.hunys_quality, hunysQualityValue()), option -> showHunysQuality()));
        dialog.appendSingleButton(UiOptionItem.from(
                hunysTitle(R.string.hunys_audio_language, hunysAudioLanguageValue()), option -> showHunysAudioLanguage()));
        dialog.appendSingleButton(UiOptionItem.from(
                hunysTitle(R.string.hunys_speed, formatSpeed(getPlayerData().getSpeed())), option -> {
                    getMainController().onButtonLongClicked(R.id.action_video_speed, getPlayer().getButtonState(R.id.action_video_speed));
                }));
        dialog.appendSingleButton(UiOptionItem.from(
                getContext().getString(R.string.hunys_repeat), option -> {
                    getMainController().onButtonLongClicked(R.id.action_repeat, getPlayer().getButtonState(R.id.action_repeat));
                }));
        dialog.appendSingleButton(UiOptionItem.from(
                getContext().getString(R.string.hunys_pip), option -> {
                    dialog.closeDialog();
                    getMainController().onButtonClicked(R.id.action_pip, getPlayer().getButtonState(R.id.action_pip));
                }));
        dialog.appendSingleButton(UiOptionItem.from(
                hunysTitle(R.string.hunys_audio_only, getContext().getString(isHunysAudioOnly() ? R.string.hunys_on : R.string.hunys_off)), option -> {
                    dialog.closeDialog();
                    toggleHunysAudioOnly();
                }));
        dialog.appendSingleButton(UiOptionItem.from(
                getContext().getString(R.string.hunys_search), option -> {
                    dialog.closeDialog();
                    getMainController().onButtonClicked(R.id.action_search, getPlayer().getButtonState(R.id.action_search));
                }));
        dialog.appendSingleButton(UiOptionItem.from(
                getContext().getString(R.string.hunys_stats), option -> {
                    dialog.closeDialog();
                    getMainController().onButtonClicked(R.id.action_video_stats, getPlayer().getButtonState(R.id.action_video_stats));
                }));

        dialog.showDialog(getContext().getString(R.string.hunys_player_settings));
    }

    private String hunysTitle(int titleRes, String value) {
        String title = getContext().getString(titleRes);
        return value == null || value.isEmpty() ? title : title + "  ·  " + value;
    }

    private String hunysQualityValue() {
        FormatItem current = getPlayer() != null ? getPlayer().getVideoFormat() : null;

        if (current == null || isHunysAudioOnly()) {
            return null;
        }

        int height = Math.min(current.getWidth(), current.getHeight());

        if (height <= 0) {
            return null;
        }

        return height + "p" + (current.getFrameRate() > 30 ? Math.round(current.getFrameRate()) : "");
    }

    private void showHunysQuality() {
        if (getPlayer() == null) {
            return;
        }

        AppDialogPresenter dialog = AppDialogPresenter.instance(getContext());
        List<FormatItem> videoFormats = getPlayer().getVideoFormats();
        dialog.appendCategory(OptionCategory.from(
                VIDEO_FORMATS_ID,
                OptionCategory.TYPE_RADIO_LIST,
                getContext().getString(R.string.hunys_quality),
                UiOptionItem.from(videoFormats, option -> {
                    selectFormatOption(option);
                    dialog.closeDialog();
                }, getContext().getString(R.string.hunys_audio_only))));
        dialog.showDialog(getContext().getString(R.string.hunys_quality));
    }

    private String hunysAudioLanguageValue() {
        String language = getPlayerData().getAudioLanguage();

        if (language == null || language.isEmpty()) {
            return getContext().getString(R.string.hunys_audio_original);
        }

        return capitalize(new Locale(language).getDisplayLanguage());
    }

    /**
     * Arată doar limbile pe care le are clipul; dacă are una singură, arată lista completă SmartTube.<br/>
     * Shows only the languages the video has; with a single one, shows the full SmartTube list.
     */
    private void showHunysAudioLanguage() {
        if (getPlayer() == null) {
            return;
        }

        AppDialogPresenter dialog = AppDialogPresenter.instance(getContext());
        Set<String> codes = new LinkedHashSet<>();
        List<FormatItem> audioFormats = getPlayer().getAudioFormats();

        if (audioFormats != null) {
            for (FormatItem format : audioFormats) {
                String code = normalizeLanguage(format.getLanguage());
                if (code != null) {
                    codes.add(code);
                }
            }
        }

        if (codes.size() < 2) {
            dialog.appendCategory(AppDialogUtil.createAudioLanguageCategory(getContext(), () -> getPlayer().restartEngine()));
            dialog.showDialog(getContext().getString(R.string.hunys_audio_language));
            return;
        }

        String current = getPlayerData().getAudioLanguage();
        List<OptionItem> options = new ArrayList<>();

        options.add(UiOptionItem.from(getContext().getString(R.string.hunys_audio_original), option -> {
            getPlayerData().setAudioLanguage("");
            getPlayer().restartEngine();
            dialog.closeDialog();
        }, current == null || current.isEmpty()));

        for (String code : codes) {
            options.add(UiOptionItem.from(capitalize(new Locale(code).getDisplayLanguage()), option -> {
                getPlayerData().setAudioLanguage(code);
                getPlayer().restartEngine();
                dialog.closeDialog();
            }, code.equals(current)));
        }

        dialog.appendRadioCategory(getContext().getString(R.string.hunys_audio_language), options);
        dialog.showDialog(getContext().getString(R.string.hunys_audio_language));
    }

    private static String normalizeLanguage(String language) {
        if (language == null || language.isEmpty() || "und".equals(language)) {
            return null;
        }

        String code = language.toLowerCase(Locale.US);
        int cut = code.indexOf('-');
        if (cut < 0) {
            cut = code.indexOf('.');
        }
        if (cut < 0) {
            cut = code.indexOf('_');
        }

        return cut > 0 ? code.substring(0, cut) : code;
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private static String formatSpeed(float speed) {
        String text = String.format(Locale.US, "%.2f", speed); // 1.00, 1.25, 1.50
        if (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1); // 1.0, 1.5
        }
        return text + "x";
    }

    private boolean isHunysAudioOnly() {
        return getPlayer() != null && FormatItem.NO_VIDEO.equals(getPlayer().getVideoFormat());
    }

    /**
     * Doar sunet = fără pistă video (economisește date). Se păstrează până îl oprești.<br/>
     * Audio only = no video track (saves data). Kept until you turn it off.
     */
    private void toggleHunysAudioOnly() {
        if (getPlayer() == null) {
            return;
        }

        FormatItem format = isHunysAudioOnly() ? getPlayerData().getDefaultVideoFormat() : FormatItem.NO_VIDEO;
        getPlayer().setFormat(format);
        persistFormat(format);

        if (!getPlayer().containsMedia()) {
            getPlayer().reloadPlayback();
        }
    }

    private void addQualityCategories() {
        if (getPlayer() == null) {
            return;
        }

        List<FormatItem> videoFormats = getPlayer().getVideoFormats();
        String videoFormatsTitle = getContext().getString(R.string.title_video_formats);

        List<FormatItem> audioFormats = getPlayer().getAudioFormats();
        String audioFormatsTitle = getContext().getString(R.string.title_audio_formats);

        addCategoryInt(OptionCategory.from(
                VIDEO_FORMATS_ID,
                OptionCategory.TYPE_RADIO_LIST,
                videoFormatsTitle,
                UiOptionItem.from(videoFormats, this::selectFormatOption, getContext().getString(R.string.option_disabled))));
        addCategoryInt(OptionCategory.from(
                AUDIO_FORMATS_ID,
                OptionCategory.TYPE_RADIO_LIST,
                audioFormatsTitle,
                UiOptionItem.from(audioFormats, this::selectFormatOption, getContext().getString(R.string.option_disabled))));
    }

    private void selectFormatOption(OptionItem option) {
        if (getPlayer() == null) {
            return;
        }

        FormatItem formatItem = UiOptionItem.toFormat(option);
        getPlayer().setFormat(formatItem);
        persistFormat(formatItem);

        if (getPlayerData().getFormat(formatItem.getType()).isPreset()) {
            // Preset currently active. Show warning about format reset.
            MessageHelpers.showMessage(getContext(), R.string.video_preset_enabled);
        }

        if (!getPlayer().containsMedia()) {
            getPlayer().reloadPlayback();
        }

        // Make result easily be spotted by the user
        if (formatItem.getType() == FormatItem.TYPE_VIDEO) {
            getPlayer().showOverlay(false);
        }
    }

    private void persistFormat(FormatItem formatItem) {
        if (formatItem.getType() == FormatItem.TYPE_VIDEO) {
            if (!getPlayerData().getFormat(FormatItem.TYPE_VIDEO).isPreset()) {
                getPlayerData().setFormat(formatItem);
            } else {
                getPlayerData().setTempVideoFormat(formatItem);
            }
        } else {
            getPlayerData().setFormat(formatItem);
        }
    }

    private void addVideoBufferCategory() {
        if (getPlayer() == null) {
            return;
        }
        addCategoryInt(AppDialogUtil.createVideoBufferCategory(getContext(),
                () -> getPlayer().restartEngine()));
    }

    private void addAudioDelayCategory() {
        if (getPlayer() == null) {
            return;
        }
        addCategoryInt(AppDialogUtil.createAudioDelayCategory(getContext(),
                () -> getPlayer().restartEngine()));
    }

    private void addPitchEffectCategory() {
        addCategoryInt(AppDialogUtil.createPitchEffectCategory(getContext()));
    }

    private void addSleepTimerCategory() {
        addCategoryInt(AppDialogUtil.createSleepTimerCategory(getContext()));
    }

    private void addAudioLanguage() {
        if (getPlayer() == null) {
            return;
        }
        addCategoryInt(AppDialogUtil.createAudioLanguageCategory(getContext(),
                () -> getPlayer().restartEngine()));
    }

    private void addNetworkEngine() {
        if (getPlayer() == null) {
            return;
        }
        addCategoryInt(AppDialogUtil.createNetworkEngineCategory(getContext(),
                () -> getPlayer().restartEngine()));
    }

    private void onDialogHide() {
        //updateBackgroundPlayback();

        for (Runnable listener : mHideListeners) {
            listener.run();
        }

        mHideListeners.clear();
        mCategories.clear();
        mCategoriesInt.clear();
    }

    //private void updateBackgroundPlayback() {
    //    ViewManager.instance(getContext()).blockTop(null);
    //
    //    if (getPlayer() != null) {
    //        getPlayer().setBackgroundMode(getPlayerData().getBackgroundMode());
    //    }
    //}

    //private void addBackgroundPlaybackCategory() {
    //    OptionCategory category =
    //            AppDialogUtil.createBackgroundPlaybackCategory(getContext(), getPlayerData(), GeneralData.instance(getContext()), this::updateBackgroundPlayback);
    //
    //    addCategoryInt(category);
    //}

    private void addPresetsCategory() {
        addCategoryInt(AppDialogUtil.createVideoPresetsCategory(
                getContext(), () -> {
                    if (getPlayer() == null) {
                        return;
                    }

                    FormatItem format = getPlayerData().getFormat(FormatItem.TYPE_VIDEO);
                    getPlayer().setFormat(format);

                    if (!getPlayer().containsMedia()) {
                        getPlayer().reloadPlayback();
                    }

                    // Make result easily be spotted by the user
                    getPlayer().showOverlay(false);
                }
        ));
    }

    private void addVideoZoomCategory() {
        if (getPlayer() == null) {
            return;
        }

        addCategoryInt(AppDialogUtil.createVideoZoomCategory(
                getContext(), () -> {
                    getPlayer().setResizeMode(getPlayerData().getResizeMode());
                    getPlayer().setZoomPercents(getPlayerData().getZoomPercents());

                    // Make result easily be spotted by the user
                    getPlayer().showOverlay(false);
                }));
    }

    private void removeCategoryInt(int id) {
        mCategoriesInt.remove(id);
    }

    private void addCategoryInt(OptionCategory category) {
        mCategoriesInt.put(category.id, category);
    }

    public void removeCategory(int id) {
        mCategories.remove(id);
    }

    public void addCategory(OptionCategory category) {
        mCategories.put(category.id, category);
    }

    public void addOnDialogHide(Runnable listener) {
        mHideListeners.add(listener);
    }

    public void removeOnDialogHide(Runnable listener) {
        mHideListeners.remove(listener);
    }

    private void appendOptions(Map<Integer, OptionCategory> categories) {
        for (OptionCategory category : categories.values()) {
            mAppDialogPresenter.appendCategory(category);
        }
    }
}
