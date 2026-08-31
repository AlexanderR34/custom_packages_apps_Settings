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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class CpuSparklineView extends View {

    private final Paint mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mPath = new Path();
    private final Path mFillPath = new Path();
    private final float[] mHistory = new float[25];
    private int mHead = 0;

    public CpuSparklineView(Context context) {
        this(context, null);
    }

    public CpuSparklineView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CpuSparklineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(3f);
        setLineColor(0xFF4CAF50); // Default green
    }

    public void setLineColor(int color) {
        mLinePaint.setColor(color);
        int alphaFill = (color & 0x00FFFFFF) | 0x25000000;
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setColor(alphaFill);
        invalidate();
    }

    public void addValue(float valRatio) {
        mHistory[mHead] = Math.max(0.04f, Math.min(1.0f, valRatio));
        mHead = (mHead + 1) % mHistory.length;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        mPath.reset();
        mFillPath.reset();

        float dx = (float) w / (mHistory.length - 1);
        mFillPath.moveTo(0, h);

        for (int i = 0; i < mHistory.length; i++) {
            int idx = (mHead + i) % mHistory.length;
            float val = mHistory[idx];
            float x = i * dx;
            float y = h - (val * (h - 6)) - 3;

            if (i == 0) {
                mPath.moveTo(x, y);
            } else {
                mPath.lineTo(x, y);
            }
            mFillPath.lineTo(x, y);
        }

        mFillPath.lineTo(w, h);
        mFillPath.close();

        canvas.drawPath(mFillPath, mFillPaint);
        canvas.drawPath(mPath, mLinePaint);
    }
}
