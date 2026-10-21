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
 * Controller for Status Bar Chips Progress Bar Color Mode (0: Monet/Artwork, 1: Rainbow, 2: Custom HEX).
 */
public class ChipsProgressBarColorModePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public static final String SETTING_KEY = Settings.System.STATUS_BAR_CHIPS_PROGRESS_BAR_COLOR_MODE;
    public static final int DEFAULT_MODE = 0; // Monet / App artwork

    private ListPreference mPreference;

    public ChipsProgressBarColorModePreferenceController(Context context, String preferenceKey) {
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
                    mContext.getContentResolver(), SETTING_KEY, DEFAULT_MODE, UserHandle.myUserId());
            mPreference.setValue(String.valueOf(currentVal));
            updateSummary(mPreference, String.valueOf(currentVal));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String valStr = (String) newValue;
        try {
            int val = Integer.parseInt(valStr);
            Settings.System.putIntForUser(
                    mContext.getContentResolver(), SETTING_KEY, val, UserHandle.myUserId());
            updateSummary(mPreference, valStr);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mPreference != null) {
            int currentVal = Settings.System.getIntForUser(
                    mContext.getContentResolver(), SETTING_KEY, DEFAULT_MODE, UserHandle.myUserId());
            mPreference.setValue(String.valueOf(currentVal));
            updateSummary(mPreference, String.valueOf(currentVal));
        }
    }

    private void updateSummary(ListPreference preference, String value) {
        if (preference == null) return;
        int index = preference.findIndexOfValue(value);
        if (index >= 0 && preference.getEntries() != null && index < preference.getEntries().length) {
            preference.setSummary(preference.getEntries()[index]);
        }
    }
}
