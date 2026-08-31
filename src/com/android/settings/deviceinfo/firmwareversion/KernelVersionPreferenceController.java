/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.deviceinfo.firmwareversion;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.DeviceInfoUtils;

// LINT.IfChange
public class KernelVersionPreferenceController extends BasePreferenceController {

    private static final String TAG = "KernelVersionPrefCtrl";
    private static final int DELAY_TIMER_MILLIS = 1200;
    private static final int ACTIVITY_TRIGGER_COUNT = 3;

    private final UserManager mUserManager;
    private final long[] mHits = new long[ACTIVITY_TRIGGER_COUNT];

    public KernelVersionPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return DeviceInfoUtils.getFormattedKernelVersion(mContext);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(getPreferenceKey());
        if (preference != null) {
            preference.setSelectable(true);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        if (Utils.isMonkeyRunning()) {
            return false;
        }
        arrayCopy();
        mHits[mHits.length - 1] = SystemClock.uptimeMillis();
        if (mHits[0] >= (SystemClock.uptimeMillis() - DELAY_TIMER_MILLIS)) {
            launchN64EasterEgg();
            return true;
        }
        return false;
    }

    private void launchN64EasterEgg() {
        ComponentName[] targetComponents = new ComponentName[] {
            new ComponentName("org.mupen64plusae.v3.alpha", "paulscode.android.mupen64plusae.SplashActivity"),
            new ComponentName("org.mupen64plusae.v3.alpha", "paulscode.android.mupen64plusae.GalleryActivity"),
            new ComponentName("org.lunaris.easteregg", "org.lunaris.easteregg.BridgeActivity"),
            new ComponentName("org.mupen64plusae.v3.alpha", "org.mupen64plusae.v3.alpha.MenuActivity"),
            new ComponentName("org.mupen64plusae.v3.fzurita", "org.mupen64plusae.v3.fzurita.MenuActivity"),
            new ComponentName("org.mupen64plusae.v3.fzurita.pro", "org.mupen64plusae.v3.fzurita.pro.MenuActivity"),
            new ComponentName("com.retroarch.aarch64", "com.retroarch.browser.retroactivity.RetroActivityFuture"),
            new ComponentName("com.retroarch", "com.retroarch.browser.retroactivity.RetroActivityFuture")
        };

        for (ComponentName component : targetComponents) {
            try {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setComponent(component);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mContext.startActivity(intent);
                return;
            } catch (ActivityNotFoundException ignored) {
            } catch (Exception e) {
                Log.e(TAG, "Error launching " + component, e);
            }
        }

        // Fallback: Launch intent by package
        String[] candidatePackages = new String[] {
            "org.lunaris.easteregg",
            "org.mupen64plusae.v3.alpha",
            "org.mupen64plusae.v3.fzurita",
            "org.mupen64plusae.v3.fzurita.pro",
            "com.retroarch.aarch64",
            "com.retroarch"
        };
        for (String pkg : candidatePackages) {
            try {
                Intent launchIntent = mContext.getPackageManager().getLaunchIntentForPackage(pkg);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mContext.startActivity(launchIntent);
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        Toast.makeText(mContext, "N64 Emulator Easter Egg", Toast.LENGTH_SHORT).show();
    }

    private void arrayCopy() {
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
    }
}
// LINT.ThenChange(KernelVersionPreference.kt)
