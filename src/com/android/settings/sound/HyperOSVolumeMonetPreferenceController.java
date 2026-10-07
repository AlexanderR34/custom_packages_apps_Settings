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
 * Controller for HyperOS Volume Panel Monet colors toggle in Settings -> Sound.
 */
public class HyperOSVolumeMonetPreferenceController extends TogglePreferenceController {

    private static final String KEY_HYPEROS_VOLUME_MONET = "hyperos_volume_use_monet";

    public HyperOSVolumeMonetPreferenceController(Context context, String key) {
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
                Settings.System.HYPEROS_VOLUME_USE_MONET,
                1,
                UserHandle.USER_CURRENT) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putIntForUser(
                mContext.getContentResolver(),
                Settings.System.HYPEROS_VOLUME_USE_MONET,
                isChecked ? 1 : 0,
                UserHandle.USER_CURRENT);
    }

    @Override
    public int getSliceType() {
        return SliceType.SWITCH;
    }
}
