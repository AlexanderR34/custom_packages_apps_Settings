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

package com.android.settings.sound;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.notification.VolumeBoostManager;
import com.android.settings.widget.SeekBarPreference;

import java.util.Locale;

/**
 * Material 3 Expressive SeekBarPreference designed for Volume Boost control.
 * Smoothly maps slider progress (0 - 100) to device volume output (100% - 250% / up to +15.0 dB)
 * with a dynamic badge chip, real-time gain summary, and precision haptic feedback.
 */
public class VolumeBoostSeekBarPreference extends SeekBarPreference {

    private TextView mValueBadgeView;
    private TextView mSummaryView;
    private ImageView mIconView;
    private int mLastHapticProgress = -1;
    private int mAccentColor = 0;
    private int mNormalIconColor = 0;

    public VolumeBoostSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public VolumeBoostSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public VolumeBoostSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VolumeBoostSeekBarPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_volume_boost_expressive);
        setMin(0);
        setMax(100);
        setContinuousUpdates(true);

        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.colorAccent, typedValue, true);
        mAccentColor = typedValue.data;

        getContext().getTheme().resolveAttribute(android.R.attr.colorControlNormal, typedValue, true);
        mNormalIconColor = typedValue.data;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mValueBadgeView = (TextView) holder.findViewById(R.id.volume_boost_badge);
        mSummaryView = (TextView) holder.findViewById(android.R.id.summary);
        mIconView = (ImageView) holder.findViewById(android.R.id.icon);

        updateViews(getProgress());
    }

    @Override
    public void setProgress(int progress) {
        super.setProgress(progress);
        updateViews(progress);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        super.onProgressChanged(seekBar, progress, fromUser);
        updateViews(progress);

        if (fromUser) {
            // Apply live boost preview without latency
            VolumeBoostManager.getInstance(getContext()).setBoostLevel(progress);

            // Precision Material 3 Expressive haptic tick every 5% step or on bounds
            if (mLastHapticProgress == -1 || Math.abs(progress - mLastHapticProgress) >= 5
                    || progress == 0 || progress == 100) {
                mLastHapticProgress = progress;
                try {
                    seekBar.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK);
                } catch (Exception e) {
                    seekBar.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                }
            }
        }
    }

    private void updateViews(int progress) {
        int displayVolumePercent = 100 + Math.round((progress / 100.0f) * 100.0f); // 100% -> 200%
        float gainDb = (progress / 100.0f) * 10.0f; // 0 dB -> +10.0 dB

        if (mValueBadgeView != null) {
            mValueBadgeView.setText(String.format(Locale.getDefault(), "%d%%", displayVolumePercent));
        }

        String summaryText;
        if (progress == 0) {
            summaryText = getContext().getString(R.string.volume_boost_summary_normal);
        } else {
            summaryText = String.format(Locale.getDefault(),
                    getContext().getString(R.string.volume_boost_summary_format),
                    displayVolumePercent, gainDb);
        }

        if (mSummaryView != null) {
            mSummaryView.setText(summaryText);
        }
        setSummary(summaryText);

        if (mIconView != null) {
            if (progress > 0 && mAccentColor != 0) {
                mIconView.setImageTintList(ColorStateList.valueOf(mAccentColor));
            } else if (mNormalIconColor != 0) {
                mIconView.setImageTintList(ColorStateList.valueOf(mNormalIconColor));
            }
        }
    }
}
