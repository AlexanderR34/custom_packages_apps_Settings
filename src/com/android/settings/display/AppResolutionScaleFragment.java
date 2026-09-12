/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.display;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-App Resolution Scaling Fragment.
 * Allows picking presets (1220p, 1080p, 720p, 480p) or a continuous slider (20% - 100%).
 */
public class AppResolutionScaleFragment extends SettingsPreferenceFragment {

    public static final String SETTING_KEY = "game_resolution_scale_map";
    private static final String PREF_KEY_APP_PREFIX = "app_res_scale_";

    private PackageManager mPackageManager;
    private PreferenceCategory mAppCategory;

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPackageManager = requireContext().getPackageManager();
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(requireContext());
        setPreferenceScreen(screen);

        mAppCategory = new PreferenceCategory(requireContext());
        mAppCategory.setTitle(R.string.app_resolution_scale_category_title);
        screen.addPreference(mAppCategory);

        loadAppList();
    }

    private void loadAppList() {
        Context context = requireContext();
        List<ApplicationInfo> apps = mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        List<AppInfo> appList = new ArrayList<>();
        for (ApplicationInfo app : apps) {
            if (mPackageManager.getLaunchIntentForPackage(app.packageName) != null) {
                String label = app.loadLabel(mPackageManager).toString();
                appList.add(new AppInfo(label, app.packageName, app));
            }
        }

        Collections.sort(appList, Comparator.comparing(a -> a.label.toLowerCase()));

        Map<String, Float> scaleMap = loadScaleMap(context);

        for (AppInfo appInfo : appList) {
            Preference preference = new Preference(context);
            preference.setKey(PREF_KEY_APP_PREFIX + appInfo.packageName);
            preference.setTitle(appInfo.label);
            preference.setIcon(appInfo.appInfo.loadIcon(mPackageManager));

            float currentScale = scaleMap.getOrDefault(appInfo.packageName, 1.0f);
            preference.setSummary(formatSummary(context, currentScale));

            preference.setOnPreferenceClickListener(pref -> {
                showScaleDialog(context, appInfo, preference);
                return true;
            });

            mAppCategory.addPreference(preference);
        }
    }

    private String formatSummary(Context context, float scale) {
        int percent = Math.round(scale * 100);
        if (percent >= 98) {
            return context.getString(R.string.app_resolution_preset_native);
        } else if (percent >= 85 && percent <= 90) {
            return context.getString(R.string.app_resolution_preset_1080p);
        } else if (percent >= 57 && percent <= 61) {
            return context.getString(R.string.app_resolution_preset_720p);
        } else if (percent >= 37 && percent <= 41) {
            return context.getString(R.string.app_resolution_preset_480p);
        } else {
            return context.getString(R.string.app_resolution_slider_label, percent);
        }
    }

    private void showScaleDialog(Context context, AppInfo appInfo, Preference preference) {
        Map<String, Float> scaleMap = loadScaleMap(context);
        float currentScale = scaleMap.getOrDefault(appInfo.packageName, 1.0f);
        int currentPercent = Math.max(20, Math.min(100, Math.round(currentScale * 100)));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, context.getResources().getDisplayMetrics());
        container.setPadding(pad, pad / 2, pad, pad / 2);

        // Preset Label
        TextView presetLabel = new TextView(context);
        presetLabel.setText(R.string.app_resolution_scale_title);
        presetLabel.setTypeface(Typeface.DEFAULT_BOLD);
        presetLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        presetLabel.setPadding(0, 0, 0, pad / 4);
        container.addView(presetLabel);

        // Presets Spinner
        Spinner presetSpinner = new Spinner(context);
        String[] presetOptions = new String[]{
                context.getString(R.string.app_resolution_preset_native), // 100%
                context.getString(R.string.app_resolution_preset_1080p),  // 88%
                context.getString(R.string.app_resolution_preset_720p),   // 59%
                context.getString(R.string.app_resolution_preset_480p),   // 39%
                context.getString(R.string.app_resolution_preset_custom)  // Custom
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, presetOptions);
        presetSpinner.setAdapter(adapter);
        container.addView(presetSpinner);

        // Slider Percentage Label
        TextView sliderText = new TextView(context);
        sliderText.setText(context.getString(R.string.app_resolution_slider_label, currentPercent));
        sliderText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        sliderText.setTypeface(Typeface.DEFAULT_BOLD);
        sliderText.setGravity(Gravity.CENTER_HORIZONTAL);
        sliderText.setPadding(0, pad / 2, 0, pad / 4);
        container.addView(sliderText);

        // Continuous Slider (20% to 100%, step 5%) -> 0 to 16 mapped to 20 to 100
        SeekBar seekBar = new SeekBar(context);
        seekBar.setMax(16); // 16 * 5 = 80 (+ 20 = 100)
        int initialProgress = (currentPercent - 20) / 5;
        seekBar.setProgress(Math.max(0, Math.min(16, initialProgress)));
        container.addView(seekBar);

        // Select initial preset in spinner
        if (currentPercent >= 98) {
            presetSpinner.setSelection(0);
        } else if (currentPercent >= 85 && currentPercent <= 90) {
            presetSpinner.setSelection(1);
        } else if (currentPercent >= 57 && currentPercent <= 61) {
            presetSpinner.setSelection(2);
        } else if (currentPercent >= 37 && currentPercent <= 41) {
            presetSpinner.setSelection(3);
        } else {
            presetSpinner.setSelection(4);
        }

        final boolean[] isUpdatingFromSpinner = {false};
        final boolean[] isUpdatingFromSlider = {false};

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (!isUpdatingFromSpinner[0]) {
                    int percent = 20 + progress * 5;
                    sliderText.setText(context.getString(R.string.app_resolution_slider_label, percent));
                    isUpdatingFromSlider[0] = true;
                    if (percent == 100) {
                        presetSpinner.setSelection(0);
                    } else if (percent == 88 || percent == 90) {
                        presetSpinner.setSelection(1);
                    } else if (percent == 60) {
                        presetSpinner.setSelection(2);
                    } else if (percent == 40) {
                        presetSpinner.setSelection(3);
                    } else {
                        presetSpinner.setSelection(4);
                    }
                    isUpdatingFromSlider[0] = false;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {}

            @Override
            public void onStopTrackingTouch(SeekBar sb) {}
        });

        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isUpdatingFromSlider[0]) return;
                isUpdatingFromSpinner[0] = true;
                int targetPercent = 100;
                switch (position) {
                    case 0: targetPercent = 100; break;
                    case 1: targetPercent = 88; break;
                    case 2: targetPercent = 59; break;
                    case 3: targetPercent = 39; break;
                    default:
                        targetPercent = 20 + seekBar.getProgress() * 5;
                        break;
                }
                int targetProgress = (targetPercent - 20) / 5;
                seekBar.setProgress(Math.max(0, Math.min(16, targetProgress)));
                sliderText.setText(context.getString(R.string.app_resolution_slider_label, targetPercent));
                isUpdatingFromSpinner[0] = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        new AlertDialog.Builder(context)
                .setTitle(appInfo.label)
                .setView(container)
                .setPositiveButton(R.string.okay, (dialog, which) -> {
                    int selectedPos = presetSpinner.getSelectedItemPosition();
                    float finalScale = 1.0f;
                    if (selectedPos == 0) {
                        finalScale = 1.0f;
                    } else if (selectedPos == 1) {
                        finalScale = 0.88f;
                    } else if (selectedPos == 2) {
                        finalScale = 0.59f;
                    } else if (selectedPos == 3) {
                        finalScale = 0.39f;
                    } else {
                        finalScale = (20 + seekBar.getProgress() * 5) / 100.0f;
                    }

                    saveAppScale(context, appInfo.packageName, finalScale);
                    preference.setSummary(formatSummary(context, finalScale));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static Map<String, Float> loadScaleMap(Context context) {
        Map<String, Float> map = new HashMap<>();
        String data = Settings.Global.getString(context.getContentResolver(), SETTING_KEY);
        if (TextUtils.isEmpty(data)) {
            data = Settings.System.getString(context.getContentResolver(), SETTING_KEY);
        }
        if (!TextUtils.isEmpty(data)) {
            String[] entries = data.split(",");
            for (String entry : entries) {
                String[] parts = entry.trim().split(":");
                if (parts.length == 2) {
                    try {
                        map.put(parts[0].trim(), Float.parseFloat(parts[1].trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return map;
    }

    private static void saveAppScale(Context context, String packageName, float scale) {
        Map<String, Float> map = loadScaleMap(context);
        if (scale >= 0.999f) {
            map.remove(packageName);
        } else {
            map.put(packageName, Math.round(scale * 100) / 100.0f);
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Float> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append(entry.getKey()).append(":").append(entry.getValue());
            first = false;
        }

        String serialized = sb.toString();
        Settings.Global.putString(context.getContentResolver(), SETTING_KEY, serialized);
        Settings.System.putString(context.getContentResolver(), SETTING_KEY, serialized);
    }

    private static class AppInfo {
        final String label;
        final String packageName;
        final ApplicationInfo appInfo;

        AppInfo(String label, String packageName, ApplicationInfo appInfo) {
            this.label = label;
            this.packageName = packageName;
            this.appInfo = appInfo;
        }
    }
}
