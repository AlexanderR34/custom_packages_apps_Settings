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

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

/**
 * Controller for selecting Always On Display (AOD) timeout mode (Always on vs 10s/5s after tap).
 */
public class AodTimeoutPreferenceController extends BasePreferenceController implements Preference.OnPreferenceChangeListener {

    private static final String KEY_AOD_TIMEOUT = "doze_always_on_timeout_mode";

    public AodTimeoutPreferenceController(Context context, String preferenceKey) {
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
            int mode = Settings.Secure.getInt(
                    mContext.getContentResolver(), KEY_AOD_TIMEOUT, 0);
            listPref.setValue(String.valueOf(mode));
            listPref.setSummary(listPref.getEntry());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int mode = Integer.parseInt((String) newValue);
        Settings.Secure.putInt(
                mContext.getContentResolver(), KEY_AOD_TIMEOUT, mode);
        updateState(preference);
        return true;
    }
}
