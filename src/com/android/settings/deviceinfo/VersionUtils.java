
package com.android.settings.deviceinfo;

import android.os.SystemProperties;
import android.text.TextUtils;

public class VersionUtils {
    public static String getCustomVersion() {
        String divaVer = SystemProperties.get("ro.diva.version", "");
        if (!TextUtils.isEmpty(divaVer)) {
            return divaVer;
        }
        String customVer = SystemProperties.get("ro.custom.version", "");
        if (!TextUtils.isEmpty(customVer)) {
            return customVer;
        }
        return "Project Diva 1.1.2";
    }
}
