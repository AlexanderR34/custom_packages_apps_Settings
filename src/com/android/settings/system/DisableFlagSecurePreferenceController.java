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

package com.android.settings.system;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

/**
 * Controller to ignore FLAG_SECURE and allow screenshots in protected apps (banking, DRM, incognito, etc.).
 */
public class DisableFlagSecurePreferenceController extends TogglePreferenceController {

    private static final String KEY_DISABLE_FLAG_SECURE = "disable_flag_secure";

    public DisableFlagSecurePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(
                mContext.getContentResolver(), KEY_DISABLE_FLAG_SECURE, 0) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        boolean success = Settings.Secure.putInt(
                mContext.getContentResolver(), KEY_DISABLE_FLAG_SECURE, isChecked ? 1 : 0);
        if (success) {
            RebootUtils.showRebootPromptDialog(mContext);
        }
        return success;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
