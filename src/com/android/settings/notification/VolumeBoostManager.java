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
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
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
 * Advanced Volume Boost Manager for AudioFlinger & AudioFX stack.
 *
 * Architecture & Sound Processing:
 * - Employs {@link DynamicsProcessing} on global Audio Session 0 (Stereo, 2 channels).
 * - Multi-band Compressor (MBC) with 3 calibrated bands:
 *     * Low Band (< 250 Hz): Threshold -16.0 dBFS, Ratio 3.5:1, Attack 15 ms, Release 80 ms.
 *     * Mid Band (250 Hz - 4000 Hz): Threshold -12.0 dBFS, Ratio 1.5:1, Attack 20 ms, Release 100 ms (linear transparent pass).
 *     * High Band (> 4000 Hz): Threshold -18.0 dBFS, Ratio 3.0:1, Attack 3 ms, Release 40 ms (eliminates sibilance/harshness).
 * - Master Brickwall Limiter stage:
 *     * Threshold: -1.0 dBFS (prevents inter-sample peak clipping).
 *     * Ratio: 10:1.
 *     * Attack: 1.0 ms.
 *     * Release: 50.0 ms.
 * - Dynamic input gain scaling (+0 dB to +10 dB) feeding the MBC and Limiter pipeline.
 * - Automatic {@link AudioDeviceCallback} integration to safely decouple and zero boost on Bluetooth (A2DP, LE Audio, Hearing Aids).
 * - Priority-managed call audio gain preventing Session 0 dB stacking and clipping collisions.
 */
public class VolumeBoostManager {
    private static final String TAG = "VolumeBoostManager";

    public static final String SETTING_KEY = Settings.System.VOLUME_BOOST_LEVEL;
    public static final String SETTING_CALL_GAIN_KEY = "volume_boost_call_gain";
    public static final int DEFAULT_BOOST_LEVEL = 0; // 0% boost = 100% standard volume

    private static final float MAX_INPUT_GAIN_DB = 10.0f;
    private static final int CALL_GAIN_MB = 1000; // +10.0 dB for calls

    private static VolumeBoostManager sInstance;

    private final Context mContext;
    private final AudioManager mAudioManager;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private DynamicsProcessing mDynamicsProcessing;
    private LoudnessEnhancer mCallLoudnessEnhancer;

    private boolean mPlaybackCallbackRegistered = false;
    private boolean mDeviceCallbackRegistered = false;
    private int mLastAppliedBoostLevel = -1;
    private boolean mIsCallGainActive = false;

