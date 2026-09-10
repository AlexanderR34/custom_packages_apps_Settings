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
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

/**
 * Controller for selecting Always On Display (AOD) timeout mode (Never / 5s / 15s / 30s / 60s / 120s).
 */
public class AodTimeoutPreferenceController extends BasePreferenceController implements Preference.OnPreferenceChangeListener {

    private static final String KEY_AOD_TIMEOUT = Settings.Secure.DOZE_ALWAYS_ON_TIMEOUT;
    private final AmbientDisplayConfiguration mConfig;

    public AodTimeoutPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mConfig = new AmbientDisplayConfiguration(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return mConfig.alwaysOnAvailableForUser(UserHandle.myUserId())
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        if (preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            int timeoutSec = Settings.Secure.getIntForUser(
                    mContext.getContentResolver(),
                    KEY_AOD_TIMEOUT,
                    0,
                    UserHandle.myUserId());
            listPref.setValue(String.valueOf(timeoutSec));
            listPref.setSummary(listPref.getEntry());

            boolean aodEnabled = mConfig.alwaysOnEnabled(UserHandle.myUserId());
            listPref.setEnabled(aodEnabled);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            int timeoutSec = Integer.parseInt((String) newValue);
            Settings.Secure.putIntForUser(
                    mContext.getContentResolver(),
                    KEY_AOD_TIMEOUT,
                    timeoutSec,
                    UserHandle.myUserId());
            if (preference instanceof ListPreference) {
                ListPreference listPref = (ListPreference) preference;
                int index = listPref.findIndexOfValue((String) newValue);
                if (index >= 0 && index < listPref.getEntries().length) {
                    listPref.setSummary(listPref.getEntries()[index]);
                }
            }
        } catch (Exception ignored) {
        }
        return true;
    }
}

