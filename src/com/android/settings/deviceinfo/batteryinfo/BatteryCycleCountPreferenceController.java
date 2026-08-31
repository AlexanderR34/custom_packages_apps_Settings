/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.deviceinfo.batteryinfo;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.fuelgauge.BatteryUtils;

/**
 * A controller that manages the information about battery cycle count.
 */
public class BatteryCycleCountPreferenceController extends BasePreferenceController {

    public BatteryCycleCountPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        Intent batteryIntent = BatteryUtils.getBatteryIntent(mContext);
        int cycleCount = batteryIntent != null ? batteryIntent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) : -1;

        if (cycleCount <= 0) {
            try {
                java.io.File file = new java.io.File("/sys/class/power_supply/battery/cycle_count");
                if (file.exists()) {
                    String content = new String(java.nio.file.Files.readAllBytes(file.toPath())).trim();
                    cycleCount = Integer.parseInt(content);
                }
            } catch (Exception e) {
                // Ignore sysfs read errors
            }
        }

        return cycleCount <= 0
                ? mContext.getText(R.string.battery_cycle_count_not_available)
                : Integer.toString(cycleCount);
    }
}
