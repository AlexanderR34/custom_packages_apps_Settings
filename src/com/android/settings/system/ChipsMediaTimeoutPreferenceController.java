/*
 * Copyright (C) 2024-2026 The Android Open Source Project
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

package com.android.settings.system;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.android.settings.core.BasePreferenceController;

/**
 * Controller for Status Bar Music Inactivity Timeout (5s, 15s, 30s, 60s, 0=Always) in Settings.
 */
public class ChipsMediaTimeoutPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public static final String SETTING_KEY = "status_bar_chips_media_timeout";
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private ListPreference mPreference;

    public ChipsMediaTimeoutPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            int currentVal = Settings.System.getIntForUser(
                    mContext.getContentResolver(), SETTING_KEY, DEFAULT_TIMEOUT_SECONDS, UserHandle.myUserId());
            mPreference.setValue(String.valueOf(currentVal));
            updateSummary(mPreference, String.valueOf(currentVal));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String stringValue = (String) newValue;
        int value = Integer.parseInt(stringValue);
        Settings.System.putIntForUser(
                mContext.getContentResolver(), SETTING_KEY, value, UserHandle.myUserId());
        updateSummary(preference, stringValue);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mPreference != null) {
            int currentVal = Settings.System.getIntForUser(
                    mContext.getContentResolver(), SETTING_KEY, DEFAULT_TIMEOUT_SECONDS, UserHandle.myUserId());
            mPreference.setValue(String.valueOf(currentVal));
            updateSummary(mPreference, String.valueOf(currentVal));
        }
    }

    private void updateSummary(Preference preference, String value) {
        if (preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            int index = listPref.findIndexOfValue(value);
            if (index >= 0) {
                listPref.setSummary(listPref.getEntries()[index]);
            }
        }
    }
}
