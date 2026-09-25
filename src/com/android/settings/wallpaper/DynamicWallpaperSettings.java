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

package com.android.settings.wallpaper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.LayoutPreference;

import java.io.File;
import java.util.List;

@SearchIndexable
public class DynamicWallpaperSettings extends DashboardFragment implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "DynamicWallpaperSettings";
    private static final String PREF_PREVIEW = "dynamic_wallpaper_preview_layout";
    private static final String PREF_TARGET = "dynamic_wallpaper_target";
    private static final String PREF_FREEZE_MONET = "dynamic_wallpaper_freeze_monet";

    private DynamicWallpaperManager mManager;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final java.util.Random mRandom = new java.util.Random();

    private ImageView mImageFront;
    private ImageView mImageBack;
    private LinearLayout mEmptyContainer;
    private TextView mBadgeCounter;
    private Button mBtnSelectImages;
    private Button mBtnClearImages;

    private DropDownPreference mTargetPref;
    private SwitchPreferenceCompat mFreezeMonetPref;

    private int mPreviewIndex = 0;
    private boolean mIsPreviewAnimating = false;

    private final Runnable mCarouselRunnable = new Runnable() {
        @Override
        public void run() {
            advancePreviewCarousel();
            mHandler.postDelayed(this, 3000L);
        }
    };

    private ActivityResultLauncher<String> mImagePickerLauncher;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mManager = DynamicWallpaperManager.getInstance(context);
        mImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        Toast.makeText(context, "Importando " + uris.size() + " imágenes...", Toast.LENGTH_SHORT).show();
                        new Thread(() -> {
                            int imported = mManager.importImages(uris);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(context, getString(R.string.dynamic_wallpaper_photos_count, imported), Toast.LENGTH_SHORT).show();
                                    updatePreviewUi();
                                    showTargetSelectionDialogIfNeeded();
                                });
                            }
                        }).start();
                    }
                });
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.dynamic_wallpaper_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WALLPAPER_TYPE;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupPreferences();
        setupPreviewCard();
    }

    private void setupPreferences() {
        mTargetPref = findPreference(PREF_TARGET);
        if (mTargetPref != null) {
            mTargetPref.setValue(String.valueOf(mManager.getTargetDestination()));
            mTargetPref.setOnPreferenceChangeListener(this);
        }

        mFreezeMonetPref = findPreference(PREF_FREEZE_MONET);
        if (mFreezeMonetPref != null) {
            mFreezeMonetPref.setChecked(mManager.isFreezeMonetEnabled());
            mFreezeMonetPref.setOnPreferenceChangeListener(this);
        }
    }

    private void setupPreviewCard() {
        LayoutPreference layoutPref = findPreference(PREF_PREVIEW);
        if (layoutPref == null) return;

        View root = layoutPref.findViewById(R.id.preview_card_frame);
        if (root == null) return;
        root.setClipToOutline(true);

        View innerContainer = layoutPref.findViewById(R.id.preview_inner_container);
        if (innerContainer != null) {
            innerContainer.setClipToOutline(true);
        }

        mImageFront = layoutPref.findViewById(R.id.preview_image_front);
        mImageBack = layoutPref.findViewById(R.id.preview_image_back);
        mEmptyContainer = layoutPref.findViewById(R.id.preview_empty_container);
        mBadgeCounter = layoutPref.findViewById(R.id.preview_badge_counter);
        mBtnSelectImages = layoutPref.findViewById(R.id.btn_select_images);
        mBtnClearImages = layoutPref.findViewById(R.id.btn_clear_images);

        if (mBtnSelectImages != null) {
            mBtnSelectImages.setOnClickListener(v -> mImagePickerLauncher.launch("image/*"));
        }

        if (mBtnClearImages != null) {
            mBtnClearImages.setOnClickListener(v -> {
                new Thread(() -> {
                    mManager.clearAllImages();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            updatePreviewUi();
                            Toast.makeText(getContext(), R.string.dynamic_wallpaper_no_photos_selected, Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            });
        }

        updatePreviewUi();
    }

    private void updatePreviewUi() {
        List<File> images = mManager.getImageFiles();
        if (images.isEmpty()) {
            if (mEmptyContainer != null) mEmptyContainer.setVisibility(View.VISIBLE);
            if (mBadgeCounter != null) mBadgeCounter.setVisibility(View.GONE);
            if (mBtnClearImages != null) mBtnClearImages.setVisibility(View.GONE);
            if (mImageFront != null) mImageFront.setImageBitmap(null);
            if (mImageBack != null) mImageBack.setImageBitmap(null);
            mHandler.removeCallbacks(mCarouselRunnable);
        } else {
            if (mEmptyContainer != null) mEmptyContainer.setVisibility(View.GONE);
            if (mBadgeCounter != null) {
                mBadgeCounter.setVisibility(View.VISIBLE);
                mBadgeCounter.setText("1 / " + images.size());
            }
            if (mBtnClearImages != null) mBtnClearImages.setVisibility(View.VISIBLE);

            mPreviewIndex = 0;
            loadBitmapIntoView(images.get(0), mImageFront);

            mHandler.removeCallbacks(mCarouselRunnable);
            if (images.size() > 1) {
                mHandler.postDelayed(mCarouselRunnable, 3000L);
            }
        }
    }

    private void advancePreviewCarousel() {
        if (mIsPreviewAnimating || mImageFront == null || mImageBack == null) return;
        List<File> images = mManager.getImageFiles();
        if (images.size() <= 1) return;

        int nextIdx = (mPreviewIndex + 1) % images.size();
        File nextFile = images.get(nextIdx);
        Bitmap nextBmp = decodeSampledBitmap(nextFile);
        if (nextBmp == null) return;

        mIsPreviewAnimating = true;
        mImageBack.setImageBitmap(nextBmp);
        mImageBack.setVisibility(View.VISIBLE);
        mImageBack.setAlpha(0.0f);

        ObjectAnimator fadeOutFront = ObjectAnimator.ofFloat(mImageFront, "alpha", 1f, 0f);
        ObjectAnimator fadeInBack = ObjectAnimator.ofFloat(mImageBack, "alpha", 0f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeOutFront, fadeInBack);
        set.setDuration(300);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                finalizePreviewSwitch(nextBmp, nextIdx, images.size());
            }
        });
        set.start();
    }

    private void finalizePreviewSwitch(Bitmap nextBmp, int nextIdx, int totalCount) {
        mImageFront.setImageBitmap(nextBmp);
        mImageFront.setTranslationX(0f);
        mImageFront.setTranslationY(0f);
        mImageFront.setScaleX(1f);
        mImageFront.setScaleY(1f);
        mImageFront.setRotationY(0f);
        mImageFront.setAlpha(1f);

        mImageBack.setVisibility(View.GONE);
        mImageBack.setImageBitmap(null);
        mImageBack.setTranslationX(0f);
        mImageBack.setTranslationY(0f);
        mImageBack.setScaleX(1f);
        mImageBack.setScaleY(1f);
        mImageBack.setRotationY(0f);
        mImageBack.setAlpha(1f);

        mPreviewIndex = nextIdx;
        if (mBadgeCounter != null) {
            mBadgeCounter.setText((mPreviewIndex + 1) + " / " + totalCount);
        }
        mIsPreviewAnimating = false;
    }

    private void loadBitmapIntoView(File file, ImageView targetView) {
        if (file == null || targetView == null) return;
        Bitmap bmp = decodeSampledBitmap(file);
        if (bmp != null) {
            targetView.setImageBitmap(bmp);
        }
    }

    private Bitmap decodeSampledBitmap(File file) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), opts);

            int targetW = 400;
            int targetH = 800;
            int inSampleSize = 1;
            if (opts.outHeight > targetH || opts.outWidth > targetW) {
                final int halfHeight = opts.outHeight / 2;
                final int halfWidth = opts.outWidth / 2;
                while ((halfHeight / inSampleSize) >= targetH && (halfWidth / inSampleSize) >= targetW) {
                    inSampleSize *= 2;
                }
            }
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = Math.max(2, inSampleSize);
            return BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        } catch (Exception e) {
            return null;
        }
    }

    private void showTargetSelectionDialogIfNeeded() {
        Context context = getContext();
        if (context == null) return;

        CharSequence[] items = new CharSequence[] {
                getString(R.string.dynamic_wallpaper_target_home),
                getString(R.string.dynamic_wallpaper_target_lock),
                getString(R.string.dynamic_wallpaper_target_both)
        };

        new AlertDialog.Builder(context)
                .setTitle(R.string.dynamic_wallpaper_dialog_apply_title)
                .setSingleChoiceItems(items, mManager.getTargetDestination() - 1, (dialog, which) -> {
                    int target = which + 1;
                    mManager.setTargetDestination(target);
                    if (mTargetPref != null) {
                        mTargetPref.setValue(String.valueOf(target));
                    }
                    dialog.dismiss();
                    // If enabled, immediately apply first wallpaper to the target destination in background
                    if (mManager.isEnabled()) {
                        mManager.applyNextWallpaperAsync();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (PREF_TARGET.equals(key)) {
            int target = Integer.parseInt((String) newValue);
            mManager.setTargetDestination(target);
            return true;
        } else if (PREF_FREEZE_MONET.equals(key)) {
            boolean enabled = (Boolean) newValue;
            mManager.setFreezeMonetEnabled(enabled);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreviewUi();
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mCarouselRunnable);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.dynamic_wallpaper_settings);
}
