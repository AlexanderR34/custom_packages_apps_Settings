/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.deviceinfo;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.core.AbstractPreferenceController;

public class KernelVersionPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String KEY_KERNEL_VERSION = "kernel_version";

    public KernelVersionPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(DeviceInfoUtils.getFormattedKernelVersion(mContext));
    }

    private static final int TAPS_TO_EASTER_EGG = 3;
    private static final long DELAY_TIMER_MILLIS = 1200;
    private final long[] mHits = new long[TAPS_TO_EASTER_EGG];

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!android.text.TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
        mHits[mHits.length - 1] = android.os.SystemClock.uptimeMillis();
        if (mHits[0] >= (android.os.SystemClock.uptimeMillis() - DELAY_TIMER_MILLIS)) {
            android.content.ComponentName[] targetComponents = new android.content.ComponentName[] {
                new android.content.ComponentName("org.mupen64plusae.v3.alpha", "paulscode.android.mupen64plusae.SplashActivity"),
                new android.content.ComponentName("org.mupen64plusae.v3.alpha", "paulscode.android.mupen64plusae.GalleryActivity"),
                new android.content.ComponentName("org.lunaris.easteregg", "org.lunaris.easteregg.BridgeActivity"),
                new android.content.ComponentName("org.mupen64plusae.v3.alpha", "org.mupen64plusae.v3.alpha.MenuActivity"),
                new android.content.ComponentName("org.mupen64plusae.v3.fzurita", "org.mupen64plusae.v3.fzurita.MenuActivity"),
                new android.content.ComponentName("org.mupen64plusae.v3.fzurita.pro", "org.mupen64plusae.v3.fzurita.pro.MenuActivity"),
                new android.content.ComponentName("com.retroarch.aarch64", "com.retroarch.browser.retroactivity.RetroActivityFuture"),
                new android.content.ComponentName("com.retroarch", "com.retroarch.browser.retroactivity.RetroActivityFuture")
            };

            for (android.content.ComponentName component : targetComponents) {
                try {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_MAIN);
                    intent.setComponent(component);
                    intent.addCategory(android.content.Intent.CATEGORY_DEFAULT);
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mContext.startActivity(intent);
                    return true;
                } catch (android.content.ActivityNotFoundException ignored) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

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
                    android.content.Intent launchIntent = mContext.getPackageManager().getLaunchIntentForPackage(pkg);
                    if (launchIntent != null) {
                        launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        mContext.startActivity(launchIntent);
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_KERNEL_VERSION;
    }
}
