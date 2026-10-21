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

package com.android.settings.notification;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Controller that enables or disables showing a notification when a screenshot is taken.
 */
public class ScreenshotNotificationPreferenceController extends TogglePreferenceController {

    private static final String KEY = "screenshot_notification_enabled";
    private static final int ON = 1;
    private static final int OFF = 0;

    public ScreenshotNotificationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.SCREENSHOT_NOTIFICATION_ENABLED, OFF, UserHandle.USER_CURRENT) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.SCREENSHOT_NOTIFICATION_ENABLED, isChecked ? ON : OFF, UserHandle.USER_CURRENT);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_notifications;
    }
}
