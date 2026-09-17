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
import android.view.View;
import android.widget.CompoundButton;

import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

public class Sm64CoinPreference extends SystemCustomSoundPreference {

    private SwitchCompat mSwitch;
    private boolean mChecked = true;

    public Sm64CoinPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(com.android.settingslib.R.layout.preference_widget_primary_switch);
    }

    @Override
    public void onPrepareRingtonePickerIntent(Intent ringtonePickerIntent) {
        super.onPrepareRingtonePickerIntent(ringtonePickerIntent);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getTitle());
    }

    @Override
    protected void onSaveRingtone(Uri ringtoneUri) {
        String uriStr = (ringtoneUri != null) ? ringtoneUri.toString() : "";
        Settings.System.putString(getContext().getContentResolver(), getKey() + "_uri", uriStr);
        updateSummary();
    }

    @Override
    protected Uri onRestoreRingtone() {
        String uriString = Settings.System.getString(getContext().getContentResolver(), getKey() + "_uri");
        if (TextUtils.isEmpty(uriString)) {
            return null;
        }
        try {
            return Uri.parse(uriString);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isChecked() {
        return Settings.System.getInt(getContext().getContentResolver(), getKey() + "_enabled", 1) == 1;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        Settings.System.putInt(getContext().getContentResolver(), getKey() + "_enabled", checked ? 1 : 0);
        if (mSwitch != null) {
            mSwitch.setChecked(checked);
        }
        updateSummary();
    }

    @Override
    public void updateSummary() {
        boolean enabled = isChecked();
        Uri uri = onRestoreRingtone();
        if (uri == null) {
            if (enabled) {
                setSummary(getContext().getString(R.string.sm64_coin_default_sound));
            } else {
                setSummary(getContext().getString(R.string.sm64_coin_disabled_sound));
            }
        } else {
            try {
                CharSequence title = Ringtone.getTitle(getContext(), uri, false, true);
                if (!TextUtils.isEmpty(title)) {
                    if (enabled) {
                        setSummary(title);
                    } else {
                        setSummary(title + " (" + getContext().getString(R.string.sm64_coin_disabled_tag) + ")");
                    }
                } else {
                    setSummary(getContext().getString(R.string.sm64_coin_default_sound));
                }
            } catch (Throwable t) {
                setSummary(getContext().getString(R.string.sm64_coin_default_sound));
            }
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View switchView = holder.findViewById(android.R.id.switch_widget);
        if (switchView == null) {
            switchView = holder.findViewById(com.android.settingslib.R.id.switchWidget);
        }

        if (switchView instanceof SwitchCompat) {
            mSwitch = (SwitchCompat) switchView;
            mSwitch.setOnCheckedChangeListener(null);
            mChecked = isChecked();
            mSwitch.setChecked(mChecked);
            mSwitch.setOnClickListener(v -> {
                boolean newChecked = mSwitch.isChecked();
                setChecked(newChecked);
            });
            mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                setChecked(isChecked);
            });
        }
        updateSummary();
    }
}
