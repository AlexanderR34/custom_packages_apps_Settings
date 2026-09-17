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

import androidx.preference.Preference;

import com.android.settings.core.TogglePreferenceController;

public class Sm64CoinTrackPreferenceController extends TogglePreferenceController {

    public Sm64CoinTrackPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(), getPreferenceKey(), 1) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putInt(mContext.getContentResolver(), getPreferenceKey(), isChecked ? 1 : 0);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        boolean isMasterEnabled = Settings.System.getInt(
                mContext.getContentResolver(), "sm64_red_coins_sound_enabled", 0) == 1;
        preference.setEnabled(isMasterEnabled);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
