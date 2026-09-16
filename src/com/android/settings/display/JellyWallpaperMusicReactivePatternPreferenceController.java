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
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

public class JellyWallpaperMusicReactivePatternPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public static final String KEY = "jelly_wallpaper_music_reactive_pattern";
    public static final String SETTING_KEY = "jelly_wallpaper_music_reactive_pattern";
    public static final int DEFAULT_PATTERN = 0; // 0: Radial Ripple, 1: Subwoofer, 2: Heartbeat, 3: Cascade, 4: 4 Corners

    public JellyWallpaperMusicReactivePatternPreferenceController(Context context, String preferenceKey) {
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
            int pattern = Settings.System.getInt(mContext.getContentResolver(), SETTING_KEY, DEFAULT_PATTERN);
            listPref.setValue(String.valueOf(pattern));
            listPref.setSummary(listPref.getEntry());

            boolean isJellyEnabled = Settings.System.getInt(
                    mContext.getContentResolver(), "jelly_wallpaper_enabled", 0) != 0;
            boolean isMusicEnabled = Settings.System.getInt(
                    mContext.getContentResolver(), "jelly_wallpaper_music_reactive", 0) != 0;
            listPref.setEnabled(isJellyEnabled && isMusicEnabled);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int val = Integer.parseInt((String) newValue);
        Settings.System.putInt(mContext.getContentResolver(), SETTING_KEY, val);

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
