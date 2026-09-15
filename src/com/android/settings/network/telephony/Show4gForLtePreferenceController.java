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
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;

/**
 * Preference controller for toggling 4G / LTE status bar icon
 */
public class Show4gForLtePreferenceController extends TelephonyTogglePreferenceController {

    public Show4gForLtePreferenceController(Context context, String key) {
        super(context, key);
    }

    public Show4gForLtePreferenceController init(int subId) {
        mSubId = subId;
        return this;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        boolean defaultVal = false;
        PersistableBundle b = getCarrierConfigForSubId(mSubId);
        if (b != null) {
            defaultVal = b.getBoolean(
                    CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL, false);
        }
        return Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.SHOW_FOURG_ICON,
                defaultVal ? 1 : 0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putInt(
                mContext.getContentResolver(),
                Settings.System.SHOW_FOURG_ICON,
                isChecked ? 1 : 0);
    }
}
