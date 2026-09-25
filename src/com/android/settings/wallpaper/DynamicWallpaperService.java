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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

/**
 * Background service that monitors screen power events (screen off / screen on)
 * to automatically cycle through dynamic wallpapers when the screen is turned off/on.
 */
public class DynamicWallpaperService extends Service {
    private static final String TAG = "DynamicWallpaperService";

    private DynamicWallpaperManager mManager;

    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                if (mManager != null && mManager.isEnabled()) {
                    // Rotate immediately on screen off so when device wakes up, the next wallpaper is already applied
                    mManager.applyNextWallpaperAsync();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mManager = DynamicWallpaperManager.getInstance(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenReceiver, filter);

        Log.i(TAG, "DynamicWallpaperService started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mScreenReceiver);
        } catch (Exception ignored) {}
        Log.i(TAG, "DynamicWallpaperService destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
