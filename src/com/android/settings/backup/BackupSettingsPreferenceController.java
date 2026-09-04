/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.backup;

import android.content.Context;
import android.content.Intent;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

public class BackupSettingsPreferenceController extends BasePreferenceController {
    private static final String BACKUP_SETTINGS = "backup_settings";
    private static final String MANUFACTURER_SETTINGS = "manufacturer_backup";
    private Intent mBackupSettingsIntent;
    private CharSequence mBackupSettingsTitle;
    private String mBackupSettingsSummary;
    private Intent mManufacturerIntent;
    private String mManufacturerLabel;

    public BackupSettingsPreferenceController(Context context, String key) {
        super(context, key);
        BackupSettingsHelper settingsHelper = new BackupSettingsHelper(context);
        mBackupSettingsIntent = settingsHelper.getIntentForBackupSettings();
        mBackupSettingsTitle = settingsHelper.getLabelForBackupSettings();
        mBackupSettingsSummary = settingsHelper.getSummaryForBackupSettings();
        mManufacturerIntent = settingsHelper.getIntentProvidedByManufacturer();
        mManufacturerLabel = settingsHelper.getLabelProvidedByManufacturer();
    }

    public BackupSettingsPreferenceController(Context context) {
        this(context, BACKUP_SETTINGS);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference backupSettings = screen.findPreference(BACKUP_SETTINGS);
        if (backupSettings != null) {
            backupSettings.setIntent(mBackupSettingsIntent);
            if (mBackupSettingsTitle != null) {
                backupSettings.setTitle(mBackupSettingsTitle);
            }
            if (mBackupSettingsSummary != null) {
                backupSettings.setSummary(mBackupSettingsSummary);
            }
        }
        Preference manufacturerSettings = screen.findPreference(MANUFACTURER_SETTINGS);
        if (manufacturerSettings != null) {
            manufacturerSettings.setIntent(mManufacturerIntent);
            if (mManufacturerLabel != null) {
                manufacturerSettings.setTitle(mManufacturerLabel);
            }
        }
    }
}
