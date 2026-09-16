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

package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Controller for the primary switch in display settings, linking to the detailed JellyWallpaperGyroSettings sub-screen.
 */
public class JellyWallpaperGyroPreferenceController extends TogglePreferenceController {

    public static final String KEY_GYRO_WALLPAPER = "jelly_wallpaper_gyro_parallax";
    public static final String KEY_GYRO_MODE = "jelly_wallpaper_gyro_mode";

    public JellyWallpaperGyroPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(
                mContext.getContentResolver(), KEY_GYRO_WALLPAPER, 0) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putInt(
                mContext.getContentResolver(), KEY_GYRO_WALLPAPER, isChecked ? 1 : 0);
    }

    @Override
    public CharSequence getSummary() {
        if (!isChecked()) {
            return mContext.getString(R.string.switch_off_text);
        }
        int mode = Settings.System.getInt(
                mContext.getContentResolver(), KEY_GYRO_MODE, 2);
        switch (mode) {
            case 0:
                return mContext.getString(R.string.jelly_wallpaper_mode_lockscreen);
            case 1:
                return mContext.getString(R.string.jelly_wallpaper_mode_homescreen);
            case 2:
            default:
                return mContext.getString(R.string.jelly_wallpaper_mode_both);
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
