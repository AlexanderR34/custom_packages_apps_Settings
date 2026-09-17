/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

public class Sm64CoinsNotificationModePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public static final String KEY = "sm64_red_coins_sound_mode";
    public static final String SETTING_KEY = "sm64_red_coins_sound_mode";
    public static final int DEFAULT_VALUE = 0;

    public Sm64CoinsNotificationModePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        if (preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            int mode = Settings.System.getInt(mContext.getContentResolver(), SETTING_KEY, DEFAULT_VALUE);
            listPref.setValue(String.valueOf(mode));
            listPref.setSummary(listPref.getEntry());

            boolean isEnabled = Settings.System.getInt(
                    mContext.getContentResolver(), "sm64_red_coins_sound_enabled", 0) == 1;
            listPref.setEnabled(isEnabled);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int value = Integer.parseInt((String) newValue);
        Settings.System.putInt(mContext.getContentResolver(), SETTING_KEY, value);
        if (preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            int index = listPref.findIndexOfValue((String) newValue);
            if (index >= 0) {
                listPref.setSummary(listPref.getEntries()[index]);
            }
        }
        return true;
    }
}
