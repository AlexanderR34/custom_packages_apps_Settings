/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.gestures;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class ShakeGesturePreferenceController extends BasePreferenceController {

    public ShakeGesturePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final boolean isEnabled = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SHAKE_GESTURE_ENABLED, 0) == 1;
        if (!isEnabled) {
            return mContext.getText(R.string.gesture_setting_off);
        }

        final String action = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.SHAKE_GESTURE_ACTION);
        final String actionKey = (action != null && !action.isEmpty()) ? action : "torch";

        switch (actionKey) {
            case "screenshot":
                return mContext.getText(R.string.shake_action_screenshot);
            case "assistant":
                return mContext.getText(R.string.shake_action_assistant);
            case "media_play_pause":
                return mContext.getText(R.string.shake_action_media);
            case "recents":
                return mContext.getText(R.string.shake_action_recents);
            case "notifications":
                return mContext.getText(R.string.shake_action_notifications);
            case "app":
                String appPkg = Settings.System.getString(mContext.getContentResolver(),
                        Settings.System.SHAKE_GESTURE_APP);
                if (appPkg != null && !appPkg.isEmpty()) {
                    try {
                        PackageManager pm = mContext.getPackageManager();
                        ApplicationInfo info = pm.getApplicationInfo(appPkg, 0);
                        return pm.getApplicationLabel(info);
                    } catch (Exception ignored) {}
                }
                return mContext.getText(R.string.shake_action_app);
            case "torch":
            default:
                return mContext.getText(R.string.shake_action_torch);
        }
    }
}
