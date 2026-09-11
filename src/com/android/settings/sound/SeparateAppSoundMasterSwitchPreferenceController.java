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

package com.android.settings.sound;

import android.content.Context;
import android.provider.Settings;
import android.widget.CompoundButton;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.MainSwitchPreference;

public class SeparateAppSoundMasterSwitchPreferenceController extends BasePreferenceController
        implements CompoundButton.OnCheckedChangeListener {

    private MainSwitchPreference mSwitchPreference;

    public SeparateAppSoundMasterSwitchPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSwitchPreference = screen.findPreference(getPreferenceKey());
        if (mSwitchPreference != null) {
            mSwitchPreference.addOnSwitchChangeListener(this);
            updateState(mSwitchPreference);
        }
    }

    @Override
    public void updateState(Preference preference) {
        boolean enabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SEPARATE_APP_SOUND_ENABLED, 0) == 1;
        if (mSwitchPreference != null) {
            mSwitchPreference.setChecked(enabled);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.SEPARATE_APP_SOUND_ENABLED, isChecked ? 1 : 0);
    }
}
