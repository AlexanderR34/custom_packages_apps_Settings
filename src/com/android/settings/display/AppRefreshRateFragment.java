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

public class AppRefreshRateFragment extends SettingsPreferenceFragment {

    private static final String PREF_KEY_APP_PREFIX = "app_rr_";

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

        CharSequence[] entries = new CharSequence[] {
                context.getString(R.string.app_refresh_rate_default),
                "60 Hz",
                "90 Hz",
                "120 Hz"
        };

        CharSequence[] entryValues = new CharSequence[] {
                "0",
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

            int currentRate = Settings.System.getInt(
                    context.getContentResolver(),
                    "app_refresh_rate_" + appInfo.packageName, 0);

            preference.setValue(String.valueOf(currentRate));
            updateSummary(preference, currentRate);

            preference.setOnPreferenceChangeListener((pref, newValue) -> {
                int rate = Integer.parseInt((String) newValue);
                Settings.System.putInt(
                        context.getContentResolver(),
                        "app_refresh_rate_" + appInfo.packageName, rate);
                updateSummary((ListPreference) pref, rate);
                return true;
            });

            mAppCategory.addPreference(preference);
        }
    }

    private void updateSummary(ListPreference preference, int rate) {
        if (rate == 60) {
            preference.setSummary("60 Hz");
        } else if (rate == 90) {
            preference.setSummary("90 Hz");
        } else if (rate == 120) {
            preference.setSummary("120 Hz");
        } else {
            preference.setSummary(requireContext().getString(R.string.app_refresh_rate_default));
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
