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
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import java.io.File;

/**
 * Controller for High Touch Sampling Rate / Touch Boost (500 Hz vs 120 Hz).
 * Communicates directly with Xiaomi ITouchFeature HAL via native Binder IPC.
 */
public class TouchSamplingRatePreferenceController extends TogglePreferenceController {

    private static final String TAG = "TouchSamplingRateCtrl";
    public static final String KEY_TOUCH_BOOST = "touch_sampling_rate";

    private static final String TOUCH_DEVICE_NODE = "/dev/xiaomi-touch";
    private static final String TOUCH_SERVICE_NAME = "vendor.xiaomi.hw.touchfeature.ITouchFeature/default";

    public TouchSamplingRatePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        boolean hasDev = new File(TOUCH_DEVICE_NODE).exists();
        boolean hasService = ServiceManager.checkService(TOUCH_SERVICE_NAME) != null;
        return (hasDev || hasService) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(
                mContext.getContentResolver(), KEY_TOUCH_BOOST, 0) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        boolean success = Settings.System.putInt(
                mContext.getContentResolver(), KEY_TOUCH_BOOST, isChecked ? 1 : 0);
        applyTouchSamplingRate(isChecked);
        return success;
    }

    /**
     * Dispatches the 500 Hz boost mode or 120 Hz normal mode to Xiaomi TouchFeature HAL.
     */
    private void applyTouchSamplingRate(boolean enabled) {
        int state = enabled ? 1 : 0;

        // Xiaomi touch modes:
        // 0: GAME_MODE (1=on, 0=off)
        // 202: SAMPLE_RATE_EXT (1=500Hz boost on, 0=off)
        // 1: HIGH_RATE (1=high polling path on, 0=off)
        // 3: REPORT_RATE (34=boosted ~500Hz, 0=standard 120Hz)
        // 2: SENSITIVITY (99=max game sensitivity, 0=normal)
        // 7: POWER_SAVE (0=boost on, 1=power save on)
        setTouchModeValue(0, 0, state);
        setTouchModeValue(0, 202, state);
        setTouchModeValue(0, 1, state);
        setTouchModeValue(0, 3, enabled ? 34 : 0);
        setTouchModeValue(0, 2, enabled ? 99 : 0);
        setTouchModeValue(0, 7, enabled ? 0 : 1);

        Log.d(TAG, "Applied touch sampling rate: 500Hz boost=" + enabled);
    }

    private static void setTouchModeValue(int type, int mode, int value) {
        try {
            IBinder binder = ServiceManager.checkService(TOUCH_SERVICE_NAME);
            if (binder != null) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("vendor.xiaomi.hw.touchfeature.ITouchFeature");
                    data.writeInt(type);
                    data.writeInt(mode);
                    data.writeInt(value);
                    // TRANSACTION_setModeValue = FIRST_CALL_TRANSACTION + 8 (Method 9)
                    binder.transact(IBinder.FIRST_CALL_TRANSACTION + 8, data, reply, 0);
                    reply.readException();
                } finally {
                    data.recycle();
                    reply.recycle();
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "TouchFeature binder transact failed: " + t.getMessage());
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }
}
