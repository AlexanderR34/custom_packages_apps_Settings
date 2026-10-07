/*
 * Copyright (C) 2024-2026 The Android Open Source Project
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

public class ChipsColorLocationPreferenceController extends AbstractChipsHexColorPreferenceController {
    public static final String KEY = "status_bar_chips_color_location";
    public static final String DEFAULT_HEX = "#0091EA";

    public ChipsColorLocationPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    protected String getSettingKey() {
        return KEY;
    }

    @Override
    protected String getDefaultHex() {
        return DEFAULT_HEX;
    }
}
