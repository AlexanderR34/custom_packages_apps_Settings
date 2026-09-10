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

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;

public class HyperOSBatteryPreferenceController extends BasePreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    public static final String KEY_SETTING = "status_bar_battery_style_hyperos";

    public HyperOSBatteryPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        int setting = Settings.System.getIntForUser(mContext.getContentResolver(),
                KEY_SETTING, 0, UserHandle.USER_CURRENT);
        if (preference instanceof TwoStatePreference) {
            ((TwoStatePreference) preference).setChecked(setting == 1);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (Boolean) newValue;
        Settings.System.putIntForUser(mContext.getContentResolver(),
                KEY_SETTING, enabled ? 1 : 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(mContext.getContentResolver(),
                "status_bar_battery_style", enabled ? 1 : 0, UserHandle.USER_CURRENT);
        return true;
    }
}
