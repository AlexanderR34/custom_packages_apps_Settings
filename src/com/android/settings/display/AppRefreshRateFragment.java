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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
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
 * Unified Per-App Display Settings: Refresh Rate (Hz) & Game Frame Pacer (FPS Limit).
 */
public class AppRefreshRateFragment extends SettingsPreferenceFragment {

    private static final String PREF_KEY_APP_PREFIX = "app_display_";

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
        mAppCategory.setTitle(R.string.app_refresh_rate_category_title);
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

        for (AppInfo appInfo : appList) {
            Preference preference = new Preference(context);
            preference.setKey(PREF_KEY_APP_PREFIX + appInfo.packageName);
            preference.setTitle(appInfo.label);
            preference.setIcon(appInfo.appInfo.loadIcon(mPackageManager));

            int currentRate = getAppRefreshRate(context, appInfo.packageName);
            int currentFps = getAppFpsLimit(context, appInfo.packageName);

            updateSummary(preference, currentRate, currentFps);

            preference.setOnPreferenceClickListener(pref -> {
                showConfigDialog(context, appInfo, preference);
                return true;
            });

            mAppCategory.addPreference(preference);
        }
    }

    private void showConfigDialog(Context context, AppInfo appInfo, Preference preference) {
        int currentRate = getAppRefreshRate(context, appInfo.packageName);
        int currentFps = getAppFpsLimit(context, appInfo.packageName);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padH = dp(context, 24);
        int padV = dp(context, 16);
        layout.setPadding(padH, padV, padH, padV);

        // Section 1: Refresh Rate (Hz)
        TextView tvRateLabel = new TextView(context);
        tvRateLabel.setText(R.string.app_refresh_rate_hz_label);
        tvRateLabel.setTypeface(null, Typeface.BOLD);
        tvRateLabel.setTextSize(14f);
        tvRateLabel.setPadding(0, 0, 0, dp(context, 6));
        layout.addView(tvRateLabel);

        Spinner rateSpinner = new Spinner(context);
        String[] rateEntries = new String[] {
                context.getString(R.string.app_refresh_rate_default),
                "60 Hz",
                "90 Hz",
                "120 Hz"
        };
        int[] rateValues = new int[] { 0, 60, 90, 120 };
        ArrayAdapter<String> rateAdapter = new ArrayAdapter<>(
                context, android.R.layout.simple_spinner_dropdown_item, rateEntries);
        rateSpinner.setAdapter(rateAdapter);

        int selectedRateIndex = 0;
        for (int i = 0; i < rateValues.length; i++) {
            if (rateValues[i] == currentRate) {
                selectedRateIndex = i;
                break;
            }
        }
        rateSpinner.setSelection(selectedRateIndex);
        layout.addView(rateSpinner);

        // Divider spacing
        View spacer = new View(context);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 20)));
        layout.addView(spacer);

        // Section 2: FPS Limit (Frame Pacer)
        TextView tvFpsLabel = new TextView(context);
        tvFpsLabel.setText(R.string.game_frame_pacer_title);
        tvFpsLabel.setTypeface(null, Typeface.BOLD);
        tvFpsLabel.setTextSize(14f);
        tvFpsLabel.setPadding(0, 0, 0, dp(context, 6));
        layout.addView(tvFpsLabel);

        Spinner fpsSpinner = new Spinner(context);
        String[] fpsEntries = new String[] {
                context.getString(R.string.game_frame_pacer_default),
                "30 FPS (33.3 ms)",
                "45 FPS (22.2 ms)",
                "60 FPS (16.6 ms)",
                "90 FPS (11.1 ms)",
                "120 FPS (8.3 ms)"
        };
        int[] fpsValues = new int[] { 0, 30, 45, 60, 90, 120 };
        ArrayAdapter<String> fpsAdapter = new ArrayAdapter<>(
                context, android.R.layout.simple_spinner_dropdown_item, fpsEntries);
        fpsSpinner.setAdapter(fpsAdapter);

        int selectedFpsIndex = 0;
        for (int i = 0; i < fpsValues.length; i++) {
            if (fpsValues[i] == currentFps) {
                selectedFpsIndex = i;
                break;
            }
        }
        fpsSpinner.setSelection(selectedFpsIndex);
        layout.addView(fpsSpinner);

        new AlertDialog.Builder(context)
                .setTitle(appInfo.label)
                .setIcon(appInfo.appInfo.loadIcon(mPackageManager))
                .setView(layout)
                .setPositiveButton(R.string.app_display_save_btn, (dialog, which) -> {
                    int newRate = rateValues[rateSpinner.getSelectedItemPosition()];
                    int newFps = fpsValues[fpsSpinner.getSelectedItemPosition()];

                    setAppRefreshRate(context, appInfo.packageName, newRate);
                    setAppFpsLimit(context, appInfo.packageName, newFps);

                    updateSummary(preference, newRate, newFps);
                })
                .setNegativeButton(R.string.app_display_cancel_btn, null)
                .show();
    }

    private int getAppRefreshRate(Context context, String packageName) {
        return Settings.System.getInt(
                context.getContentResolver(),
                "app_refresh_rate_" + packageName, 0);
    }

    private void setAppRefreshRate(Context context, String packageName, int rate) {
        Settings.System.putInt(
                context.getContentResolver(),
                "app_refresh_rate_" + packageName, rate);
    }

    private int getAppFpsLimit(Context context, String packageName) {
        int fps = Settings.System.getInt(
                context.getContentResolver(),
                "game_fps_limit_" + packageName, 0);
        if (fps > 0) {
            return fps;
        }

        String pacerApps = Settings.System.getString(context.getContentResolver(), "game_frame_pacer_apps");
        if (pacerApps != null && !pacerApps.isEmpty()) {
            String[] entries = pacerApps.split(",");
            for (String entry : entries) {
                String[] parts = entry.trim().split("=");
                if (parts.length == 2 && packageName.equals(parts[0].trim())) {
                    try {
                        return Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return 0;
    }

    private void setAppFpsLimit(Context context, String packageName, int fps) {
        Settings.System.putInt(
                context.getContentResolver(),
                "game_fps_limit_" + packageName, fps);

        // Synchronize game_frame_pacer_apps property string
        String pacerApps = Settings.System.getString(context.getContentResolver(), "game_frame_pacer_apps");
        Map<String, Integer> map = new HashMap<>();
        if (pacerApps != null && !pacerApps.isEmpty()) {
            String[] entries = pacerApps.split(",");
            for (String entry : entries) {
                String[] parts = entry.trim().split("=");
                if (parts.length == 2) {
                    try {
                        map.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        if (fps > 0) {
            map.put(packageName, fps);
        } else {
            map.remove(packageName);
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        Settings.System.putString(context.getContentResolver(), "game_frame_pacer_apps", sb.toString());
    }

    private void updateSummary(Preference preference, int rate, int fps) {
        Context context = requireContext();
        if (rate == 0 && fps == 0) {
            preference.setSummary(context.getString(R.string.app_refresh_rate_default));
        } else if (rate > 0 && fps > 0) {
            preference.setSummary(rate + " Hz • " + fps + " FPS");
        } else if (rate > 0) {
            preference.setSummary(rate + " Hz • " + context.getString(R.string.game_frame_pacer_default));
        } else {
            preference.setSummary(context.getString(R.string.app_refresh_rate_default) + " • " + fps + " FPS");
        }
    }

    private int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }

    private static class AppInfo {
        String label;
        String packageName;
        ApplicationInfo appInfo;

        AppInfo(String label, String packageName, ApplicationInfo appInfo) {
            this.label = label;
            this.packageName = packageName;
            this.appInfo = appInfo;
        }
    }
}
