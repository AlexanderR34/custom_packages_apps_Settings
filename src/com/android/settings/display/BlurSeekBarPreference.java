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

package com.android.settings.display;

import android.content.Context;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.widget.SeekBarPreference;

/**
 * SeekBarPreference for System Background Blur Intensity control with smooth updates and haptic feedback.
 */
public class BlurSeekBarPreference extends SeekBarPreference {

    private TextView mValueTextView;
    private int mLastProgress = -1;

    public BlurSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public BlurSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public BlurSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BlurSeekBarPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setMin(0);
        setMax(100);
        setContinuousUpdates(true);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mValueTextView = (TextView) holder.findViewById(android.R.id.summary);
        updateValueText(getProgress());
    }

    @Override
    public void setProgress(int progress) {
        super.setProgress(progress);
        updateValueText(progress);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        super.onProgressChanged(seekBar, progress, fromUser);
        updateValueText(progress);
        if (fromUser && Math.abs(progress - mLastProgress) >= 5) {
            mLastProgress = progress;
            seekBar.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
    }

    private void updateValueText(int progress) {
        String text = progress + "%";
        if (mValueTextView != null) {
            mValueTextView.setText(text);
        }
        setSummary(getContext().getString(R.string.blur_intensity_summary_format, progress));
    }
}
