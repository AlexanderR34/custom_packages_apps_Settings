/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.sound;

import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.RingtonePreference;

public class SystemCustomSoundPreference extends RingtonePreference {

    public SystemCustomSoundPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onPrepareRingtonePickerIntent(Intent ringtonePickerIntent) {
        super.onPrepareRingtonePickerIntent(ringtonePickerIntent);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
    }

    @Override
    protected void onSaveRingtone(Uri ringtoneUri) {
        Settings.System.putString(getContext().getContentResolver(), getKey() + "_uri",
                ringtoneUri != null ? ringtoneUri.toString() : "");
        updateSummary();
    }

    @Override
    protected Uri onRestoreRingtone() {
        String uriString = Settings.System.getString(getContext().getContentResolver(), getKey() + "_uri");
        if (TextUtils.isEmpty(uriString)) {
            return null;
        }
        return Uri.parse(uriString);
    }

    public void updateSummary() {
        Uri uri = onRestoreRingtone();
        if (uri == null) {
            setSummary(getContext().getString(R.string.custom_sound_none));
        } else {
            try {
                CharSequence title = Ringtone.getTitle(getContext(), uri, false /* followSettingsUri */, true /* allowRemote */);
                setSummary(!TextUtils.isEmpty(title) ? title : getContext().getString(R.string.custom_sound_none));
            } catch (Throwable t) {
                setSummary(getContext().getString(R.string.custom_sound_none));
            }
        }
    }

    @Override
    public void onAttached() {
        super.onAttached();
        updateSummary();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
    }
}
