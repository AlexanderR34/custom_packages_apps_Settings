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

package com.android.settings.display;

import android.content.Context;
import android.os.IBinder;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;

/**
 * Controller for hardware DC Dimming (Anti-Flicker) mode.
 * Directly interacts with the kernel sysfs dc_status node and Xiaomi DisplayFeature HAL.
 */
public class DcDimmingPreferenceController extends TogglePreferenceController {

    private static final String TAG = "DcDimmingPrefCtrl";
    public static final String KEY_DC_DIMMING = "dc_dimming_state";

    private static final String[] DC_NODES = {
        "/sys/devices/virtual/mi_display/disp_feature/disp-DSI-0/dc_status",
        "/sys/class/mi_display/disp-DSI-0/dc_status"
    };

    public DcDimmingPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        boolean hasNode = false;
        for (String path : DC_NODES) {
            if (new File(path).exists()) {
                hasNode = true;
                break;
            }
        }
        boolean hasService = ServiceManager.checkService(
                "vendor.xiaomi.hardware.displayfeature_aidl.IDisplayFeature/default") != null;
        return (hasNode || hasService) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        for (String path : DC_NODES) {
            try {
                File file = new File(path);
                if (file.exists() && file.canRead()) {
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                        String line = reader.readLine();
                        if (line != null && !line.trim().isEmpty()) {
                            return "1".equals(line.trim());
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        return Settings.System.getInt(
                mContext.getContentResolver(), KEY_DC_DIMMING, 0) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        boolean success = Settings.System.putInt(
                mContext.getContentResolver(), KEY_DC_DIMMING, isChecked ? 1 : 0);
        applyDcDimming(isChecked);
        return success;
    }

    private void applyDcDimming(boolean enabled) {
        // 1. Direct sysfs node write
        for (String path : DC_NODES) {
            try {
                File node = new File(path);
                if (node.exists()) {
                    try (FileOutputStream fos = new FileOutputStream(node)) {
                        fos.write((enabled ? "1" : "0").getBytes());
                        fos.flush();
                    }
                }
            } catch (Throwable t) {
                // Ignore sysfs permissions failure and fallback to AIDL
            }
        }

        // 2. Dispatch to Xiaomi DisplayFeature AIDL
        try {
            IBinder binder = ServiceManager.checkService(
                    "vendor.xiaomi.hardware.displayfeature_aidl.IDisplayFeature/default");
            if (binder != null) {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken("vendor.xiaomi.hardware.displayfeature_aidl.IDisplayFeature");
                    data.writeInt(0 /* displayId */);
                    data.writeInt(8 /* mode: DISP_FEATURE_DC (DC Dimming mode) */);
                    data.writeInt(enabled ? 1 : 0 /* value */);
                    data.writeInt(0 /* cookie */);
                    // TRANSACTION_setFeature = FIRST_CALL_TRANSACTION + 6 (Method 7)
                    binder.transact(IBinder.FIRST_CALL_TRANSACTION + 6, data, reply, 0);
                    reply.readException();
                } finally {
                    data.recycle();
                    reply.recycle();
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "DisplayFeature AIDL DC Dimming call failed via binder transact: " + t.getMessage());
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }
}
