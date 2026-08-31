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
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.FileUtils;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Controller to pick and apply custom TTF/OTF fonts across the system.
 */
public class CustomFontPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "CustomFontController";
    public static final int REQUEST_CODE_PICK_FONT = 1042;
    private static final String FONT_FILE_PATH = "/data/system/theme/custom_font.ttf";
    private static final String SETTING_KEY_FONT_NAME = "custom_font_name";

    private Preference mPreference;

    public CustomFontPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreference.setOnPreferenceClickListener(this);
            updateSummary();
        }
    }

    @Override
    public CharSequence getSummary() {
        String savedFontName = Settings.System.getString(
                mContext.getContentResolver(), SETTING_KEY_FONT_NAME);
        File fontFile = new File(FONT_FILE_PATH);
        if (fontFile.exists() && !TextUtils.isEmpty(savedFontName)) {
            return mContext.getString(R.string.custom_font_summary_active, savedFontName);
        }
        return mContext.getString(R.string.custom_font_summary_default);
    }

    private void updateSummary() {
        if (mPreference != null) {
            mPreference.setSummary(getSummary());
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        File fontFile = new File(FONT_FILE_PATH);
        if (fontFile.exists()) {
            new AlertDialog.Builder(mContext)
                    .setTitle(R.string.custom_font_dialog_title)
                    .setItems(new CharSequence[]{
                            mContext.getString(R.string.custom_font_option_change),
                            mContext.getString(R.string.custom_font_option_reset)
                    }, (dialog, which) -> {
                        if (which == 0) {
                            launchFontPicker();
                        } else {
                            resetToDefaultFont();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            launchFontPicker();
        }
        return true;
    }

    private Activity getActivity() {
        Context context = mContext;
        while (context instanceof android.content.ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((android.content.ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    public void launchFontPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"font/ttf", "font/otf", "application/x-font-ttf",
                "application/x-font-otf", "application/octet-stream"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        Activity activity = getActivity();
        if (activity != null) {
            activity.startActivityForResult(intent, REQUEST_CODE_PICK_FONT);
        } else if (mPreference != null && mPreference.getContext() instanceof Activity) {
            ((Activity) mPreference.getContext()).startActivityForResult(intent, REQUEST_CODE_PICK_FONT);
        }
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_FONT && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                applyCustomFontFromUri(uri);
                return true;
            }
        }
        return false;
    }

    private void applyCustomFontFromUri(Uri uri) {
        try {
            String fontName = getFileNameFromUri(uri);
            File fontFile = new File(FONT_FILE_PATH);
            File parentDir = fontFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
                parentDir.setExecutable(true, false);
                parentDir.setReadable(true, false);
            }

            try (InputStream in = mContext.getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(fontFile)) {
                FileUtils.copy(in, out);
            }

            fontFile.setReadable(true, false);

            Settings.System.putString(mContext.getContentResolver(), SETTING_KEY_FONT_NAME, fontName);
            Settings.System.putLong(mContext.getContentResolver(), "custom_font_updated", System.currentTimeMillis());

            updateSummary();
            Toast.makeText(mContext, R.string.custom_font_applied_toast, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error applying custom font", e);
            Toast.makeText(mContext, R.string.custom_font_error_toast, Toast.LENGTH_SHORT).show();
        }
    }

    private void resetToDefaultFont() {
        try {
            File fontFile = new File(FONT_FILE_PATH);
            if (fontFile.exists()) {
                fontFile.delete();
            }
            Settings.System.putString(mContext.getContentResolver(), SETTING_KEY_FONT_NAME, null);
            Settings.System.putLong(mContext.getContentResolver(), "custom_font_updated", System.currentTimeMillis());
            updateSummary();
            Toast.makeText(mContext, R.string.custom_font_reset_toast, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error resetting custom font", e);
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
