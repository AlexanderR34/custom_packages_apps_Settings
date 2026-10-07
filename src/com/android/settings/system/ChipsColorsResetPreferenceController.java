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
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class ChipsColorsResetPreferenceController extends BasePreferenceController {

    public static final String KEY = "status_bar_chips_colors_reset";
    private PreferenceScreen mScreen;

    public ChipsColorsResetPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            resetToDefaults();
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }

    private void resetToDefaults() {
        // 1. Reset Settings.System keys to defaults
        Settings.System.putString(mContext.getContentResolver(), ChipsColorCamMicPreferenceController.KEY, ChipsColorCamMicPreferenceController.DEFAULT_HEX);
        Settings.System.putString(mContext.getContentResolver(), ChipsColorLocationPreferenceController.KEY, ChipsColorLocationPreferenceController.DEFAULT_HEX);
        Settings.System.putString(mContext.getContentResolver(), ChipsColorComboPreferenceController.KEY, ChipsColorComboPreferenceController.DEFAULT_HEX);
        Settings.System.putString(mContext.getContentResolver(), ChipsColorAllPreferenceController.KEY, ChipsColorAllPreferenceController.DEFAULT_HEX);

        // 2. Update UI preferences in current screen
        if (mScreen != null) {
            updatePref(ChipsColorCamMicPreferenceController.KEY, ChipsColorCamMicPreferenceController.DEFAULT_HEX);
            updatePref(ChipsColorLocationPreferenceController.KEY, ChipsColorLocationPreferenceController.DEFAULT_HEX);
            updatePref(ChipsColorComboPreferenceController.KEY, ChipsColorComboPreferenceController.DEFAULT_HEX);
            updatePref(ChipsColorAllPreferenceController.KEY, ChipsColorAllPreferenceController.DEFAULT_HEX);
        }

        // 3. Show confirmation toast
        Toast.makeText(mContext, R.string.status_bar_chips_colors_reset_toast, Toast.LENGTH_SHORT).show();
    }

    private void updatePref(String key, String defaultHex) {
        Preference pref = mScreen.findPreference(key);
        if (pref instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) pref;
            editTextPref.setText(defaultHex);
            editTextPref.setSummary(defaultHex);
        }
    }
}
