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

package com.android.settings.network.telephony;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

/**
 * Controller for selecting VoLTE / VoWiFi status bar icon visibility mode:
 * 0: Disabled
 * 1: VoLTE only
 * 2: VoWiFi only
 * 3: Both (VoLTE and VoWiFi)
 */
public class StatusBarImsIconPreferenceController extends TelephonyBasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public StatusBarImsIconPreferenceController(Context context, String key) {
        super(context, key);
    }

    public StatusBarImsIconPreferenceController init(int subId) {
        mSubId = subId;
        return this;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        if (preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            int mode = Settings.System.getInt(
                    mContext.getContentResolver(),
                    Settings.System.STATUS_BAR_IMS_ICON_MODE,
                    3); // Default to 3 (Both) or 0
            listPref.setValue(String.valueOf(mode));
            listPref.setSummary(listPref.getEntry());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            int mode = Integer.parseInt((String) newValue);
            Settings.System.putInt(
                    mContext.getContentResolver(),
                    Settings.System.STATUS_BAR_IMS_ICON_MODE,
                    mode);
            // Also keep individual keys synchronized for compatibility
            Settings.System.putInt(
                    mContext.getContentResolver(),
                    Settings.System.SHOW_VOLTE_ICON,
                    (mode == 1 || mode == 3) ? 1 : 0);
            Settings.System.putInt(
                    mContext.getContentResolver(),
                    Settings.System.SHOW_VOWIFI_ICON,
                    (mode == 2 || mode == 3) ? 1 : 0);

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
