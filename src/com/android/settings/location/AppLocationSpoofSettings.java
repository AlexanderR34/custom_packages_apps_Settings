/*
 * Copyright (C) 2026 Project Diva
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

package com.android.settings.location;

import android.Manifest;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppLocationSpoofSettings extends SettingsPreferenceFragment {

    private static final String SETTING_SPOOF_PKG_PREFIX = "location_spoof_pkg_";
    private static final String SETTING_SPOOF_COORDS_PREFIX = "location_spoof_coords_";

    private PreferenceCategory mAppsCategory;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.LOCATION;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.app_location_spoof_settings);
        mAppsCategory = findPreference("category_spoof_apps");
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAppList();
    }

    private void refreshAppList() {
        if (mAppsCategory == null) return;
        mAppsCategory.removeAll();

        Context context = getContext();
        if (context == null) return;

        PackageManager pm = context.getPackageManager();
        List<PackageInfo> installedPackages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);

        List<AppItem> locationApps = new ArrayList<>();

        for (PackageInfo pkgInfo : installedPackages) {
            if (pkgInfo.requestedPermissions == null) continue;
            // Ignore system package "android" or Settings itself
            if ("android".equals(pkgInfo.packageName) || context.getPackageName().equals(pkgInfo.packageName)) {
                continue;
            }

            boolean hasLocation = false;
            for (String perm : pkgInfo.requestedPermissions) {
                if (Manifest.permission.ACCESS_FINE_LOCATION.equals(perm)
                        || Manifest.permission.ACCESS_COARSE_LOCATION.equals(perm)) {
                    hasLocation = true;
                    break;
                }
            }

            if (hasLocation) {
                CharSequence label = pkgInfo.applicationInfo.loadLabel(pm);
                Drawable icon = pkgInfo.applicationInfo.loadIcon(pm);
                locationApps.add(new AppItem(pkgInfo.packageName, label != null ? label.toString() : pkgInfo.packageName, icon));
            }
        }

        Collections.sort(locationApps, Comparator.comparing(a -> a.label.toLowerCase()));

        for (AppItem app : locationApps) {
            SwitchPreferenceCompat pref = new SwitchPreferenceCompat(getPrefContext());
            pref.setKey("pref_spoof_" + app.packageName);
            pref.setTitle(app.label);
            pref.setIcon(app.icon);

            boolean isSpoofEnabled = Settings.Secure.getIntForUser(
                    context.getContentResolver(),
                    SETTING_SPOOF_PKG_PREFIX + app.packageName,
                    0,
                    UserHandle.USER_CURRENT) == 1;

            String coords = Settings.Secure.getStringForUser(
                    context.getContentResolver(),
                    SETTING_SPOOF_COORDS_PREFIX + app.packageName,
                    UserHandle.USER_CURRENT);

            updateSummary(pref, isSpoofEnabled, coords);
            pref.setChecked(isSpoofEnabled);

            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                Settings.Secure.putIntForUser(
                        context.getContentResolver(),
                        SETTING_SPOOF_PKG_PREFIX + app.packageName,
                        enabled ? 1 : 0,
                        UserHandle.USER_CURRENT);

                String currentCoords = Settings.Secure.getStringForUser(
                        context.getContentResolver(),
                        SETTING_SPOOF_COORDS_PREFIX + app.packageName,
                        UserHandle.USER_CURRENT);

                updateSummary((SwitchPreferenceCompat) preference, enabled, currentCoords);
                return true;
            });

            pref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(context, LocationMapPickerActivity.class);
                intent.putExtra(LocationMapPickerActivity.EXTRA_PACKAGE_NAME, app.packageName);
                intent.putExtra(LocationMapPickerActivity.EXTRA_APP_TITLE, app.label);
                startActivity(intent);
                return true;
            });

            mAppsCategory.addPreference(pref);
        }
    }

    private void updateSummary(SwitchPreferenceCompat pref, boolean isSpoofEnabled, String coords) {
        if (!isSpoofEnabled) {
            pref.setSummary(R.string.location_spoof_status_disabled);
        } else if (!TextUtils.isEmpty(coords)) {
            String[] parts = coords.split(",");
            if (parts.length >= 2) {
                pref.setSummary(getString(R.string.location_spoof_status_active_coords, parts[0].trim(), parts[1].trim()));
            } else {
                pref.setSummary(R.string.location_spoof_status_active_no_coords);
            }
        } else {
            pref.setSummary(R.string.location_spoof_status_active_no_coords);
        }
    }

    private static class AppItem {
        final String packageName;
        final String label;
        final Drawable icon;

        AppItem(String packageName, String label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }
}
