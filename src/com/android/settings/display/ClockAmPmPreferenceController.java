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
import android.provider.Settings;
import android.text.format.DateFormat;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

/**
 * Controller for configuring status bar clock AM/PM style (Normal, Small, Hidden).
 */
public class ClockAmPmPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public ClockAmPmPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return DateFormat.is24HourFormat(mContext) ? DISABLED_DEPENDENT_SETTING : AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        if (preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            int amPmStyle = Settings.System.getInt(
                    mContext.getContentResolver(),
                    Settings.System.STATUS_BAR_AM_PM,
                    2); // Default to 2 (Gone) in AOSP
            listPref.setValue(String.valueOf(amPmStyle));
            listPref.setSummary(listPref.getEntry());
            listPref.setEnabled(!DateFormat.is24HourFormat(mContext));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            int amPmStyle = Integer.parseInt((String) newValue);
            Settings.System.putInt(
                    mContext.getContentResolver(),
                    Settings.System.STATUS_BAR_AM_PM,
                    amPmStyle);
            if (preference instanceof ListPreference) {
                ListPreference listPref = (ListPreference) preference;
                int index = listPref.findIndexOfValue((String) newValue);
                if (index >= 0 && index < listPref.getEntries().length) {
                    listPref.setSummary(listPref.getEntries()[index]);
                }
            }
        } catch (Exception ignored) {
        }
        return true;
    }
}
