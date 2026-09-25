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

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manager class for Dynamic Wallpapers.
 * Manages local persistent image storage, index cycling, and wallpaper application.
 */
public class DynamicWallpaperManager {
    private static final String TAG = "DynamicWallpaperManager";
    private static final String PREF_NAME = "dynamic_wallpaper_prefs";
    private static final String KEY_CURRENT_INDEX = "current_index";
    private static final String WALLPAPER_DIR = "dynamic_wallpapers";

    public static final String KEY_ENABLED = "dynamic_wallpaper_enabled";
    public static final String KEY_TARGET = "dynamic_wallpaper_target";
    public static final String KEY_FREEZE_MONET = "dynamic_wallpaper_freeze_monet";

    public static final int TARGET_HOME = 1;
    public static final int TARGET_LOCK = 2;
    public static final int TARGET_BOTH = 3;

    private static DynamicWallpaperManager sInstance;
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final File mStorageDir;
    private final java.util.concurrent.ExecutorService mExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

    private DynamicWallpaperManager(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        mStorageDir = new File(mContext.getFilesDir(), WALLPAPER_DIR);
        if (!mStorageDir.exists()) {
            mStorageDir.mkdirs();
        }
    }

    public void applyNextWallpaperAsync() {
        mExecutor.execute(this::applyNextWallpaper);
    }

    public static synchronized DynamicWallpaperManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DynamicWallpaperManager(context);
        }
        return sInstance;
    }

    public boolean isEnabled() {
        return Settings.System.getIntForUser(
                mContext.getContentResolver(),
                KEY_ENABLED, 0, UserHandle.USER_CURRENT) == 1;
    }

    public void setEnabled(boolean enabled) {
        Settings.System.putIntForUser(
                mContext.getContentResolver(),
                KEY_ENABLED, enabled ? 1 : 0, UserHandle.USER_CURRENT);

        Intent intent = new Intent(mContext, DynamicWallpaperService.class);
        if (enabled) {
            mContext.startService(intent);
        } else {
            mContext.stopService(intent);
        }
    }

    public int getTargetDestination() {
        return Settings.System.getIntForUser(
                mContext.getContentResolver(),
                KEY_TARGET, TARGET_BOTH, UserHandle.USER_CURRENT);
    }

    public void setTargetDestination(int target) {
        Settings.System.putIntForUser(
                mContext.getContentResolver(),
                KEY_TARGET, target, UserHandle.USER_CURRENT);
    }





    public boolean isFreezeMonetEnabled() {
        return Settings.System.getIntForUser(
                mContext.getContentResolver(),
                KEY_FREEZE_MONET, 1, UserHandle.USER_CURRENT) == 1;
    }

    public void setFreezeMonetEnabled(boolean enabled) {
        Settings.System.putIntForUser(
                mContext.getContentResolver(),
                KEY_FREEZE_MONET, enabled ? 1 : 0, UserHandle.USER_CURRENT);
    }

    public synchronized List<File> getImageFiles() {
        List<File> list = new ArrayList<>();
        if (mStorageDir.exists() && mStorageDir.isDirectory()) {
            File[] files = mStorageDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && (f.getName().endsWith(".jpg") || f.getName().endsWith(".png") || f.getName().endsWith(".webp"))) {
                        list.add(f);
                    }
                }
                Collections.sort(list, (f1, f2) -> f1.getName().compareTo(f2.getName()));
            }
        }
        return list;
    }

    public synchronized int importImages(List<Uri> uris) {
        int importedCount = 0;
        long timestamp = System.currentTimeMillis();
        for (int i = 0; i < uris.size(); i++) {
            Uri uri = uris.get(i);
            try (InputStream in = mContext.getContentResolver().openInputStream(uri)) {
                if (in == null) continue;
                File dest = new File(mStorageDir, "wallpaper_" + timestamp + "_" + i + ".jpg");
                try (FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buffer = new byte[16384];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                    importedCount++;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error importing image from uri: " + uri, e);
            }
        }
        return importedCount;
    }

    public synchronized void clearAllImages() {
        if (mStorageDir.exists()) {
            File[] files = mStorageDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        }
        mPrefs.edit().putInt(KEY_CURRENT_INDEX, 0).apply();
    }

    public synchronized File getNextImageFile() {
        List<File> images = getImageFiles();
        if (images.isEmpty()) {
            return null;
        }
        int currentIndex = mPrefs.getInt(KEY_CURRENT_INDEX, 0);
        if (currentIndex >= images.size()) {
            currentIndex = 0;
        }
        File currentFile = images.get(currentIndex);
        int nextIndex = (currentIndex + 1) % images.size();
        mPrefs.edit().putInt(KEY_CURRENT_INDEX, nextIndex).apply();
        return currentFile;
    }

    public synchronized boolean applyNextWallpaper() {
        File file = getNextImageFile();
        if (file == null || !file.exists()) {
            Log.w(TAG, "No image files available to apply");
            return false;
        }

        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from " + file.getAbsolutePath());
                return false;
            }

            // Always scale and center-crop to device's full display dimensions
            Point displaySize = getDisplaySize();
            Bitmap scaledBitmap = createCenterCropBitmap(bitmap, displaySize.x, displaySize.y);
            if (scaledBitmap != bitmap) {
                bitmap.recycle();
            }

            WallpaperManager wm = WallpaperManager.getInstance(mContext);
            int target = getTargetDestination();
            int which = 0;
            switch (target) {
                case TARGET_HOME:
                    which = WallpaperManager.FLAG_SYSTEM;
                    break;
                case TARGET_LOCK:
                    which = WallpaperManager.FLAG_LOCK;
                    break;
                case TARGET_BOTH:
                default:
                    which = WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK;
                    break;
            }

            wm.setBitmap(scaledBitmap, null, true, which);
            scaledBitmap.recycle();
            Log.i(TAG, "Dynamic wallpaper applied successfully (" + displaySize.x + "x" + displaySize.y + ") from " + file.getName() + " to flags=" + which);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to set wallpaper: " + e.getMessage(), e);
            return false;
        }
    }

    public Point getDisplaySize() {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        if (wm != null) {
            try {
                android.view.WindowMetrics metrics = wm.getMaximumWindowMetrics();
                Rect bounds = metrics.getBounds();
                size.x = bounds.width();
                size.y = bounds.height();
            } catch (Throwable t) {
                DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
                size.x = dm.widthPixels;
                size.y = dm.heightPixels;
            }
        } else {
            DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
            size.x = dm.widthPixels;
            size.y = dm.heightPixels;
        }
        return size;
    }

    public static Bitmap createCenterCropBitmap(Bitmap src, int targetWidth, int targetHeight) {
        if (src == null || targetWidth <= 0 || targetHeight <= 0) return src;
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        if (srcWidth == targetWidth && srcHeight == targetHeight) {
            return src;
        }

        float scale;
        float dx = 0;
        float dy = 0;

        if (srcWidth * targetHeight > targetWidth * srcHeight) {
            scale = (float) targetHeight / (float) srcHeight;
            dx = (targetWidth - srcWidth * scale) * 0.5f;
        } else {
            scale = (float) targetWidth / (float) srcWidth;
            dy = (targetHeight - srcHeight * scale) * 0.5f;
        }

        Bitmap output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate(Math.round(dx), Math.round(dy));

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(src, matrix, paint);
        return output;
    }
}
