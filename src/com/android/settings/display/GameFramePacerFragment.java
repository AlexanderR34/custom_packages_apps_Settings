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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.ListPreference;
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

public class GameFramePacerFragment extends SettingsPreferenceFragment {

    private static final String PREF_KEY_APP_PREFIX = "game_fps_";

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
        mAppCategory.setTitle(R.string.game_frame_pacer_category_title);
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

        CharSequence[] entries = new CharSequence[] {
                context.getString(R.string.game_frame_pacer_default),
                "30 FPS (33.3 ms)",
                "45 FPS (22.2 ms)",
                "60 FPS (16.6 ms)",
                "90 FPS (11.1 ms)",
                "120 FPS (8.3 ms)"
        };

        CharSequence[] entryValues = new CharSequence[] {
                "0",
                "30",
                "45",
                "60",
                "90",
                "120"
        };

        for (AppInfo appInfo : appList) {
            ListPreference preference = new ListPreference(context);
            preference.setKey(PREF_KEY_APP_PREFIX + appInfo.packageName);
            preference.setTitle(appInfo.label);
            preference.setIcon(appInfo.appInfo.loadIcon(mPackageManager));
            preference.setEntries(entries);
            preference.setEntryValues(entryValues);

            int currentFps = getAppFpsLimit(context, appInfo.packageName);

            preference.setValue(String.valueOf(currentFps));
            updateSummary(preference, currentFps);

            preference.setOnPreferenceChangeListener((pref, newValue) -> {
                int fps = Integer.parseInt((String) newValue);
                setAppFpsLimit(context, appInfo.packageName, fps);
                updateSummary((ListPreference) pref, fps);
                return true;
            });

            mAppCategory.addPreference(preference);
        }
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
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return 0;
    }

    private void setAppFpsLimit(Context context, String packageName, int fps) {
        Settings.System.putInt(
                context.getContentResolver(),
                "game_fps_limit_" + packageName, fps);

        // Synchronize game_frame_pacer_apps
        String pacerApps = Settings.System.getString(context.getContentResolver(), "game_frame_pacer_apps");
        Map<String, Integer> map = new HashMap<>();
        if (pacerApps != null && !pacerApps.isEmpty()) {
            String[] entries = pacerApps.split(",");
            for (String entry : entries) {
                String[] parts = entry.trim().split("=");
                if (parts.length == 2) {
                    try {
                        map.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                    } catch (NumberFormatException ignored) {
                    }
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

    private void updateSummary(ListPreference preference, int fps) {
        if (fps == 30) {
            preference.setSummary("30 FPS (33.3 ms)");
        } else if (fps == 45) {
            preference.setSummary("45 FPS (22.2 ms)");
        } else if (fps == 60) {
            preference.setSummary("60 FPS (16.6 ms)");
        } else if (fps == 90) {
            preference.setSummary("90 FPS (11.1 ms)");
        } else if (fps == 120) {
            preference.setSummary("120 FPS (8.3 ms)");
        } else {
            preference.setSummary(requireContext().getString(R.string.game_frame_pacer_default));
        }
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
