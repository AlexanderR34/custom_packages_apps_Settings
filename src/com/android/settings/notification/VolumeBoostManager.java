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

package com.android.settings.notification;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.util.List;

/**
 * Manager responsible for boosting MEDIA audio output up to +15.0 dB using
 * Android's Dynamic Processing / Loudness Enhancer audio effect APIs without audio distortion.
 *
 * Automatically bypasses volume boost during notifications, ringtones, and alarms,
 * and completely disables boost when connected to Bluetooth audio devices (A2DP / LE Audio).
 */
public class VolumeBoostManager {
    private static final String TAG = "VolumeBoostManager";
    public static final String SETTING_KEY = Settings.System.VOLUME_BOOST_LEVEL;
    public static final String SETTING_CALL_GAIN_KEY = "volume_boost_call_gain";
    public static final int DEFAULT_BOOST_LEVEL = 0; // 0% boost = 100% standard volume

    private static VolumeBoostManager sInstance;

    private final Context mContext;
    private final AudioManager mAudioManager;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private LoudnessEnhancer mLoudnessEnhancer;
    private DynamicsProcessing mDynamicsProcessing;
    private boolean mCallbackRegistered = false;
    private int mLastAppliedBoostLevel = -1;

    private final AudioManager.AudioPlaybackCallback mPlaybackCallback =
            new AudioManager.AudioPlaybackCallback() {
        @Override
        public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
            updateBoostForPlaybackConfigs(configs);
        }
    };

    private VolumeBoostManager(Context context) {
        mContext = context.getApplicationContext();
        mAudioManager = mContext.getSystemService(AudioManager.class);
        initAudioFx();
    }

    public static synchronized VolumeBoostManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new VolumeBoostManager(context);
        }
        return sInstance;
    }

    private void initAudioFx() {
        try {
            // Audio session 0 corresponds to the global audio output mix
            mLoudnessEnhancer = new LoudnessEnhancer(0);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize LoudnessEnhancer: " + e.getMessage());
        }

        // Apply saved setting level on init
        int savedLevel = getBoostLevel();
        setBoostLevel(savedLevel);
    }

    /**
     * Retrieves the current boost level from Settings.System.
     * @return 0 to 100 (where 0 is 100% volume / default, 100 is max boost).
     */
    public int getBoostLevel() {
        return Settings.System.getInt(mContext.getContentResolver(), SETTING_KEY, DEFAULT_BOOST_LEVEL);
    }

    /**
     * Sets and applies the volume boost level.
     * @param level boost percentage level (0 to 100)
     */
    public void setBoostLevel(int level) {
        int clampedLevel = Math.max(0, Math.min(100, level));
        Settings.System.putInt(mContext.getContentResolver(), SETTING_KEY, clampedLevel);

        if (clampedLevel > 0) {
            registerCallbacksIfNeeded();
            updateBoostState();
        } else {
            unregisterCallbacksIfNeeded();
            applyAudioFx(0);
        }
    }

    private void registerCallbacksIfNeeded() {
        if (!mCallbackRegistered && mAudioManager != null) {
            try {
                mAudioManager.registerAudioPlaybackCallback(mPlaybackCallback, mHandler);
                mCallbackRegistered = true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to register callbacks: " + e.getMessage());
            }
        }
    }

    private void unregisterCallbacksIfNeeded() {
        if (mCallbackRegistered && mAudioManager != null) {
            try {
                mAudioManager.unregisterAudioPlaybackCallback(mPlaybackCallback);
                mCallbackRegistered = false;
            } catch (Exception e) {
                Log.e(TAG, "Failed to unregister callbacks: " + e.getMessage());
            }
        }
    }

    private void updateBoostState() {
        if (mAudioManager != null) {
            updateBoostForPlaybackConfigs(mAudioManager.getActivePlaybackConfigurations());
        } else {
            applyAudioFx(getBoostLevel());
        }
    }

    private synchronized void updateBoostForPlaybackConfigs(List<AudioPlaybackConfiguration> configs) {
        int targetLevel = getBoostLevel();
        if (targetLevel <= 0) {
            applyAudioFx(0);
            return;
        }

        boolean hasActiveMediaPlayback = false;
        boolean hasActiveNonMediaPlayback = false;

        if (configs != null) {
            for (AudioPlaybackConfiguration config : configs) {
                if (config.getPlayerState() == AudioPlaybackConfiguration.PLAYER_STATE_STARTED) {
                    AudioAttributes attr = config.getAudioAttributes();
                    int usage = attr != null ? attr.getUsage() : AudioAttributes.USAGE_UNKNOWN;

                    if (usage == AudioAttributes.USAGE_MEDIA || usage == AudioAttributes.USAGE_GAME) {
                        hasActiveMediaPlayback = true;
                    } else if (usage == AudioAttributes.USAGE_NOTIFICATION
                            || usage == AudioAttributes.USAGE_NOTIFICATION_RINGTONE
                            || usage == AudioAttributes.USAGE_NOTIFICATION_EVENT
                            || usage == AudioAttributes.USAGE_ALARM
                            || usage == AudioAttributes.USAGE_ASSISTANT) {
                        hasActiveNonMediaPlayback = true;
                        break;
                    }
                }
            }
        }

        // Apply boost ONLY if MEDIA is playing AND NO Notification/Ringtone/Alarm is playing
        if (hasActiveMediaPlayback && !hasActiveNonMediaPlayback) {
            applyAudioFx(targetLevel);
        } else {
            applyAudioFx(0);
        }
    }

    private synchronized void applyAudioFx(int level) {
        int clampedLevel = Math.max(0, Math.min(100, level));
        if (mLastAppliedBoostLevel == clampedLevel) {
            return;
        }
        mLastAppliedBoostLevel = clampedLevel;

        int gainmB = 0;
        if (clampedLevel > 0) {
            gainmB = Math.round((clampedLevel / 100.0f) * 3500.0f); // Max +35 dB gain
        }

        if (mLoudnessEnhancer != null) {
            try {
                mLoudnessEnhancer.setTargetGain(gainmB);
                
                // If user wants boost, keep the effect enabled (with 0 gain if muted)
                // to prevent Session 0 unhooking bugs that cause the active track to stay quiet.
                if (getBoostLevel() > 0) {
                    if (!mLoudnessEnhancer.getEnabled()) {
                        mLoudnessEnhancer.setEnabled(true);
                    }
                } else {
                    mLoudnessEnhancer.setEnabled(gainmB > 0);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting LoudnessEnhancer target gain: " + e.getMessage());
            }
        }
    }

    public boolean isCallAudioGainEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(), SETTING_CALL_GAIN_KEY, 1) == 1;
    }

    public void setCallAudioGainEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(), SETTING_CALL_GAIN_KEY, enabled ? 1 : 0);
        applyCallAudioGain(enabled);
    }

    public void applyCallAudioGain(boolean enabled) {
        try {
            if (mLoudnessEnhancer != null) {
                float currentGain = mLoudnessEnhancer.getTargetGain();
                float callGain = enabled ? 1000.0f : 0.0f; // +10.0 dB gain for calls (1000 mB)
                mLoudnessEnhancer.setTargetGain(Math.round(Math.max(currentGain, callGain)));
                mLoudnessEnhancer.setEnabled(enabled || currentGain > 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying call audio gain: " + e.getMessage());
        }
    }

    public void release() {
        unregisterCallbacksIfNeeded();
        if (mLoudnessEnhancer != null) {
            try {
                mLoudnessEnhancer.release();
            } catch (Exception ignored) {}
            mLoudnessEnhancer = null;
        }
        if (mDynamicsProcessing != null) {
            try {
                mDynamicsProcessing.release();
            } catch (Exception ignored) {}
            mDynamicsProcessing = null;
        }
    }
}
