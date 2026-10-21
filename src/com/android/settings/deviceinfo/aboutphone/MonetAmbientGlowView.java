/*
 * Copyright (C) 2026 Project Diva
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

package com.android.settings.deviceinfo.aboutphone;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class MonetAmbientGlowView extends View {

    private final Paint mOrb1Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mOrb2Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mOrb3Paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int mColorPrimary;
    private int mColorSecondary;
    private int mColorTertiary;

    private float mProgress = 0f;
    private ValueAnimator mAnimator;

    public MonetAmbientGlowView(Context context) {
        super(context);
        init();
    }

    public MonetAmbientGlowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MonetAmbientGlowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        updateMonetColors();

        mAnimator = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
        mAnimator.setDuration(14000L); // 14 seconds per full cycle
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setRepeatMode(ValueAnimator.RESTART);
        mAnimator.addUpdateListener(anim -> {
            mProgress = (float) anim.getAnimatedValue();
            invalidate();
        });
    }

    public void updateMonetColors() {
        final Context context = getContext();
        final boolean isNight = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        if (isNight) {
            try {
                mColorPrimary = context.getColor(android.R.color.system_accent1_700);
            } catch (Exception e) {
                mColorPrimary = 0xFF1A365D;
            }
            try {
                mColorSecondary = context.getColor(android.R.color.system_accent2_700);
            } catch (Exception e) {
                mColorSecondary = 0xFF4A154B;
            }
            try {
                mColorTertiary = context.getColor(android.R.color.system_accent3_700);
            } catch (Exception e) {
                mColorTertiary = 0xFF5C3C10;
            }
        } else {
            try {
                mColorPrimary = context.getColor(android.R.color.system_accent1_100);
            } catch (Exception e) {
                mColorPrimary = 0xFFE0EDFE;
            }
            try {
                mColorSecondary = context.getColor(android.R.color.system_accent2_100);
            } catch (Exception e) {
                mColorSecondary = 0xFFFBE4F8;
            }
            try {
                mColorTertiary = context.getColor(android.R.color.system_accent3_100);
            } catch (Exception e) {
                mColorTertiary = 0xFFFEF3D6;
            }
        }
    }

    public void startGlowAnimation() {
        if (mAnimator != null && !mAnimator.isRunning()) {
            mAnimator.start();
        }
    }

    public void stopGlowAnimation() {
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startGlowAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopGlowAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int w = getWidth();
        final int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        // Removed canvas.drawColor(mBgColor) to avoid covering the menu options with black!

        final float radius = Math.max(w, h) * 0.40f;

        // 1. Point 1 (Monet Primary) - Floating orbital path in upper section
        final float cx1 = w * 0.5f + (float) Math.cos(mProgress) * (w * 0.28f);
        final float cy1 = h * 0.22f + (float) Math.sin(mProgress * 0.8f) * (h * 0.12f);

        final int primaryAlpha = Color.argb(210, Color.red(mColorPrimary),
                Color.green(mColorPrimary), Color.blue(mColorPrimary));
        final int primaryTransparent = Color.argb(0, Color.red(mColorPrimary),
                Color.green(mColorPrimary), Color.blue(mColorPrimary));
        final RadialGradient grad1 = new RadialGradient(
                cx1, cy1, radius,
                primaryAlpha, primaryTransparent,
                Shader.TileMode.CLAMP
        );
        mOrb1Paint.setShader(grad1);
        canvas.drawCircle(cx1, cy1, radius, mOrb1Paint);

        // 2. Point 2 (Monet Secondary) - Counter-orbital dance
        final float cx2 = w * 0.5f + (float) Math.cos(mProgress * 0.7f + Math.PI) * (w * 0.26f);
        final float cy2 = h * 0.35f + (float) Math.sin(mProgress * 1.1f + 1.2f) * (h * 0.14f);

        final int secondaryAlpha = Color.argb(190, Color.red(mColorSecondary),
                Color.green(mColorSecondary), Color.blue(mColorSecondary));
        final int secondaryTransparent = Color.argb(0, Color.red(mColorSecondary),
                Color.green(mColorSecondary), Color.blue(mColorSecondary));
        final RadialGradient grad2 = new RadialGradient(
                cx2, cy2, radius,
                secondaryAlpha, secondaryTransparent,
                Shader.TileMode.CLAMP
        );
        mOrb2Paint.setShader(grad2);
        canvas.drawCircle(cx2, cy2, radius, mOrb2Paint);

        // 3. Point 3 (Monet Tertiary) - Organic random drifting movement
        final float cx3 = w * 0.5f + (float) (Math.sin(mProgress * 1.3f + 2.5f) * Math.cos(mProgress * 0.7f)) * (w * 0.35f);
        final float cy3 = h * 0.48f + (float) (Math.cos(mProgress * 0.9f + 1.8f) * Math.sin(mProgress * 1.5f)) * (h * 0.20f);

        final int tertiaryAlpha = Color.argb(180, Color.red(mColorTertiary),
                Color.green(mColorTertiary), Color.blue(mColorTertiary));
        final int tertiaryTransparent = Color.argb(0, Color.red(mColorTertiary),
                Color.green(mColorTertiary), Color.blue(mColorTertiary));
        final RadialGradient grad3 = new RadialGradient(
                cx3, cy3, radius,
                tertiaryAlpha, tertiaryTransparent,
                Shader.TileMode.CLAMP
        );
        mOrb3Paint.setShader(grad3);
        canvas.drawCircle(cx3, cy3, radius, mOrb3Paint);
    }
}
