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

package com.android.settings.utils;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.IActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

public class SystemUiRestartUtils {
    private static final String TAG = "SystemUiRestartUtils";

    public static void showRestartDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(com.android.settings.R.string.systemui_restart_dialog_title)
                .setMessage(com.android.settings.R.string.systemui_restart_dialog_message)
                .setPositiveButton(com.android.settings.R.string.systemui_restart_dialog_positive, (dialog, which) -> restartSystemUI(context))
                .setNegativeButton(com.android.settings.R.string.systemui_restart_dialog_negative, (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static void restartSystemUI(Context context) {
        new Thread(() -> {
            try {
                IActivityManager am = ActivityManager.getService();
                if (am != null) {
                    int uid = -1;
                    try {
                        uid = context.getPackageManager().getPackageUid("com.android.systemui", 0);
                    } catch (PackageManager.NameNotFoundException ignored) {}

                    if (uid != -1) {
                        am.killApplicationProcess("com.android.systemui", uid);
                        return;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to kill SystemUI via ActivityManager.getService()", e);
            }

            try {
                ActivityManager am = context.getSystemService(ActivityManager.class);
                if (am != null && am.getRunningAppProcesses() != null) {
                    for (ActivityManager.RunningAppProcessInfo process : am.getRunningAppProcesses()) {
                        if ("com.android.systemui".equals(process.processName)) {
                            ActivityManager.getService().killApplicationProcess(process.processName, process.uid);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to kill SystemUI via RunningAppProcessInfo", e);
            }
        }).start();
    }
}
