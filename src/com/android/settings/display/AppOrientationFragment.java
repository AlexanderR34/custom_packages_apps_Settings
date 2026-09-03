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
import java.util.List;

public class AppOrientationFragment extends SettingsPreferenceFragment {

    private static final String PREF_KEY_APP_PREFIX = "app_orientation_";

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
        mAppCategory.setTitle(R.string.app_orientation_category_title);
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
                context.getString(R.string.app_orientation_default),
                context.getString(R.string.app_orientation_portrait),
                context.getString(R.string.app_orientation_landscape),
                context.getString(R.string.app_orientation_reverse_portrait),
                context.getString(R.string.app_orientation_reverse_landscape),
                context.getString(R.string.app_orientation_full_sensor),
                context.getString(R.string.app_orientation_sensor_landscape)
        };

        CharSequence[] entryValues = new CharSequence[] {
                "-1",
                "1",
                "0",
                "9",
                "8",
                "10",
                "6"
        };

        for (AppInfo appInfo : appList) {
            ListPreference preference = new ListPreference(context);
            preference.setKey(PREF_KEY_APP_PREFIX + appInfo.packageName);
            preference.setTitle(appInfo.label);
            preference.setIcon(appInfo.appInfo.loadIcon(mPackageManager));
            preference.setEntries(entries);
            preference.setEntryValues(entryValues);

            int currentOrientation = Settings.System.getInt(
                    context.getContentResolver(),
                    "app_orientation_" + appInfo.packageName, -1);

            preference.setValue(String.valueOf(currentOrientation));
            updateSummary(preference, currentOrientation);

            preference.setOnPreferenceChangeListener((pref, newValue) -> {
                int orientation = Integer.parseInt((String) newValue);
                Settings.System.putInt(
                        context.getContentResolver(),
                        "app_orientation_" + appInfo.packageName, orientation);
                updateSummary((ListPreference) pref, orientation);
                return true;
            });

            mAppCategory.addPreference(preference);
        }
    }

    private void updateSummary(ListPreference preference, int orientation) {
        Context context = requireContext();
        switch (orientation) {
            case 1:
                preference.setSummary(context.getString(R.string.app_orientation_portrait));
                break;
            case 0:
                preference.setSummary(context.getString(R.string.app_orientation_landscape));
                break;
            case 9:
                preference.setSummary(context.getString(R.string.app_orientation_reverse_portrait));
                break;
            case 8:
                preference.setSummary(context.getString(R.string.app_orientation_reverse_landscape));
                break;
            case 10:
                preference.setSummary(context.getString(R.string.app_orientation_full_sensor));
                break;
            case 6:
                preference.setSummary(context.getString(R.string.app_orientation_sensor_landscape));
                break;
            default:
                preference.setSummary(context.getString(R.string.app_orientation_default));
                break;
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
