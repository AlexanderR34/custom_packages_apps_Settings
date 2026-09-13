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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;

import com.android.settings.utils.SystemUiRestartUtils;

public class BatteryStylePreferenceController extends BasePreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    public static final String KEY_SETTING = "status_bar_battery_style";
    public static final String KEY_SETTING_LEGACY = "status_bar_battery_style_hyperos";

    public BatteryStylePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        if (preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            int style = Settings.System.getIntForUser(mContext.getContentResolver(),
                    KEY_SETTING, -1, UserHandle.USER_CURRENT);
            if (style == -1) {
                int hyperOs = Settings.System.getIntForUser(mContext.getContentResolver(),
                    KEY_SETTING_LEGACY, 0, UserHandle.USER_CURRENT);
                style = hyperOs == 1 ? 1 : 0;
            }
            listPref.setValue(String.valueOf(style));
            listPref.setSummary(listPref.getEntry());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String val = (String) newValue;
        int style = Integer.parseInt(val);
        Settings.System.putIntForUser(mContext.getContentResolver(),
                KEY_SETTING, style, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(mContext.getContentResolver(),
                KEY_SETTING_LEGACY, (style == 1) ? 1 : 0, UserHandle.USER_CURRENT);
        if (preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            int index = listPref.findIndexOfValue(val);
            if (index >= 0) {
                listPref.setSummary(listPref.getEntries()[index]);
            }
        }
        return true;
    }
}
