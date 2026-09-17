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

package com.android.settings.sound;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class Sm64CoinsResetPreferenceController extends BasePreferenceController {

    public static final String KEY = "sm64_coins_reset";

    public Sm64CoinsResetPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            Settings.System.putInt(mContext.getContentResolver(), "sm64_red_coins_sound_mode", 0);
            Settings.System.putInt(mContext.getContentResolver(), "sm64_red_coins_burst_timeout", 5);
            for (int i = 1; i <= 8; i++) {
                Settings.System.putInt(mContext.getContentResolver(), "sm64_coin_" + i + "_enabled", 1);
                Settings.System.putString(mContext.getContentResolver(), "sm64_coin_" + i + "_uri", "");
            }
            Toast.makeText(mContext, R.string.sm64_coins_reset_toast, Toast.LENGTH_SHORT).show();

            PreferenceScreen screen = preference.getPreferenceManager().getPreferenceScreen();
            if (screen != null) {
                Preference modePref = screen.findPreference("sm64_red_coins_sound_mode");
                if (modePref instanceof ListPreference) {
                    ListPreference lp = (ListPreference) modePref;
                    lp.setValue("0");
                    lp.setSummary(lp.getEntry());
                }
                Preference timeoutPref = screen.findPreference("sm64_red_coins_burst_timeout");
                if (timeoutPref instanceof ListPreference) {
                    ListPreference lp = (ListPreference) timeoutPref;
                    lp.setValue("5");
                    lp.setSummary(lp.getEntry());
                }
                for (int i = 1; i <= 8; i++) {
                    Preference coinPref = screen.findPreference("sm64_coin_" + i);
                    if (coinPref instanceof Sm64CoinPreference) {
                        Sm64CoinPreference cp = (Sm64CoinPreference) coinPref;
                        cp.setChecked(true);
                        cp.updateSummary();
                    } else if (coinPref instanceof TwoStatePreference) {
                        ((TwoStatePreference) coinPref).setChecked(true);
                    }
                }
            }
            return true;
        }
        return false;
    }
}
