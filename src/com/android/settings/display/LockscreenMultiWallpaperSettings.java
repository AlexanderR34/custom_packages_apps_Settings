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

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings fragment for configuring Samsung-style Dynamic Lock Screen Wallpapers (Multi-photo rotation).
 */
public class LockscreenMultiWallpaperSettings extends SettingsPreferenceFragment {

    private static final String TAG = "LockscreenMultiWallpaperSettings";
    private static final int REQUEST_PICK_PHOTOS = 1001;

    private static final String KEY_ENABLE_SWITCH = "lockscreen_multi_wallpaper_enable";
    private static final String KEY_SELECT_PHOTOS = "lockscreen_multi_wallpaper_select";
    private static final String KEY_CLEAR_PHOTOS = "lockscreen_multi_wallpaper_clear";

    private SwitchPreferenceCompat mEnableSwitch;
    private Preference mSelectPhotosPref;
    private Preference mClearPhotosPref;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.lockscreen_multi_wallpaper_settings);

        mEnableSwitch = findPreference(KEY_ENABLE_SWITCH);
        mSelectPhotosPref = findPreference(KEY_SELECT_PHOTOS);
        mClearPhotosPref = findPreference(KEY_CLEAR_PHOTOS);

        if (mEnableSwitch != null) {
            boolean enabled = Settings.System.getInt(
                    getContentResolver(), Settings.System.LOCKSCREEN_MULTI_WALLPAPER_ENABLED, 0) == 1;
            mEnableSwitch.setChecked(enabled);
            mEnableSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean checked = (Boolean) newValue;
                Settings.System.putInt(
                        getContentResolver(), Settings.System.LOCKSCREEN_MULTI_WALLPAPER_ENABLED, checked ? 1 : 0);
                updateUiState();
                return true;
            });
        }

        if (mSelectPhotosPref != null) {
            mSelectPhotosPref.setOnPreferenceClickListener(preference -> {
                launchPhotoPicker();
                return true;
            });
        }

        if (mClearPhotosPref != null) {
            mClearPhotosPref.setOnPreferenceClickListener(preference -> {
                clearPhotos();
                return true;
            });
        }

        updateUiState();
    }

    private void launchPhotoPicker() {
        try {
            Intent intent = new Intent(android.provider.MediaStore.ACTION_PICK_IMAGES);
            intent.setType("image/*");
            intent.putExtra(android.provider.MediaStore.EXTRA_PICK_IMAGES_MAX, 25);
            startActivityForResult(intent, REQUEST_PICK_PHOTOS);
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, REQUEST_PICK_PHOTOS);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_PHOTOS && resultCode == Activity.RESULT_OK && data != null) {
            List<Uri> selectedUris = new ArrayList<>();
            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    selectedUris.add(clipData.getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                selectedUris.add(data.getData());
            }

            if (!selectedUris.isEmpty()) {
                saveSelectedPhotos(selectedUris);
            }
        }
    }

    private void saveSelectedPhotos(List<Uri> uris) {
        Context context = getContext();
        if (context == null) return;

        File wallpaperDir = new File(context.getFilesDir(), "lockscreen_wallpapers");
        if (wallpaperDir.exists()) {
            File[] files = wallpaperDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
        } else {
            wallpaperDir.mkdirs();
        }

        List<String> savedFilePaths = new ArrayList<>();
        int index = 0;

        for (Uri uri : uris) {
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    File outFile = new File(wallpaperDir, "lockscreen_photo_" + index + ".jpg");
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                    }
                    savedFilePaths.add(outFile.getAbsolutePath());
                    index++;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error copying image URI: " + uri, e);
            }
        }

        if (!savedFilePaths.isEmpty()) {
            String joinedPaths = TextUtils.join(";", savedFilePaths);
            Settings.System.putString(
                    getContentResolver(), Settings.System.LOCKSCREEN_MULTI_WALLPAPER_FILES, joinedPaths);
            Settings.System.putInt(
                    getContentResolver(), Settings.System.LOCKSCREEN_MULTI_WALLPAPER_INDEX, 0);

            Toast.makeText(context,
                    getString(R.string.lockscreen_multi_wallpaper_saved_toast, savedFilePaths.size()),
                    Toast.LENGTH_SHORT).show();
        }

        updateUiState();
    }

    private void clearPhotos() {
        Context context = getContext();
        if (context == null) return;

        File wallpaperDir = new File(context.getFilesDir(), "lockscreen_wallpapers");
        if (wallpaperDir.exists()) {
            File[] files = wallpaperDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
        }

        Settings.System.putString(getContentResolver(), Settings.System.LOCKSCREEN_MULTI_WALLPAPER_FILES, "");
        Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_MULTI_WALLPAPER_INDEX, 0);

        Toast.makeText(context, getString(R.string.lockscreen_multi_wallpaper_cleared_toast), Toast.LENGTH_SHORT).show();
        updateUiState();
    }

    private void updateUiState() {
        String paths = Settings.System.getString(
                getContentResolver(), Settings.System.LOCKSCREEN_MULTI_WALLPAPER_FILES);
        int photoCount = 0;
        if (!TextUtils.isEmpty(paths)) {
            photoCount = paths.split(";").length;
        }

        if (mSelectPhotosPref != null) {
            mSelectPhotosPref.setSummary(photoCount > 0
                    ? getString(R.string.lockscreen_multi_wallpaper_selected_count, photoCount)
                    : getString(R.string.lockscreen_multi_wallpaper_select_photos_summary));
        }

        if (mClearPhotosPref != null) {
            mClearPhotosPref.setEnabled(photoCount > 0);
        }
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }
}
