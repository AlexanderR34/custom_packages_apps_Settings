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

import com.android.settings.core.SliderPreferenceController;

/**
 * Controller for managing the Volume Boost slider preference in Sound & Vibration
 * and Accessibility Audio settings screens.
 */
public class VolumeBoostPreferenceController extends SliderPreferenceController {

    private static final String KEY_VOLUME_BOOST = "volume_boost";
    private final VolumeBoostManager mVolumeBoostManager;

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
    public int getSliderPosition() {
        return mVolumeBoostManager.getBoostLevel();
    }

    @Override
    public boolean setSliderPosition(int position) {
        mVolumeBoostManager.setBoostLevel(position);
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
        return com.android.settings.R.string.menu_key_sound;
    }
}
