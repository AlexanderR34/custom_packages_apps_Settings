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

package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class LockscreenMultiWallpaperPreferenceController extends BasePreferenceController {

    public LockscreenMultiWallpaperPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        boolean enabled = Settings.System.getInt(
                mContext.getContentResolver(), Settings.System.LOCKSCREEN_MULTI_WALLPAPER_ENABLED, 0) == 1;

        if (!enabled) {
            return mContext.getString(R.string.switch_off_text);
        }

        String paths = Settings.System.getString(
                mContext.getContentResolver(), Settings.System.LOCKSCREEN_MULTI_WALLPAPER_FILES);
        int photoCount = 0;
        if (!TextUtils.isEmpty(paths)) {
            photoCount = paths.split(";").length;
        }

        if (photoCount > 0) {
            return mContext.getString(R.string.lockscreen_multi_wallpaper_summary_enabled, photoCount);
        }
        return mContext.getString(R.string.switch_on_text);
    }
}
