/*
 * Copyright (C) 2021-2022 Chaldeaprjkt
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

package com.android.settings.applications;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

public class GameSpaceController extends BasePreferenceController {

    private static final String PACKAGE_NAME = "io.chaldeaprjkt.gamespace";
    private static final String ACTIVITY_NAME = "io.chaldeaprjkt.gamespace.settings.SettingsActivity";

    public GameSpaceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        try {
            mContext.getPackageManager().getPackageInfo(PACKAGE_NAME, 0);
            return AVAILABLE;
        } catch (PackageManager.NameNotFoundException e) {
            return UNSUPPORTED_ON_DEVICE;
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!getPreferenceKey().equals(preference.getKey())) {
            return false;
        }

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(PACKAGE_NAME, ACTIVITY_NAME));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
        return true;
    }
}
