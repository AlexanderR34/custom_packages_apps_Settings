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

package com.android.settings.system;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.preference.Preference;
import com.android.settings.core.BasePreferenceController;

public class LMOFreeformPreferenceController extends BasePreferenceController {

    private static final String[] POSSIBLE_PACKAGES = {
        "org.lmodroid.freeform",
        "com.libremobileos.freeform"
    };

    public LMOFreeformPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        PackageManager pm = mContext.getPackageManager();
        for (String pkg : POSSIBLE_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
                return AVAILABLE;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return AVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!getPreferenceKey().equals(preference.getKey())) {
            return false;
        }

        PackageManager pm = mContext.getPackageManager();
        for (String pkg : POSSIBLE_PACKAGES) {
            Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(launchIntent);
                return true;
            }
        }

        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setPackage("org.lmodroid.freeform");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
