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

import android.app.AlertDialog;
import android.content.Context;
import android.os.PowerManager;

import com.android.settings.R;

/**
 * Utility class to display reboot prompt when critical system settings change.
 */
public class RebootUtils {

    public static void showRebootPromptDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.reboot_prompt_title)
                .setMessage(R.string.reboot_prompt_message)
                .setPositiveButton(R.string.reboot_prompt_now, (dialog, which) -> {
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    if (pm != null) {
                        pm.reboot(null);
                    }
                })
                .setNegativeButton(R.string.reboot_prompt_later, null)
                .show();
    }
}
