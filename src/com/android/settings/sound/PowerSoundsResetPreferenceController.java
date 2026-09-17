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
import android.text.TextUtils;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class PowerSoundsResetPreferenceController extends BasePreferenceController {

    public static final String KEY = "power_sounds_reset";

    public PowerSoundsResetPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            Settings.System.putInt(mContext.getContentResolver(), "system_boot_sound_enabled", 0);
            Settings.System.putString(mContext.getContentResolver(), "system_boot_sound_uri", "");
            Settings.System.putInt(mContext.getContentResolver(), "system_shutdown_sound_enabled", 0);
            Settings.System.putString(mContext.getContentResolver(), "system_shutdown_sound_uri", "");
            Toast.makeText(mContext, R.string.power_sounds_reset_toast, Toast.LENGTH_SHORT).show();

            PreferenceScreen screen = preference.getPreferenceManager().getPreferenceScreen();
            if (screen != null) {
                Preference bootSwitch = screen.findPreference("system_boot_sound_enabled");
                if (bootSwitch instanceof TwoStatePreference) {
                    ((TwoStatePreference) bootSwitch).setChecked(false);
                }
                Preference bootPicker = screen.findPreference("system_boot_sound");
                if (bootPicker instanceof SystemCustomSoundPreference) {
                    ((SystemCustomSoundPreference) bootPicker).updateSummary();
                }
                Preference shutSwitch = screen.findPreference("system_shutdown_sound_enabled");
                if (shutSwitch instanceof TwoStatePreference) {
                    ((TwoStatePreference) shutSwitch).setChecked(false);
                }
                Preference shutPicker = screen.findPreference("system_shutdown_sound");
                if (shutPicker instanceof SystemCustomSoundPreference) {
                    ((SystemCustomSoundPreference) shutPicker).updateSummary();
                }
            }
            return true;
        }
        return false;
    }
}
