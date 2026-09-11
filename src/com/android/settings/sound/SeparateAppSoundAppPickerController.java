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

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SeparateAppSoundAppPickerController extends BasePreferenceController {

    private static final int MAX_SELECTED_APPS = 5;
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
        super.updateState(preference);
        List<String> selectedPackages = getSelectedPackages();
        if (selectedPackages.isEmpty()) {
            preference.setSummary(mContext.getString(R.string.separate_app_sound_no_apps_selected));
            return;
        }

        List<String> labels = new ArrayList<>();
        for (String pkg : selectedPackages) {
            try {
                ApplicationInfo ai = mPackageManager.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0));
                CharSequence label = ai.loadLabel(mPackageManager);
                if (!TextUtils.isEmpty(label)) {
                    labels.add(label.toString());
                } else {
                    labels.add(pkg);
                }
            } catch (PackageManager.NameNotFoundException e) {
                // App uninstalled or invalid
            }
        }

        if (labels.isEmpty()) {
            preference.setSummary(mContext.getString(R.string.separate_app_sound_no_apps_selected));
        } else {
            preference.setSummary(String.join(", ", labels));
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }

        showAppSelectionDialog(preference);
        return true;
    }

    private void showAppSelectionDialog(Preference preference) {
        List<AppEntry> apps = getInstalledMediaApps();
        if (apps.isEmpty()) {
            Toast.makeText(mContext, R.string.separate_app_sound_no_apps, Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] entries = new CharSequence[apps.size()];
        boolean[] checkedItems = new boolean[apps.size()];
        Set<String> selectedSet = new HashSet<>(getSelectedPackages());

        final int[] checkedCount = new int[]{0};
        for (int i = 0; i < apps.size(); i++) {
            entries[i] = apps.get(i).label;
            if (selectedSet.contains(apps.get(i).packageName)) {
                checkedItems[i] = true;
                checkedCount[0]++;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getString(R.string.separate_app_sound_app_select_title));
        builder.setMultiChoiceItems(entries, checkedItems, (dialog, which, isChecked) -> {
            if (isChecked) {
                if (checkedCount[0] >= MAX_SELECTED_APPS) {
                    ((AlertDialog) dialog).getListView().setItemChecked(which, false);
                    checkedItems[which] = false;
                    Toast.makeText(mContext, mContext.getString(R.string.separate_app_sound_max_apps_reached),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                checkedCount[0]++;
                checkedItems[which] = true;
            } else {
                checkedCount[0]--;
                checkedItems[which] = false;
            }
        });

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            List<String> newSelected = new ArrayList<>();
            for (int i = 0; i < checkedItems.length; i++) {
                if (checkedItems[i]) {
                    newSelected.add(apps.get(i).packageName);
                }
            }
            saveSelectedPackages(newSelected);
            updateState(preference);
        });

        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private List<String> getSelectedPackages() {
        String raw = Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.SEPARATE_APP_SOUND_PACKAGE);
        if (TextUtils.isEmpty(raw)) {
            return Collections.emptyList();
        }
        String[] parts = raw.split("[,;]");
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!TextUtils.isEmpty(trimmed) && !result.contains(trimmed)) {
                result.add(trimmed);
                if (result.size() >= MAX_SELECTED_APPS) break;
            }
        }
        return result;
    }

    private void saveSelectedPackages(List<String> packages) {
        String joined = String.join(",", packages);
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.SEPARATE_APP_SOUND_PACKAGE, joined);
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
