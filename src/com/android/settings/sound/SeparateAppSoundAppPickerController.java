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

package com.android.settings.sound;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.DropDownPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeparateAppSoundAppPickerController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private final PackageManager mPackageManager;

    public SeparateAppSoundAppPickerController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPackageManager = context.getPackageManager();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof DropDownPreference)) return;
        DropDownPreference dropDown = (DropDownPreference) preference;

        List<AppEntry> apps = getInstalledMediaApps();
        if (apps.isEmpty()) {
            dropDown.setEnabled(false);
            return;
        }

        CharSequence[] entries = new CharSequence[apps.size()];
        CharSequence[] entryValues = new CharSequence[apps.size()];

        String currentPackage = Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.SEPARATE_APP_SOUND_PACKAGE);

        int selectedIndex = -1;
        for (int i = 0; i < apps.size(); i++) {
            entries[i] = apps.get(i).label;
            entryValues[i] = apps.get(i).packageName;
            if (TextUtils.equals(currentPackage, apps.get(i).packageName)) {
                selectedIndex = i;
            }
        }

        dropDown.setEntries(entries);
        dropDown.setEntryValues(entryValues);

        if (selectedIndex >= 0) {
            dropDown.setValueIndex(selectedIndex);
            dropDown.setSummary(entries[selectedIndex]);
        } else if (entryValues.length > 0) {
            dropDown.setValueIndex(0);
            dropDown.setSummary(entries[0]);
            Settings.Secure.putString(mContext.getContentResolver(),
                    Settings.Secure.SEPARATE_APP_SOUND_PACKAGE, entryValues[0].toString());
        }

        dropDown.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String packageName = (String) newValue;
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.SEPARATE_APP_SOUND_PACKAGE, packageName);
        updateState(preference);
        return true;
    }

    private List<AppEntry> getInstalledMediaApps() {
        Map<String, AppEntry> appMap = new HashMap<>();

        Intent mediaIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC);
        List<ResolveInfo> resolved = mPackageManager.queryIntentActivities(mediaIntent, PackageManager.ResolveInfoFlags.of(0));
        for (ResolveInfo info : resolved) {
            String pkg = info.activityInfo.packageName;
            CharSequence label = info.loadLabel(mPackageManager);
            appMap.put(pkg, new AppEntry(pkg, label));
        }

        List<ApplicationInfo> installedApps = mPackageManager.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(0));
        for (ApplicationInfo app : installedApps) {
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0 
                    || app.category == ApplicationInfo.CATEGORY_AUDIO 
                    || app.category == ApplicationInfo.CATEGORY_VIDEO) {
                if (!appMap.containsKey(app.packageName)) {
                    CharSequence label = app.loadLabel(mPackageManager);
                    appMap.put(app.packageName, new AppEntry(app.packageName, label));
                }
            }
        }

        List<AppEntry> list = new ArrayList<>(appMap.values());
        list.sort(Comparator.comparing(a -> a.label.toString().toLowerCase()));
        return list;
    }

    private static class AppEntry {
        final String packageName;
        final CharSequence label;

        AppEntry(String packageName, CharSequence label) {
            this.packageName = packageName;
            this.label = label;
        }
    }
}
