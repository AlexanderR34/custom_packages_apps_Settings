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

package com.android.settings.notification;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.provider.Settings;
import android.util.Log;

/**
 * Volume Boost Manager for Settings UI.
 *
 * Manages the Volume Boost Level (100% to 200%) and Call Audio Gain settings.
 */
public class VolumeBoostManager {
    private static final String TAG = "VolumeBoostManager";

    public static final String SETTING_KEY = Settings.System.VOLUME_BOOST_LEVEL;
    public static final String SETTING_CALL_GAIN_KEY = "volume_boost_call_gain";
    public static final int DEFAULT_BOOST_LEVEL = 0; // 0% boost = 100% standard volume
    private static final float MAX_BOOST_GAIN_DB = 8.0f;

    private static VolumeBoostManager sInstance;

    private final Context mContext;
    private final AudioManager mAudioManager;
    private VolumeBoostManager(Context context) {
        mContext = context.getApplicationContext();
        mAudioManager = mContext.getSystemService(AudioManager.class);
    }

    public static synchronized VolumeBoostManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new VolumeBoostManager(context);
        }
        return sInstance;
    }

    /**
     * Checks if a Bluetooth or LE Audio output device is currently connected.
     */
    public boolean isBluetoothAudioConnected() {
        if (mAudioManager == null) return false;
        try {
            AudioDeviceInfo[] devices = mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            if (devices != null) {
                for (AudioDeviceInfo device : devices) {
                    int type = device.getType();
                    if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                            || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                            || type == AudioDeviceInfo.TYPE_BLE_HEADSET
                            || type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                            || type == AudioDeviceInfo.TYPE_BLE_BROADCAST
                            || type == AudioDeviceInfo.TYPE_HEARING_AID) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error querying audio devices: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves the current boost level from Settings.System.
     * @return 0 to 100 (where 0 is standard volume 100%, 100 is maximum clean boost 200%).
     */
    public int getBoostLevel() {
        return Settings.System.getInt(mContext.getContentResolver(), SETTING_KEY, DEFAULT_BOOST_LEVEL);
    }

    /**
     * Sets the volume boost level in Settings.System.
     * The persistent VolumeBoostHelper in AudioService handles live audio processing.
     * @param level boost percentage level (0 to 100)
     */
    public void setBoostLevel(int level) {
        int clampedLevel = Math.max(0, Math.min(100, level));
        Settings.System.putInt(mContext.getContentResolver(), SETTING_KEY, clampedLevel);
    }

    public boolean isCallAudioGainEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(), SETTING_CALL_GAIN_KEY, 1) == 1;
    }

    public void setCallAudioGainEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(), SETTING_CALL_GAIN_KEY, enabled ? 1 : 0);
    }
}






