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

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SliderPreferenceController;
import com.android.settingslib.widget.SliderPreference;

/**
 * Controller to configure vertical offset (shift up/down) of the camera cutout.
 * Range: -30 dp to +30 dp.
 */
public class CameraCutoutOffsetYPreferenceController extends SliderPreferenceController {

    public static final String KEY = "status_bar_camera_cutout_offset_y";
    public static final int MIN_DP = -30;
    public static final int MAX_DP = 30;

    public CameraCutoutOffsetYPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        SliderPreference preference = screen.findPreference(getPreferenceKey());
        if (preference != null) {
            preference.setUpdatesContinuously(true);
            preference.setMin(getMin());
            preference.setMax(getMax());
            preference.setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_TICKS);
            updateSummary(preference, getSliderPosition());
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference instanceof SliderPreference) {
            updateSummary((SliderPreference) preference, getSliderPosition());
        }
    }

    private void updateSummary(SliderPreference preference, int position) {
        if (position == 0) {
            preference.setSummary(mContext.getString(R.string.status_bar_camera_cutout_offset_center));
        } else if (position > 0) {
            preference.setSummary("+" + position + " dp");
        } else {
            preference.setSummary(position + " dp");
        }
    }

    @Override
    public int getSliderPosition() {
        return Settings.System.getInt(
                mContext.getContentResolver(), KEY, 0);
    }

    @Override
    public boolean setSliderPosition(int position) {
        Settings.System.putInt(
                mContext.getContentResolver(), KEY, position);
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = super.onPreferenceChange(preference, newValue);
        if (result && preference instanceof SliderPreference) {
            updateSummary((SliderPreference) preference, (int) newValue);
        }
        return result;
    }

    @Override
    public int getMax() {
        return MAX_DP;
    }

    @Override
    public int getMin() {
        return MIN_DP;
    }
}
