/*
 * Copyright (C) 2024-2026 The Android Open Source Project
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
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SliderPreference;

/**
 * Controller to reset camera cutout settings to defaults (black color, auto width/height, 0 offset, solid style).
 */
public class CameraCutoutResetPreferenceController extends BasePreferenceController {

    public static final String KEY = "status_bar_camera_cutout_reset";
    private PreferenceScreen mScreen;

    public CameraCutoutResetPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            resetToDefaults();
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }

    private void resetToDefaults() {
        // 1. Reset Settings.System keys
        Settings.System.putString(mContext.getContentResolver(), CameraCutoutColorPreferenceController.KEY, CameraCutoutColorPreferenceController.DEFAULT_HEX);
        Settings.System.putInt(mContext.getContentResolver(), CameraCutoutWidthPreferenceController.KEY, 0);
        Settings.System.putInt(mContext.getContentResolver(), CameraCutoutHeightPreferenceController.KEY, 0);
        Settings.System.putInt(mContext.getContentResolver(), CameraCutoutOffsetXPreferenceController.KEY, 0);
        Settings.System.putInt(mContext.getContentResolver(), CameraCutoutOffsetYPreferenceController.KEY, 0);
        Settings.System.putIntForUser(mContext.getContentResolver(), CameraCutoutStylePreferenceController.KEY, CameraCutoutStylePreferenceController.DEFAULT_STYLE, UserHandle.myUserId());

        // 2. Update preferences in screen if open
        if (mScreen != null) {
            Preference colorPref = mScreen.findPreference(CameraCutoutColorPreferenceController.KEY);
            if (colorPref instanceof EditTextPreference) {
                ((EditTextPreference) colorPref).setText(CameraCutoutColorPreferenceController.DEFAULT_HEX);
                colorPref.setSummary(CameraCutoutColorPreferenceController.DEFAULT_HEX);
            }

            Preference widthPref = mScreen.findPreference(CameraCutoutWidthPreferenceController.KEY);
            if (widthPref instanceof SliderPreference) {
                ((SliderPreference) widthPref).setValue(0);
                widthPref.setSummary(mContext.getString(R.string.status_bar_camera_cutout_size_auto));
            }

            Preference heightPref = mScreen.findPreference(CameraCutoutHeightPreferenceController.KEY);
            if (heightPref instanceof SliderPreference) {
                ((SliderPreference) heightPref).setValue(0);
                heightPref.setSummary(mContext.getString(R.string.status_bar_camera_cutout_size_auto));
            }

            Preference offsetXPref = mScreen.findPreference(CameraCutoutOffsetXPreferenceController.KEY);
            if (offsetXPref instanceof SliderPreference) {
                ((SliderPreference) offsetXPref).setValue(0);
                offsetXPref.setSummary(mContext.getString(R.string.status_bar_camera_cutout_offset_center));
            }

            Preference offsetPref = mScreen.findPreference(CameraCutoutOffsetYPreferenceController.KEY);
            if (offsetPref instanceof SliderPreference) {
                ((SliderPreference) offsetPref).setValue(0);
                offsetPref.setSummary(mContext.getString(R.string.status_bar_camera_cutout_offset_center));
            }

            Preference stylePref = mScreen.findPreference(CameraCutoutStylePreferenceController.KEY);
            if (stylePref instanceof ListPreference) {
                ((ListPreference) stylePref).setValue("0");
                stylePref.setSummary(mContext.getString(R.string.status_bar_camera_cutout_style_solid));
            }
        }

        // 3. Show Toast confirmation
        Toast.makeText(mContext, R.string.status_bar_camera_cutout_reset_toast, Toast.LENGTH_SHORT).show();
    }
}
