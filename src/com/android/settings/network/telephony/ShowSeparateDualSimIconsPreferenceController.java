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

package com.android.settings.network.telephony;

import android.content.Context;
import android.provider.Settings;

/**
 * Preference controller for toggling separate status bar signal icons in dual SIM mode
 */
public class ShowSeparateDualSimIconsPreferenceController extends TelephonyTogglePreferenceController {

    public ShowSeparateDualSimIconsPreferenceController(Context context, String key) {
        super(context, key);
    }

    public ShowSeparateDualSimIconsPreferenceController init(int subId) {
        mSubId = subId;
        return this;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.SHOW_SEPARATE_DUAL_SIM_ICONS,
                0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putInt(
                mContext.getContentResolver(),
                Settings.System.SHOW_SEPARATE_DUAL_SIM_ICONS,
                isChecked ? 1 : 0);
    }
}
