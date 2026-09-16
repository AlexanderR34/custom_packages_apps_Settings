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

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.SliderPreferenceController;
import com.android.settingslib.widget.SliderPreference;

public class JellyWallpaperGyroInertiaStrengthPreferenceController extends SliderPreferenceController {

    public static final String KEY = "jelly_wallpaper_gyro_inertia_strength";
    public static final String SETTING_KEY = "jelly_wallpaper_gyro_inertia_strength";
    public static final int DEFAULT_VALUE = 65;

    private SliderPreference mPreference;

    public JellyWallpaperGyroInertiaStrengthPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreference.setUpdatesContinuously(true);
            mPreference.setMin(getMin());
            mPreference.setMax(getMax());
            mPreference.setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_TICKS);
            mPreference.setSummary(getSummary());
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        boolean isJellyEnabled = Settings.System.getInt(
                mContext.getContentResolver(), "jelly_wallpaper_enabled", 0) != 0;
        boolean isInertiaEnabled = Settings.System.getInt(
                mContext.getContentResolver(), "jelly_wallpaper_gyro_inertia", 1) != 0;
        preference.setEnabled(isJellyEnabled && isInertiaEnabled);
        if (preference instanceof SliderPreference) {
            preference.setSummary(getSummary());
        }
    }

    @Override
    public CharSequence getSummary() {
        return getSliderPosition() + "%";
    }

    @Override
    public int getSliderPosition() {
        return Settings.System.getInt(mContext.getContentResolver(), SETTING_KEY, DEFAULT_VALUE);
    }

    @Override
    public boolean setSliderPosition(int position) {
        Settings.System.putInt(mContext.getContentResolver(), SETTING_KEY, position);
        if (mPreference != null) {
            mPreference.setSummary(position + "%");
        }
        return true;
    }

    @Override
    public int getMax() {
        return 100;
    }

    @Override
    public int getMin() {
        return 0;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
