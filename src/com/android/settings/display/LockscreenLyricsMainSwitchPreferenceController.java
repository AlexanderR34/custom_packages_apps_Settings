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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.MainSwitchPreference;

public class LockscreenLyricsMainSwitchPreferenceController extends BasePreferenceController
        implements OnCheckedChangeListener {

    public static final String KEY = "lockscreen_lyrics_main_switch";
    public static final String SETTING_KEY = "lockscreen_lyrics_enabled";

    private MainSwitchPreference mPreference;

    public LockscreenLyricsMainSwitchPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference pref = screen.findPreference(getPreferenceKey());
        if (pref instanceof MainSwitchPreference) {
            mPreference = (MainSwitchPreference) pref;
            mPreference.addOnSwitchChangeListener(this);
            updateState(mPreference);
        }
    }

    @Override
    public void updateState(Preference preference) {
        boolean isEnabled = Settings.System.getInt(
                mContext.getContentResolver(), SETTING_KEY, 1) != 0;
        if (mPreference != null) {
            mPreference.setChecked(isEnabled);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Settings.System.putInt(mContext.getContentResolver(), SETTING_KEY, isChecked ? 1 : 0);
    }
}
