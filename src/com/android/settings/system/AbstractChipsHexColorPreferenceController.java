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
import android.graphics.Color;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

public abstract class AbstractChipsHexColorPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public AbstractChipsHexColorPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    protected abstract String getSettingKey();
    protected abstract String getDefaultHex();

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference instanceof EditTextPreference) {
            EditTextPreference editTextPreference = (EditTextPreference) preference;
            String color = Settings.System.getString(mContext.getContentResolver(), getSettingKey());
            if (TextUtils.isEmpty(color)) {
                editTextPreference.setSummary(getDefaultHex());
                editTextPreference.setText(getDefaultHex());
            } else {
                editTextPreference.setSummary(color);
                editTextPreference.setText(color);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String value = (String) newValue;
        if (TextUtils.isEmpty(value)) {
            Settings.System.putString(mContext.getContentResolver(), getSettingKey(), getDefaultHex());
            preference.setSummary(getDefaultHex());
            if (preference instanceof EditTextPreference) {
                ((EditTextPreference) preference).setText(getDefaultHex());
            }
            return true;
        }
        String clean = value.trim();
        if (!clean.startsWith("#")) {
            clean = "#" + clean;
        }
        try {
            Color.parseColor(clean);
            String formattedHex = clean.toUpperCase();
            Settings.System.putString(mContext.getContentResolver(), getSettingKey(), formattedHex);
            preference.setSummary(formattedHex);
            if (preference instanceof EditTextPreference) {
                ((EditTextPreference) preference).setText(formattedHex);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
