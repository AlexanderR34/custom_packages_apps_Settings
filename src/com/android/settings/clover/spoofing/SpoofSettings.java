/*
 * SPDX-FileCopyrightText: 2024-2026 The Clover Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.clover.spoofing;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settings.system.RebootUtils;

public class SpoofSettings extends SettingsPreferenceFragment {

    private static final String TAG = "SpoofSettings";
    private static final String KEY_SPOOF_STREAMING = "spoof_streaming";

    private static final String[] STREAMING_PACKAGES = {
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus"
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.spoofing_settings);

        SwitchPreferenceCompat spoofStreamingPref = findPreference(KEY_SPOOF_STREAMING);
        if (spoofStreamingPref != null) {
            boolean isEnabled = Settings.Secure.getInt(
                    requireContext().getContentResolver(), KEY_SPOOF_STREAMING, 1) == 1;
            spoofStreamingPref.setChecked(isEnabled);
            spoofStreamingPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                Settings.Secure.putInt(
                        requireContext().getContentResolver(), KEY_SPOOF_STREAMING, enabled ? 1 : 0);
                killStreamingApps();
                if (enabled) {
                    RebootUtils.showRebootPromptDialog(requireContext());
                }
                return true;
            });
        }
    }

    private void killStreamingApps() {
        try {
            ActivityManager am = (ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                for (String pkg : STREAMING_PACKAGES) {
                    try {
                        am.forceStopPackage(pkg);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop streaming apps", e);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DASHBOARD_SUMMARY;
    }
}