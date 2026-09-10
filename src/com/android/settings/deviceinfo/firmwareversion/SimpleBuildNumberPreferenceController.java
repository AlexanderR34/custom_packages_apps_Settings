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

import android.content.Context;
import android.os.Build;
import android.text.BidiFormatter;
import android.text.TextUtils;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.deviceinfo.VersionUtils;

// LINT.IfChange
public class SimpleBuildNumberPreferenceController extends BasePreferenceController {

    public SimpleBuildNumberPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public CharSequence getSummary() {
        String display = Build.DISPLAY != null ? Build.DISPLAY.replace("\\n", " ").replace("\n", " ").trim() : "";
        String customVersion = VersionUtils.getCustomVersion();
        if (!TextUtils.isEmpty(customVersion)) {
            String cleanVersion = customVersion.replace("\\n", " ").replace("\n", " ").trim();
            if (!cleanVersion.isEmpty()) {
                if (display.isEmpty() || display.contains(cleanVersion)) {
                    return BidiFormatter.getInstance().unicodeWrap(cleanVersion);
                } else if (cleanVersion.contains(display)) {
                    return BidiFormatter.getInstance().unicodeWrap(cleanVersion);
                } else {
                    return BidiFormatter.getInstance().unicodeWrap(cleanVersion + " (" + display + ")");
                }
            }
        }
        return BidiFormatter.getInstance().unicodeWrap(display);
    }
}
// LINT.ThenChange(SimpleBuildNumberPreference.kt)
