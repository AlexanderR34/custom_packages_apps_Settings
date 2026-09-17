
package com.android.settings.deviceinfo;

import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;

public class VersionUtils {
    public static String getCustomVersion() {
        String divaVer = SystemProperties.get("ro.diva.version", "");
        if (!TextUtils.isEmpty(divaVer)) {
            if (divaVer.startsWith("Project")) {
                return divaVer.replace("_", " ");
            }
            return "Project Diva " + divaVer.replace("_", " ");
        }
        String customVer = SystemProperties.get("ro.custom.version", "");
        if (!TextUtils.isEmpty(customVer)) {
            return customVer.replace("_", " ");
        }
        String display = Build.DISPLAY;
        if (!TextUtils.isEmpty(display)) {
            if (display.startsWith("Project")) {
                return display.replace("_", " ");
            }
            return "Project Diva " + display.replace("_", " ");
        }
        return "Project Diva 1.2.0";
    }
}
