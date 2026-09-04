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

package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.text.BidiFormatter;
import android.text.format.DateFormat;

import com.android.settings.R;

import java.util.Date;

public class AppVersionPreferenceController extends AppInfoPreferenceControllerBase {

    public AppVersionPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public CharSequence getSummary() {
        final PackageInfo packageInfo = mParent.getPackageInfo();
        if (packageInfo == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(mContext.getString(R.string.version_text,
                BidiFormatter.getInstance().unicodeWrap(packageInfo.versionName)));

        if (packageInfo.firstInstallTime > 0) {
            java.text.DateFormat dateFormat = DateFormat.getMediumDateFormat(mContext);
            java.text.DateFormat timeFormat = DateFormat.getTimeFormat(mContext);
            Date installDate = new Date(packageInfo.firstInstallTime);
            String formattedDate = dateFormat.format(installDate);
            String formattedTime = timeFormat.format(installDate);

            sb.append("\n");
            sb.append(mContext.getString(R.string.app_install_time_format, formattedDate, formattedTime));
        }

        return sb.toString();
    }
}
