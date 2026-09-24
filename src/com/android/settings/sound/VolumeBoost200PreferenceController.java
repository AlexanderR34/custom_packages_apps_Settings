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

package com.android.settings.sound;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

/**
 * Controller for 200% Volume Boost switch in Settings -> Sound.
 */
public class VolumeBoost200PreferenceController extends TogglePreferenceController {

    public static final String KEY_VOLUME_BOOST_200 = "volume_boost_200_enabled";

    public VolumeBoost200PreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getIntForUser(
                mContext.getContentResolver(),
                Settings.System.VOLUME_BOOST_200_ENABLED,
                0,
                UserHandle.USER_CURRENT) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (!isChecked) {
            // When turning off, immediately reset any active boost back to native 100% (0 dB)
            Settings.System.putIntForUser(
                    mContext.getContentResolver(),
                    Settings.System.VOLUME_BOOST_LEVEL,
                    0,
                    UserHandle.USER_CURRENT);
        }
        return Settings.System.putIntForUser(
                mContext.getContentResolver(),
                Settings.System.VOLUME_BOOST_200_ENABLED,
                isChecked ? 1 : 0,
                UserHandle.USER_CURRENT);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
