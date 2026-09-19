/*
 * Copyright (C) 2026 Project Diva
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

package com.android.settings.connecteddevice.usb;

import static com.android.settingslib.RestrictedLockUtilsInternal.checkIfUsbDataSignalingIsDisabled;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnPause;

/**
 * Controller to manage USB Debugging (ADB) directly from USB details screen.
 */
public class UsbDetailsAdbController extends UsbDetailsController
        implements Preference.OnPreferenceChangeListener, LifecycleObserver, OnResume, OnPause {

    public static final String KEY_USB_DETAILS_DEBUGGING = "usb_details_debugging";

    private TwoStatePreference mPreference;
    private final ContentObserver mAdbObserver;
    private final UserManager mUserManager;

    public UsbDetailsAdbController(Context context, UsbDetailsFragment fragment, UsbBackend backend) {
        super(context, fragment, backend);
        mUserManager = context.getSystemService(UserManager.class);
        mAdbObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                updateAdbState();
            }
        };

        if (fragment != null && fragment.getSettingsLifecycle() != null) {
            fragment.getSettingsLifecycle().addObserver(this);
        }
    }

    @Override
    public boolean isAvailable() {
        return mUserManager != null && mUserManager.isAdminUser();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_USB_DETAILS_DEBUGGING;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY_USB_DETAILS_DEBUGGING);
        updateAdbState();
    }

    @Override
    public void onResume() {
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ADB_ENABLED),
                false,
                mAdbObserver);
        updateAdbState();
    }

    @Override
    public void onPause() {
        mContext.getContentResolver().unregisterContentObserver(mAdbObserver);
    }

    @Override
    protected void refresh(boolean connected, long functions, int powerRole, int dataRole) {
        updateAdbState();
    }

    private boolean isAdbEnabled() {
        final ContentResolver cr = mContext.getContentResolver();
        return Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) != 0;
    }

    private void updateAdbState() {
        if (mPreference != null) {
            boolean enabled = isAdbEnabled();
            if (mPreference.isChecked() != enabled) {
                mPreference.setChecked(enabled);
            }
            if (mPreference instanceof RestrictedSwitchPreference) {
                ((RestrictedSwitchPreference) mPreference).setDisabledByAdmin(
                        checkIfUsbDataSignalingIsDisabled(mContext, UserHandle.myUserId()));
            }
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        if (KEY_USB_DETAILS_DEBUGGING.equals(preference.getKey())) {
            boolean enable = (Boolean) newValue;
            requireAuthAndExecute(() -> {
                Settings.Global.putInt(mContext.getContentResolver(),
                        Settings.Global.ADB_ENABLED, enable ? 1 : 0);
                updateAdbState();
            });
            return true;
        }
        return false;
    }
}
