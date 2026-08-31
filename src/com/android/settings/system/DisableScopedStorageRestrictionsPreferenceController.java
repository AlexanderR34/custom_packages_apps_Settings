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
import android.os.SystemProperties;

import com.android.settings.core.TogglePreferenceController;

/**
 * Controller to bypass system folder restrictions (Android/data, Android/obb, Download) for third party apps.
 */
public class DisableScopedStorageRestrictionsPreferenceController extends TogglePreferenceController {

    private static final String PROP_DISABLE_SCOPED_STORAGE_RESTRICTIONS =
            "persist.sys.disable_scoped_storage_restrictions";

    public DisableScopedStorageRestrictionsPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return SystemProperties.getBoolean(PROP_DISABLE_SCOPED_STORAGE_RESTRICTIONS, false);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        try {
            SystemProperties.set(PROP_DISABLE_SCOPED_STORAGE_RESTRICTIONS, isChecked ? "true" : "false");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
