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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Controller for managing USB Power Delivery charging optimization toggle in Battery Settings.
 */
public class UsbPdOptimizationPreferenceController extends TogglePreferenceController {

    private static final String KEY_USB_PD_OPTIMIZATION = "usb_pd_optimization";

    public UsbPdOptimizationPreferenceController(Context context, String key) {
        super(context, key);
    }

    public UsbPdOptimizationPreferenceController(Context context) {
        this(context, KEY_USB_PD_OPTIMIZATION);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.USB_PD_OPTIMIZATION_ENABLED, 1) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.System.putInt(
                mContext.getContentResolver(),
                Settings.System.USB_PD_OPTIMIZATION_ENABLED,
                isChecked ? 1 : 0);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_battery;
    }
}
