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
import android.content.Context;

public class SystemUiRestartUtils {

    public static void showRestartDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Reiniciar SystemUI")
                .setMessage("¿Deseas reiniciar la interfaz del sistema (SystemUI) ahora para aplicar los cambios visuales?")
                .setPositiveButton("Reiniciar ahora", (dialog, which) -> restartSystemUI(context))
                .setNegativeButton("Más tarde", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static void restartSystemUI(Context context) {
        try {
            ActivityManager am = context.getSystemService(ActivityManager.class);
            if (am != null) {
                am.killBackgroundProcesses("com.android.systemui");
            }
        } catch (Exception ignored) {}

        try {
            Runtime.getRuntime().exec(new String[]{"pkill", "-f", "com.android.systemui"});
        } catch (Exception ignored) {}
    }
}
