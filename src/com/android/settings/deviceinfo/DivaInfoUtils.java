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

package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Process;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Formatter;

import com.android.settings.R;

import java.util.Date;

public class DivaInfoUtils {

    public static String getDivaVersion() {
        return SystemProperties.get("ro.diva.version", "1.1.2");
    }

    public static String getMaintainer() {
        return SystemProperties.get("ro.diva.maintainer", "AlexMainMandy");
    }

    public static String getBuildType(Context context) {
        String prop = SystemProperties.get("ro.diva.buildtype", "");
        if ("Unofficial".equalsIgnoreCase(prop)) {
            return context.getString(R.string.diva_build_type_unofficial);
        }
        return context.getString(R.string.diva_build_type_official);
    }

    public static String getBuildDate(Context context) {
        String utc = SystemProperties.get("ro.build.date.utc", "");
        if (!TextUtils.isEmpty(utc)) {
            try {
                long timestamp = Long.parseLong(utc) * 1000L;
                return DateFormat.getMediumDateFormat(context).format(new Date(timestamp));
            } catch (Exception ignored) {
            }
        }
        String buildDate = SystemProperties.get("ro.build.date", "");
        if (!TextUtils.isEmpty(buildDate)) {
            return buildDate;
        }
        return SystemProperties.get("ro.custom.build.date", "");
    }

    public static String getTotalStorage(Context context) {
        long bytes = 0;
        StorageManager sm = context.getSystemService(StorageManager.class);
        if (sm != null) {
            try {
                bytes = sm.getPrimaryStorageSize();
            } catch (Exception ignored) {
            }
        }
        if (bytes <= 0) {
            try {
                bytes = FileUtils.roundStorageSize(Environment.getDataDirectory().getTotalSpace()
                        + Environment.getRootDirectory().getTotalSpace());
            } catch (Exception ignored) {
            }
        }
        if (bytes > 0) {
            return Formatter.formatFileSize(context, bytes);
        }
        return "128 GB";
    }

    public static String getTotalRam() {
        long totalBytes = Process.getTotalMemory();
        if (totalBytes <= 0) {
            return "8 GB";
        }
        double gb = (double) totalBytes / (1024.0 * 1024.0 * 1024.0);
        int ramGb;
        if (gb > 20.0) {
            ramGb = 24;
        } else if (gb > 14.0) {
            ramGb = 16;
        } else if (gb > 10.0) {
            ramGb = 12;
        } else if (gb > 6.2) {
            ramGb = 8;
        } else if (gb > 4.5) {
            ramGb = 6;
        } else if (gb > 3.2) {
            ramGb = 4;
        } else if (gb > 2.2) {
            ramGb = 3;
        } else if (gb > 1.2) {
            ramGb = 2;
        } else {
            ramGb = 1;
        }
        return ramGb + " GB";
    }
}
