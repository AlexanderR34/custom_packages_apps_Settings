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
import java.lang.reflect.Method;

/**
 * Controller for High-Frequency PWM Dimming (1920 Hz / Eye Protection mode).
 * Dispatches commands to Xiaomi DisplayFeature AIDL (Feature ID 0: DISP_FEATURE_DIMMING).
 */
public class HighPwmDimmingPreferenceController extends TogglePreferenceController {

    private static final String TAG = "HighPwmDimmingCtrl";
    public static final String KEY_PWM_DIMMING = "pwm_dimming_state";
    private static final String DISP_PARAM_NODE =
            "/sys/devices/virtual/mi_display/disp_feature/disp-DSI-0/disp_param";

    public HighPwmDimmingPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        boolean hasNode = new File(DISP_PARAM_NODE).exists();
        boolean hasService = ServiceManager.checkService(
                "vendor.xiaomi.hardware.displayfeature_aidl.IDisplayFeature/default") != null;
        return (hasNode || hasService) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(
                mContext.getContentResolver(), KEY_PWM_DIMMING, 0) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        boolean success = Settings.System.putInt(
                mContext.getContentResolver(), KEY_PWM_DIMMING, isChecked ? 1 : 0);
        applyPwmDimming(isChecked);
        return success;
    }

    private void applyPwmDimming(boolean enabled) {
        try {
            IBinder binder = ServiceManager.checkService(
                    "vendor.xiaomi.hardware.displayfeature_aidl.IDisplayFeature/default");
            if (binder != null) {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken("vendor.xiaomi.hardware.displayfeature_aidl.IDisplayFeature");
                    data.writeInt(0 /* displayId */);
                    data.writeInt(0 /* mode: DISP_FEATURE_DIMMING (1920Hz PWM) */);
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
            Log.w(TAG, "Failed to apply High PWM dimming via binder transact: " + t.getMessage());
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }
}
