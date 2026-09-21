/*
 * Copyright (C) 2026 The Project Diva
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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.settings.R;

import java.io.File;

public class CustomSoundItemPreference extends Preference {

    private static final String TAG = "CustomSoundItemPref";
    private static MediaPlayer sMediaPlayer = null;

    private String mSoundType = "notification";
    private String mSoundPath = "";
    private String mSoundTitle = "";

    public CustomSoundItemPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public CustomSoundItemPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public CustomSoundItemPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomSoundItemPreference(Context context) {
        super(context);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomSoundItemPreference);
            mSoundType = a.getString(R.styleable.CustomSoundItemPreference_soundType);
            if (mSoundType == null) {
                mSoundType = "notification";
            }
            mSoundPath = a.getString(R.styleable.CustomSoundItemPreference_soundPath);
            mSoundTitle = a.getString(R.styleable.CustomSoundItemPreference_soundTitle);
            a.recycle();
        }
    }

    @Override
    protected void onClick() {
        super.onClick();
        applyAndPreviewSound();
    }

    private void applyAndPreviewSound() {
        if (mSoundPath == null || mSoundPath.isEmpty()) {
            return;
        }

        File soundFile = new File(mSoundPath);
        if (!soundFile.exists()) {
            Log.e(TAG, "Sound file does not exist: " + mSoundPath);
            return;
        }

        int ringtoneType = "ringtone".equalsIgnoreCase(mSoundType)
                ? RingtoneManager.TYPE_RINGTONE
                : RingtoneManager.TYPE_NOTIFICATION;

        Uri soundContentUri = getContentUriForPath(getContext(), soundFile, ringtoneType);
        if (soundContentUri == null) {
            soundContentUri = Uri.fromFile(soundFile);
        }

        playPreview(soundContentUri);

        try {
            RingtoneManager.setActualDefaultRingtoneUri(getContext(), ringtoneType, soundContentUri);

            // Also guarantee setting in Settings.System
            String settingKey = (ringtoneType == RingtoneManager.TYPE_RINGTONE)
                    ? Settings.System.RINGTONE
                    : Settings.System.NOTIFICATION_SOUND;
            Settings.System.putString(getContext().getContentResolver(), settingKey, soundContentUri.toString());

            String titleDisplay = (mSoundTitle != null && !mSoundTitle.isEmpty())
                    ? mSoundTitle
                    : (getTitle() != null ? getTitle().toString() : soundFile.getName());

            String typeStr = "ringtone".equalsIgnoreCase(mSoundType)
                    ? getContext().getString(R.string.sound_type_ringtone)
                    : getContext().getString(R.string.sound_type_notification);

            Toast.makeText(getContext(),
                    getContext().getString(R.string.sound_applied_toast, titleDisplay, typeStr),
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to set default sound URI: " + soundContentUri, e);
        }
    }

    private Uri getContentUriForPath(Context context, File file, int ringtoneType) {
        if (!file.exists()) {
            return null;
        }
        ContentResolver resolver = context.getContentResolver();
        Uri internalUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;

        // 1. Query by absolute path in DATA
        try (Cursor cursor = resolver.query(
                internalUri,
                new String[]{MediaStore.Audio.Media._ID},
                MediaStore.Audio.Media.DATA + "=?",
                new String[]{file.getAbsolutePath()},
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                return ContentUris.withAppendedId(internalUri, id);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error querying MediaStore by DATA for " + file.getAbsolutePath(), e);
        }

        // 2. Query by file name / display name
        try (Cursor cursor = resolver.query(
                internalUri,
                new String[]{MediaStore.Audio.Media._ID},
                MediaStore.Audio.Media.DISPLAY_NAME + "=?",
                new String[]{file.getName()},
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                return ContentUris.withAppendedId(internalUri, id);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error querying MediaStore by DISPLAY_NAME for " + file.getName(), e);
        }

        // 3. Insert into MediaStore to produce a valid content:// URI
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.DATA, file.getAbsolutePath());
            values.put(MediaStore.Audio.Media.TITLE, (mSoundTitle != null && !mSoundTitle.isEmpty()) ? mSoundTitle : file.getName());
            values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/ogg");
            values.put(MediaStore.Audio.Media.IS_RINGTONE, ringtoneType == RingtoneManager.TYPE_RINGTONE ? 1 : 0);
            values.put(MediaStore.Audio.Media.IS_NOTIFICATION, ringtoneType == RingtoneManager.TYPE_NOTIFICATION ? 1 : 0);
            values.put(MediaStore.Audio.Media.IS_ALARM, 0);
            values.put(MediaStore.Audio.Media.IS_MUSIC, 0);
            return resolver.insert(internalUri, values);
        } catch (Exception e) {
            Log.e(TAG, "Failed to insert custom sound into MediaStore: " + file.getAbsolutePath(), e);
        }

        return null;
    }

    private void playPreview(Uri uri) {
        stopCurrentPlayback();

        try {
            sMediaPlayer = new MediaPlayer();
            int usage = "ringtone".equalsIgnoreCase(mSoundType)
                    ? AudioAttributes.USAGE_NOTIFICATION_RINGTONE
                    : AudioAttributes.USAGE_NOTIFICATION;

            sMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            sMediaPlayer.setDataSource(getContext(), uri);
            sMediaPlayer.setOnCompletionListener(mp -> stopCurrentPlayback());
            sMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                stopCurrentPlayback();
                return true;
            });
            sMediaPlayer.prepare();
            sMediaPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Error playing preview sound for URI: " + uri, e);
            stopCurrentPlayback();
        }
    }

    public static void stopCurrentPlayback() {
        if (sMediaPlayer != null) {
            try {
                if (sMediaPlayer.isPlaying()) {
                    sMediaPlayer.stop();
                }
                sMediaPlayer.release();
            } catch (Exception ignored) {
            } finally {
                sMediaPlayer = null;
            }
        }
    }
}