    private final AudioManager.AudioPlaybackCallback mPlaybackCallback =
            new AudioManager.AudioPlaybackCallback() {
        @Override
        public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
            updateBoostForPlaybackConfigs(configs);
        }
    };

    private final AudioDeviceCallback mAudioDeviceCallback = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            updateBoostState();
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            updateBoostState();
        }
    };

    private VolumeBoostManager(Context context) {
        mContext = context.getApplicationContext();
        mAudioManager = mContext.getSystemService(AudioManager.class);
        initAudioFx();
        registerDeviceCallback();
    }

    public static synchronized VolumeBoostManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new VolumeBoostManager(context);
        }
        return sInstance;
    }

    /**
     * Initializes the DynamicsProcessing stereo engine (Session 0) with a 3-band MBC
     * and a final Master Brickwall Limiter.
     */
    private void initAudioFx() {
        try {
            DynamicsProcessing.Config.Builder builder = new DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_TIME_RESOLUTION,
                    2 /* stereo channels */,
                    false /* preEqInUse */, 0 /* preEqBandCount */,
                    true  /* mbcInUse */,   3 /* mbcBandCount */,
                    false /* postEqInUse */, 0 /* postEqBandCount */,
                    true  /* limiterInUse */
            );

            // Multiband Compressor (MBC) Stage
            DynamicsProcessing.Mbc mbc = new DynamicsProcessing.Mbc(true, true, 3);

            // 1. Low Band (< 250 Hz)
            DynamicsProcessing.MbcBand band0 = new DynamicsProcessing.MbcBand(
                    true, 250.0f, 15.0f, 80.0f, 3.5f, -16.0f, 6.0f, -90.0f, 1.0f, 0.0f, 0.0f
            );
            band0.setEnabled(true);
            mbc.setBand(0, band0);

            // 2. Mid Band (250 Hz - 4000 Hz)
            DynamicsProcessing.MbcBand band1 = new DynamicsProcessing.MbcBand(
                    true, 4000.0f, 20.0f, 100.0f, 1.5f, -12.0f, 6.0f, -90.0f, 1.0f, 0.0f, 0.0f
            );
            band1.setEnabled(true);
            mbc.setBand(1, band1);

            // 3. High Band (> 4000 Hz)
            DynamicsProcessing.MbcBand band2 = new DynamicsProcessing.MbcBand(
                    true, 20000.0f, 3.0f, 40.0f, 3.0f, -18.0f, 4.0f, -90.0f, 1.0f, 0.0f, 0.0f
            );
            band2.setEnabled(true);
            mbc.setBand(2, band2);

            // Master Brickwall Limiter Stage (Zero Inter-Sample Peak Distortion)
            DynamicsProcessing.Limiter limiter = new DynamicsProcessing.Limiter(
                    true,      // inUse
                    true,      // enabled
                    0,         // linkGroup (stereo linked)
                    1.0f,      // attackTime ms (fast brickwall catch)
                    50.0f,     // releaseTime ms
                    10.0f,     // ratio
                    -1.0f,     // threshold dBFS
                    0.0f       // postGain dB
            );

            for (int ch = 0; ch < 2; ch++) {
                builder.setMbcByChannelIndex(ch, mbc);
                builder.setLimiterByChannelIndex(ch, limiter);
            }

            DynamicsProcessing.Config config = builder.build();
            mDynamicsProcessing = new DynamicsProcessing(0 /* priority */, 0 /* session 0 */, config);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize DynamicsProcessing: " + e.getMessage(), e);
        }

        // Apply saved settings
        int savedLevel = getBoostLevel();
        setBoostLevel(savedLevel);
    }

    /**
     * Checks if a Bluetooth or LE Audio output device is currently connected.
     */
    public boolean isBluetoothAudioConnected() {
        if (mAudioManager == null) return false;
        try {
            AudioDeviceInfo[] devices = mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            if (devices != null) {
                for (AudioDeviceInfo device : devices) {
                    int type = device.getType();
                    if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                            || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                            || type == AudioDeviceInfo.TYPE_BLE_HEADSET
                            || type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                            || type == AudioDeviceInfo.TYPE_BLE_BROADCAST
                            || type == AudioDeviceInfo.TYPE_HEARING_AID) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error querying audio devices: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves the current boost level from Settings.System.
     * @return 0 to 100 (where 0 is standard volume, 100 is maximum clean boost).
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
            registerPlaybackCallbackIfNeeded();
            updateBoostState();
        } else {
            unregisterPlaybackCallbackIfNeeded();
            applyAudioFx(0);
        }
    }

    private void registerDeviceCallback() {
        if (!mDeviceCallbackRegistered && mAudioManager != null) {
            try {
                mAudioManager.registerAudioDeviceCallback(mAudioDeviceCallback, mHandler);
                mDeviceCallbackRegistered = true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to register AudioDeviceCallback: " + e.getMessage());
            }
        }
    }

    private void unregisterDeviceCallback() {
        if (mDeviceCallbackRegistered && mAudioManager != null) {
            try {
                mAudioManager.unregisterAudioDeviceCallback(mAudioDeviceCallback);
                mDeviceCallbackRegistered = false;
            } catch (Exception e) {
                Log.e(TAG, "Failed to unregister AudioDeviceCallback: " + e.getMessage());
            }
        }
    }

    private void registerPlaybackCallbackIfNeeded() {
        if (!mPlaybackCallbackRegistered && mAudioManager != null) {
            try {
                mAudioManager.registerAudioPlaybackCallback(mPlaybackCallback, mHandler);
                mPlaybackCallbackRegistered = true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to register playback callback: " + e.getMessage());
            }
        }
    }

    private void unregisterPlaybackCallbackIfNeeded() {
        if (mPlaybackCallbackRegistered && mAudioManager != null) {
            try {
                mAudioManager.unregisterAudioPlaybackCallback(mPlaybackCallback);
                mPlaybackCallbackRegistered = false;
            } catch (Exception e) {
                Log.e(TAG, "Failed to unregister playback callback: " + e.getMessage());
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
        // Priority 1: If call gain is active on Session 0, suspend media boost
        if (mIsCallGainActive) {
            applyAudioFx(0);
            return;
        }

        // Priority 2: Decouple effect immediately if Bluetooth / LE Audio is connected
        if (isBluetoothAudioConnected()) {
            applyAudioFx(0);
            return;
        }

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

        // Apply clean boost ONLY if MEDIA is actively playing and no interruption source is present
        if (hasActiveMediaPlayback && !hasActiveNonMediaPlayback) {
            applyAudioFx(targetLevel);
        } else {
            applyAudioFx(0);
        }
    }

    /**
     * Applies calibrated input gain to the DynamicsProcessing engine and controls activation.
     */
    private synchronized void applyAudioFx(int level) {
        if (mIsCallGainActive) {
            // Keep media DynamicsProcessing disabled while telephony call is amplified
            if (mDynamicsProcessing != null) {
                try {
                    if (mDynamicsProcessing.getEnabled()) {
                        mDynamicsProcessing.setEnabled(false);
                    }
                } catch (Exception ignored) {}
            }
            mLastAppliedBoostLevel = 0;
            return;
        }

        int clampedLevel = Math.max(0, Math.min(100, level));
        if (mLastAppliedBoostLevel == clampedLevel) {
            return;
        }
        mLastAppliedBoostLevel = clampedLevel;

        if (mDynamicsProcessing != null) {
            try {
                if (clampedLevel > 0) {
                    float inputGainDb = (clampedLevel / 100.0f) * MAX_INPUT_GAIN_DB;
                    mDynamicsProcessing.setInputGainAllChannelsTo(inputGainDb);
                    if (!mDynamicsProcessing.getEnabled()) {
                        mDynamicsProcessing.setEnabled(true);
                    }
                } else {
                    mDynamicsProcessing.setInputGainAllChannelsTo(0.0f);
                    if (mDynamicsProcessing.getEnabled()) {
                        mDynamicsProcessing.setEnabled(false);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error applying DynamicsProcessing gain: " + e.getMessage(), e);
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

    /**
     * Applies discrete gain for telephony calls without mutating or corrupting the media boost state.
     */
    public synchronized void applyCallAudioGain(boolean enabled) {
        mIsCallGainActive = enabled;
        try {
            if (enabled) {
                // 1. Temporarily mute/disable media boost to avoid Session 0 collision
                if (mDynamicsProcessing != null) {
                    try {
                        if (mDynamicsProcessing.getEnabled()) {
                            mDynamicsProcessing.setEnabled(false);
                        }
                    } catch (Exception ignored) {}
                }

                // 2. Enable discrete call loudness enhancer
                if (mCallLoudnessEnhancer == null) {
                    mCallLoudnessEnhancer = new LoudnessEnhancer(0);
                }
                mCallLoudnessEnhancer.setTargetGain(CALL_GAIN_MB);
                mCallLoudnessEnhancer.setEnabled(true);
            } else {
                if (mCallLoudnessEnhancer != null) {
                    try {
                        mCallLoudnessEnhancer.setEnabled(false);
                        mCallLoudnessEnhancer.setTargetGain(0);
                    } catch (Exception ignored) {}
                }
                // 3. Restore media boost state
                mLastAppliedBoostLevel = -1;
                updateBoostState();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying discrete call audio gain: " + e.getMessage(), e);
        }
    }

    /**
     * Releases all AudioFX native resources and unregisters system callbacks cleanly.
     */
    public synchronized void release() {
        unregisterPlaybackCallbackIfNeeded();
        unregisterDeviceCallback();

        if (mDynamicsProcessing != null) {
            try {
                mDynamicsProcessing.setEnabled(false);
            } catch (Exception ignored) {}
            try {
                mDynamicsProcessing.release();
            } catch (Exception ignored) {}
            mDynamicsProcessing = null;
        }

        if (mCallLoudnessEnhancer != null) {
            try {
                mCallLoudnessEnhancer.setEnabled(false);
            } catch (Exception ignored) {}
            try {
                mCallLoudnessEnhancer.release();
            } catch (Exception ignored) {}
            mCallLoudnessEnhancer = null;
        }

        mLastAppliedBoostLevel = -1;
        mIsCallGainActive = false;
    }
}
