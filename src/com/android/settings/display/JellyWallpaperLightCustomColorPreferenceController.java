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

package com.android.settings.display;

import android.content.Context;
import android.graphics.Color;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class JellyWallpaperLightCustomColorPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public static final String KEY = "jelly_wallpaper_light_custom_color";
    public static final String SETTING_KEY = "jelly_wallpaper_light_custom_color";

    public JellyWallpaperLightCustomColorPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference instanceof EditTextPreference) {
            EditTextPreference editTextPreference = (EditTextPreference) preference;
            String color = Settings.System.getString(mContext.getContentResolver(), SETTING_KEY);
            if (TextUtils.isEmpty(color)) {
                editTextPreference.setSummary(mContext.getString(R.string.jelly_wallpaper_light_custom_color_summary));
                editTextPreference.setText("");
            } else {
                editTextPreference.setSummary(color);
                editTextPreference.setText(color);
            }
            boolean isEnabled = Settings.System.getInt(
                    mContext.getContentResolver(), "jelly_wallpaper_light_source_enabled", 0) != 0;
            editTextPreference.setEnabled(isEnabled);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String value = (String) newValue;
        if (TextUtils.isEmpty(value)) {
            Settings.System.putString(mContext.getContentResolver(), SETTING_KEY, "");
            preference.setSummary(mContext.getString(R.string.jelly_wallpaper_light_custom_color_summary));
            if (preference instanceof EditTextPreference) {
                ((EditTextPreference) preference).setText("");
            }
            return true;
        }
        String clean = value.trim();
        if (!clean.startsWith("#")) {
            clean = "#" + clean;
        }
        try {
            Color.parseColor(clean);
            Settings.System.putString(mContext.getContentResolver(), SETTING_KEY, clean);
            preference.setSummary(clean);
            if (preference instanceof EditTextPreference) {
                ((EditTextPreference) preference).setText(clean);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
