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

package com.android.settings.deviceinfo.storage;

import android.content.Context;
import android.os.SystemProperties;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/**
 * Controller for RAM Plus preference on the Storage dashboard.
 */
public class RamPlusPreferenceController extends BasePreferenceController {

    public static final String PROP_RAM_PLUS_ENABLED = "persist.sys.ram_plus_enabled";
    public static final String PROP_RAM_PLUS_SIZE = "persist.sys.ram_plus_size_gb";

    public RamPlusPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        boolean enabled = SystemProperties.getBoolean(PROP_RAM_PLUS_ENABLED, true);
        if (!enabled) {
            return mContext.getString(R.string.ram_plus_disabled);
        }
        int sizeGb = SystemProperties.getInt(PROP_RAM_PLUS_SIZE, 4);
        String sizeStr = mContext.getString(R.string.ram_plus_gb_format, sizeGb);
        return mContext.getString(R.string.ram_plus_enabled_format, sizeStr);
    }
}
