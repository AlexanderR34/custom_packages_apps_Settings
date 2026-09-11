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
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.DropDownPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.ArrayList;
import java.util.List;

public class SeparateAppSoundDevicePickerController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private final AudioManager mAudioManager;

    public SeparateAppSoundDevicePickerController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mAudioManager = context.getSystemService(AudioManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof DropDownPreference)) return;
        DropDownPreference dropDown = (DropDownPreference) preference;

        AudioDeviceInfo[] devices = mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        List<DeviceEntry> entriesList = new ArrayList<>();

        entriesList.add(new DeviceEntry(
                mContext.getString(R.string.separate_app_sound_device_speaker),
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                ""));

        for (AudioDeviceInfo dev : devices) {
            if (dev.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                    || dev.getType() == AudioDeviceInfo.TYPE_BLE_HEADSET
                    || dev.getType() == AudioDeviceInfo.TYPE_BLE_SPEAKER
                    || dev.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    || dev.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                    || dev.getType() == AudioDeviceInfo.TYPE_USB_HEADSET
                    || dev.getType() == AudioDeviceInfo.TYPE_USB_DEVICE) {
                String name = dev.getProductName() != null ? dev.getProductName().toString() : "Audio Device";
                String label = mContext.getString(R.string.separate_app_sound_device_bluetooth, name);
                entriesList.add(new DeviceEntry(label, dev.getType(), dev.getAddress()));
            }
        }

        CharSequence[] entries = new CharSequence[entriesList.size()];
        CharSequence[] entryValues = new CharSequence[entriesList.size()];

        int currentType = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SEPARATE_APP_SOUND_TARGET_DEVICE_TYPE, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
        String currentAddress = Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.SEPARATE_APP_SOUND_TARGET_DEVICE_ADDRESS);

        int selectedIndex = 0;
        for (int i = 0; i < entriesList.size(); i++) {
            DeviceEntry entry = entriesList.get(i);
            entries[i] = entry.label;
            entryValues[i] = entry.type + ";" + entry.address;

            if (entry.type == currentType && TextUtils.equals(entry.address, currentAddress)) {
                selectedIndex = i;
            }
        }

        dropDown.setEntries(entries);
        dropDown.setEntryValues(entryValues);
        dropDown.setValueIndex(selectedIndex);
        dropDown.setSummary(entries[selectedIndex]);
        dropDown.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String value = (String) newValue;
        String[] parts = value.split(";", 2);
        int type = Integer.parseInt(parts[0]);
        String address = parts.length > 1 ? parts[1] : "";

        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.SEPARATE_APP_SOUND_TARGET_DEVICE_TYPE, type);
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.SEPARATE_APP_SOUND_TARGET_DEVICE_ADDRESS, address);

        updateState(preference);
        return true;
    }

    private static class DeviceEntry {
        final String label;
        final int type;
        final String address;

        DeviceEntry(String label, int type, String address) {
            this.label = label;
            this.type = type;
            this.address = address;
        }
    }
}
