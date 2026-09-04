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

import androidx.preference.PreferenceScreen;

import com.android.settings.core.SliderPreferenceController;
import com.android.settingslib.widget.SliderPreference;

/**
 * Controller for the Custom Background Blur Intensity Slider in Settings -> Display.
 */
public class CustomBlurPreferenceController extends SliderPreferenceController {

    private static final String KEY_CUSTOM_BLUR = "custom_blur_intensity";
    private static final int DEFAULT_BLUR = 50;

    public CustomBlurPreferenceController(Context context, String key) {
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
        }
    }

    @Override
    public int getSliderPosition() {
        return Settings.System.getInt(
                mContext.getContentResolver(), Settings.System.CUSTOM_BLUR_INTENSITY, DEFAULT_BLUR);
    }

    @Override
    public boolean setSliderPosition(int position) {
        Settings.System.putInt(
                mContext.getContentResolver(), Settings.System.CUSTOM_BLUR_INTENSITY, position);
        
        // Dynamically toggle window blurs disable state if set to 0
        if (position == 0) {
            Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.DISABLE_WINDOW_BLURS, 1);
        } else {
            Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.DISABLE_WINDOW_BLURS, 0);
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