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

package com.android.settings.notification;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SliderPreferenceController;
import com.android.settingslib.widget.SliderPreference;

import java.util.Locale;

/**
 * Controller for managing the Volume Boost SliderPreference with native Material 3 design.
 */
public class VolumeBoostPreferenceController extends SliderPreferenceController {

    private static final String KEY_VOLUME_BOOST = "volume_boost";
    private final VolumeBoostManager mVolumeBoostManager;
    private SliderPreference mPreference;

    public VolumeBoostPreferenceController(Context context, String key) {
        super(context, key);
        mVolumeBoostManager = VolumeBoostManager.getInstance(context);
    }

    public VolumeBoostPreferenceController(Context context) {
        this(context, KEY_VOLUME_BOOST);
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
        if (preference instanceof SliderPreference) {
            preference.setSummary(getSummary());
        }
    }

    @Override
    public CharSequence getSummary() {
        int pos = getSliderPosition();
        if (pos <= 0) {
            return mContext.getString(R.string.volume_boost_summary) + " • 100%";
        }
        int displayVolumePercent = 100 + Math.round((pos / 100.0f) * 100.0f);
        float gainDb = (pos / 100.0f) * 8.0f;
        return mContext.getString(R.string.volume_boost_summary) + " • "
                + String.format(Locale.getDefault(), "%d%% (+%.1f dB)", displayVolumePercent, gainDb);
    }

    @Override
    public int getSliderPosition() {
        return mVolumeBoostManager.getBoostLevel();
    }

    @Override
    public boolean setSliderPosition(int position) {
        mVolumeBoostManager.setBoostLevel(position);
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
        return 0;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_sound;
    }
}

