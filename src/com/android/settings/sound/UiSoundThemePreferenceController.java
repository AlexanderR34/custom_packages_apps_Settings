/*
 * Copyright (C) 2026 The Project Diva
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
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

public class UiSoundThemePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public static final String KEY = "ui_sounds_theme";
    public static final String SETTING_KEY = "ui_sounds_theme";

    public UiSoundThemePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            int currentValue = Settings.System.getInt(mContext.getContentResolver(), SETTING_KEY, 0);
            listPref.setValue(String.valueOf(currentValue));
            listPref.setSummary(listPref.getEntry());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof ListPreference) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(mContext.getContentResolver(), SETTING_KEY, value);
            ListPreference listPref = (ListPreference) preference;
            int index = listPref.findIndexOfValue((String) newValue);
            if (index >= 0) {
                listPref.setSummary(listPref.getEntries()[index]);
            }
            applySoundThemePaths(value);
            return true;
        }
        return false;
    }

    private void applySoundThemePaths(int theme) {
        String chargingSound = null;
        String disconnectSound = null;
        String wirelessChargingSound = null;
        String lockSound = null;
        String unlockSound = null;
        String lowBatterySound = null;
        String screenshotSound = null;

        if (theme == 1) { // POCO / HyperOS
            chargingSound = resolvePath("/system/media/audio/ui/poco/charging.ogg", "/product/media/audio/ui/ChargingStarted.ogg", "/system/media/audio/ui/ChargingStarted.ogg");
            disconnectSound = resolvePath("/system/media/audio/ui/poco/disconnect.ogg");
            wirelessChargingSound = resolvePath("/system/media/audio/ui/poco/charge_wireless.ogg", "/system/media/audio/ui/poco/charging.ogg", "/product/media/audio/ui/WirelessChargingStarted.ogg");
            lockSound = resolvePath("/system/media/audio/ui/poco/Lock.ogg", "/product/media/audio/ui/Lock.ogg", "/system/media/audio/ui/Lock.ogg");
            unlockSound = resolvePath("/system/media/audio/ui/poco/Unlock.ogg", "/product/media/audio/ui/Unlock.ogg", "/system/media/audio/ui/Unlock.ogg");
            lowBatterySound = resolvePath("/system/media/audio/ui/poco/LowBattery.ogg", "/product/media/audio/ui/LowBattery.ogg", "/system/media/audio/ui/LowBattery.ogg");
            screenshotSound = resolvePath("/system/media/audio/ui/poco/screenshot.ogg", "/system/media/audio/ui/poco/camera_click.ogg", "/product/media/audio/ui/camera_click.ogg");
        } else if (theme == 2) { // Samsung
            chargingSound = resolvePath("/system/media/audio/ui/samsung/ChargingStarted.ogg", "/product/media/audio/ui/ChargingStarted.ogg", "/system/media/audio/ui/ChargingStarted.ogg");
            disconnectSound = resolvePath("/system/media/audio/ui/samsung/disconnect.ogg");
            wirelessChargingSound = resolvePath("/system/media/audio/ui/samsung/ChargingStarted.ogg", "/product/media/audio/ui/WirelessChargingStarted.ogg", "/system/media/audio/ui/WirelessChargingStarted.ogg");
            lockSound = resolvePath("/system/media/audio/ui/samsung/Lock.ogg", "/product/media/audio/ui/Lock.ogg", "/system/media/audio/ui/Lock.ogg");
            unlockSound = resolvePath("/system/media/audio/ui/samsung/Unlock.ogg", "/product/media/audio/ui/Unlock.ogg", "/system/media/audio/ui/Unlock.ogg");
            lowBatterySound = resolvePath("/system/media/audio/ui/samsung/LowBattery.ogg", "/product/media/audio/ui/LowBattery.ogg", "/system/media/audio/ui/LowBattery.ogg");
            screenshotSound = resolvePath("/system/media/audio/ui/samsung/Screen_Capture.ogg", "/system/media/audio/ui/samsung/camera_click.ogg", "/product/media/audio/ui/camera_click.ogg");
        } else if (theme == 3) { // Apple iOS
            chargingSound = resolvePath("/system/media/audio/ui/ios/ChargingStarted.ogg", "/product/media/audio/ui/ios/ChargingStarted.ogg", "/product/media/audio/ui/ChargingStarted.ogg");
            disconnectSound = resolvePath("/system/media/audio/ui/ios/disconnect.ogg", "/product/media/audio/ui/ios/disconnect.ogg");
            wirelessChargingSound = resolvePath("/system/media/audio/ui/ios/ChargingStarted.ogg", "/product/media/audio/ui/ios/ChargingStarted.ogg", "/product/media/audio/ui/WirelessChargingStarted.ogg");
            lockSound = resolvePath("/system/media/audio/ui/ios/Lock.ogg", "/product/media/audio/ui/ios/Lock.ogg", "/product/media/audio/ui/Lock.ogg");
            unlockSound = resolvePath("/system/media/audio/ui/ios/Unlock.ogg", "/product/media/audio/ui/ios/Unlock.ogg", "/product/media/audio/ui/Unlock.ogg");
            lowBatterySound = resolvePath("/system/media/audio/ui/ios/LowBattery.ogg", "/product/media/audio/ui/ios/LowBattery.ogg", "/product/media/audio/ui/LowBattery.ogg");
            screenshotSound = resolvePath("/system/media/audio/ui/ios/screenshot.ogg", "/product/media/audio/ui/ios/screenshot.ogg", "/system/media/audio/ui/ios/camera_click.ogg", "/product/media/audio/ui/camera_click.ogg");
        } else { // Stock / AOSP
            chargingSound = resolvePath("/product/media/audio/ui/ChargingStarted.ogg", "/system/media/audio/ui/ChargingStarted.ogg");
            disconnectSound = resolvePath("/product/media/audio/ui/Undock.ogg", "/system/media/audio/ui/Undock.ogg");
            wirelessChargingSound = resolvePath("/product/media/audio/ui/WirelessChargingStarted.ogg", "/system/media/audio/ui/WirelessChargingStarted.ogg");
            lockSound = resolvePath("/product/media/audio/ui/Lock.ogg", "/system/media/audio/ui/Lock.ogg");
            unlockSound = resolvePath("/product/media/audio/ui/Unlock.ogg", "/system/media/audio/ui/Unlock.ogg");
            lowBatterySound = resolvePath("/product/media/audio/ui/LowBattery.ogg", "/system/media/audio/ui/LowBattery.ogg");
            screenshotSound = resolvePath("/product/media/audio/ui/camera_click.ogg", "/system/media/audio/ui/camera_click.ogg");
        }

        try {
            if (chargingSound != null) {
                Settings.Global.putString(mContext.getContentResolver(), Settings.Global.CHARGING_STARTED_SOUND, chargingSound);
            }
            if (disconnectSound != null) {
                Settings.Global.putString(mContext.getContentResolver(), "charging_stopped_sound", disconnectSound);
            }
            if (wirelessChargingSound != null) {
                Settings.Global.putString(mContext.getContentResolver(), Settings.Global.WIRELESS_CHARGING_STARTED_SOUND, wirelessChargingSound);
            }
            if (lockSound != null) {
                Settings.Global.putString(mContext.getContentResolver(), Settings.Global.LOCK_SOUND, lockSound);
            }
            if (unlockSound != null) {
                Settings.Global.putString(mContext.getContentResolver(), Settings.Global.UNLOCK_SOUND, unlockSound);
            }
            if (lowBatterySound != null) {
                Settings.Global.putString(mContext.getContentResolver(), Settings.Global.LOW_BATTERY_SOUND, lowBatterySound);
            }
            if (screenshotSound != null) {
                Settings.System.putString(mContext.getContentResolver(), "custom_screenshot_sound", screenshotSound);
            }
        } catch (Exception e) {
            android.util.Log.e("UiSoundThemeCtrl", "Failed to update global sound paths for theme " + theme, e);
        }
    }

    private String resolvePath(String... candidates) {
        for (String path : candidates) {
            if (path != null && new java.io.File(path).exists()) {
                return path;
            }
        }
        return candidates.length > 0 ? candidates[candidates.length - 1] : null;
    }
}
