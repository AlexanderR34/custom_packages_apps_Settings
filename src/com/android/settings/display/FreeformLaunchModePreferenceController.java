/*
 * Copyright (C) 2024 LibreMobileOS Foundation
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

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class FreeformLaunchModePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_FREEFORM_LAUNCH_MODE = "freeform_launch_mode";
    private static final int DEFAULT_VALUE = 1;

    public FreeformLaunchModePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int mode = Settings.System.getInt(
                    mContext.getContentResolver(), KEY_FREEFORM_LAUNCH_MODE, DEFAULT_VALUE);
            listPreference.setValue(String.valueOf(mode));
            listPreference.setSummary(getEntryForValue(listPreference, String.valueOf(mode)));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            int val = Integer.parseInt((String) newValue);
            Settings.System.putInt(mContext.getContentResolver(), KEY_FREEFORM_LAUNCH_MODE, val);
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                listPreference.setSummary(getEntryForValue(listPreference, (String) newValue));
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private CharSequence getEntryForValue(ListPreference pref, String val) {
        int index = pref.findIndexOfValue(val);
        if (index >= 0 && pref.getEntries() != null && index < pref.getEntries().length) {
            return pref.getEntries()[index];
        }
        return null;
    }
}
