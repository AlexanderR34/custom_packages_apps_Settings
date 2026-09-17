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

public class JellyWallpaperStiffnessPreferenceController extends SliderPreferenceController {

    public static final String KEY = "jelly_wallpaper_stiffness";
    public static final String SETTING_KEY = "jelly_wallpaper_stiffness";
    public static final int DEFAULT_VALUE = 45;

    private SliderPreference mPreference;

    public JellyWallpaperStiffnessPreferenceController(Context context, String key) {
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
        boolean isEnabled = Settings.System.getInt(
                mContext.getContentResolver(), "jelly_wallpaper_enabled", 0) != 0;
        preference.setEnabled(isEnabled);
        if (preference instanceof SliderPreference) {
            preference.setSummary(getSummary());
        }
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getString(com.android.settings.R.string.jelly_wallpaper_stiffness_summary)
                + " • " + getSliderPosition() + "%";
    }

    @Override
    public int getSliderPosition() {
        return Settings.System.getInt(mContext.getContentResolver(), SETTING_KEY, DEFAULT_VALUE);
    }

    @Override
    public boolean setSliderPosition(int position) {
        Settings.System.putInt(mContext.getContentResolver(), SETTING_KEY, position);
        // Cuando el usuario mueve manualmente el slider, marcar como Personalizado (8)
        Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_preset", 8);
        if (mPreference != null) {
            mPreference.setSummary(getSummary());
        }
        return true;
    }

    @Override
    public int getMax() {
        return 100;
    }

    @Override
    public int getMin() {
        return 10;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
