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

public class JellyWallpaperWaterPresetPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public static final String KEY = "jelly_wallpaper_water_preset";
    public static final String SETTING_KEY = "jelly_wallpaper_water_preset";
    public static final int DEFAULT_PRESET = 1; // Tropical Ocean by default

    public JellyWallpaperWaterPresetPreferenceController(Context context, String preferenceKey) {
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
            int preset = Settings.System.getInt(mContext.getContentResolver(), SETTING_KEY, DEFAULT_PRESET);
            listPref.setValue(String.valueOf(preset));
            listPref.setSummary(listPref.getEntry());

            boolean isLightEnabled = Settings.System.getInt(
                    mContext.getContentResolver(), "jelly_wallpaper_light_source_enabled", 0) != 0;
            boolean isWavesEnabled = Settings.System.getInt(
                    mContext.getContentResolver(), "jelly_wallpaper_light_water_waves", 1) != 0;
            listPref.setEnabled(isLightEnabled && isWavesEnabled);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int val = Integer.parseInt((String) newValue);
        Settings.System.putInt(mContext.getContentResolver(), SETTING_KEY, val);

        if (val == 0) { // Calm Lake
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_light_water_waves_intensity", 35);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_wave_size", 75);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_wave_speed", 25);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_glitter_density", 30);
        } else if (val == 1) { // Tropical Ocean
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_light_water_waves_intensity", 75);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_wave_size", 50);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_wave_speed", 60);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_glitter_density", 75);
        } else if (val == 2) { // Deep Ocean Swell
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_light_water_waves_intensity", 95);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_wave_size", 65);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_wave_speed", 45);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_glitter_density", 60);
        } else if (val == 3) { // Crystal Pool Caustics
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_light_water_waves_intensity", 60);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_wave_size", 40);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_wave_speed", 50);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_glitter_density", 70);
        } else if (val == 4) { // Golden Sunset Shoreline & Foam
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_light_water_waves_intensity", 70);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_wave_size", 55);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_wave_speed", 40);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_glitter_density", 90);
        } else if (val == 5) { // Underwater God Rays
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_light_water_waves_intensity", 80);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_wave_size", 45);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_wave_speed", 35);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_glitter_density", 50);
        } else if (val == 6) { // Diamond Glitter & Bokeh
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_light_water_waves_intensity", 65);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_wave_size", 35);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_wave_speed", 65);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_glitter_density", 100);
        } else if (val == 7) { // Splash Droplets & Spray
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_light_water_waves_intensity", 85);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_wave_size", 50);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_wave_speed", 75);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_water_glitter_density", 85);
        }

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
