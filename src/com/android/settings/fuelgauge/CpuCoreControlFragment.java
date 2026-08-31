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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.widget.LayoutPreference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class CpuCoreControlFragment extends DashboardFragment implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "CpuCoreControlFragment";
    private static final int MAX_CORES = 8;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final TextView[] mFreqViews = new TextView[MAX_CORES];
    private final CpuSparklineView[] mGraphViews = new CpuSparklineView[MAX_CORES];
    private final long[] mMaxFreqs = new long[MAX_CORES];

    private final Runnable mUpdateTask = new Runnable() {
        @Override
        public void run() {
            updateLiveGraphs();
            mHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.cpu_core_control;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FUELGAUGE_BATTERY_SAVER;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Core 0 is system main core: ALWAYS enabled, cannot be turned off
        SwitchPreferenceCompat core0 = findPreference("cpu_core_0");
        if (core0 != null) {
            core0.setChecked(true);
            core0.setEnabled(false);
            updateSummary(0, core0, true);
        }

        for (int i = 1; i < MAX_CORES; i++) {
            SwitchPreferenceCompat pref = findPreference("cpu_core_" + i);
            if (pref != null) {
                boolean isOnline = isCoreOnline(i);
                pref.setChecked(isOnline);
                updateSummary(i, pref, isOnline);
                pref.setOnPreferenceChangeListener(this);
            }
        }

        initClusterGraphHeader();
    }

    private void initClusterGraphHeader() {
        LayoutPreference headerPref = findPreference("cpu_cluster_graph_header");
        if (headerPref == null) return;
        View view = headerPref.findViewById(R.id.cpu_0_graph);
        if (view == null) return;

        int[] colors = new int[] {
            0xFF81C784, 0xFF81C784, 0xFF81C784, 0xFF81C784, // Cores 0-3 (Green)
            0xFFDCE775, 0xFFDCE775, 0xFFDCE775,            // Cores 4-6 (Yellow-Green)
            0xFFFFB74D                                     // Core 7 (Amber/Gold)
        };

        for (int i = 0; i < MAX_CORES; i++) {
            mMaxFreqs[i] = getMaxFrequencyKHz(i);
            int freqResId = getResources().getIdentifier("cpu_" + i + "_freq", "id", getContext().getPackageName());
            int graphResId = getResources().getIdentifier("cpu_" + i + "_graph", "id", getContext().getPackageName());

            if (freqResId != 0) mFreqViews[i] = headerPref.findViewById(freqResId);
            if (graphResId != 0) mGraphViews[i] = headerPref.findViewById(graphResId);

            if (mGraphViews[i] != null) {
                mGraphViews[i].setLineColor(colors[i]);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mHandler.post(mUpdateTask);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mUpdateTask);
    }

    private void updateLiveGraphs() {
        for (int i = 0; i < MAX_CORES; i++) {
            boolean online = isCoreOnline(i);
            long curKHz = online ? getCoreFrequencyKHz(i) : 0;
            long maxKHz = mMaxFreqs[i] > 0 ? mMaxFreqs[i] : 3000000;

            if (mFreqViews[i] != null) {
                if (!online) {
                    mFreqViews[i].setText("OFFLINE");
                    mFreqViews[i].setTextColor(0xFF888888);
                } else {
                    mFreqViews[i].setText(formatKHz(curKHz));
                    mFreqViews[i].setTextColor(i < 4 ? 0xFF81C784 : (i < 7 ? 0xFFDCE775 : 0xFFFFB74D));
                }
            }

            if (mGraphViews[i] != null) {
                float ratio = online ? (float) curKHz / (float) maxKHz : 0f;
                mGraphViews[i].addValue(ratio);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (key != null && key.startsWith("cpu_core_")) {
            try {
                int coreId = Integer.parseInt(key.substring("cpu_core_".length()));
                if (coreId == 0) {
                    return false; // Safety lock: Core 0 cannot be turned off
                }
                boolean online = (Boolean) newValue;
                Context context = getContext();
                if (context != null) {
                    Settings.System.putInt(context.getContentResolver(), "cpu_core_online_" + coreId, online ? 1 : 0);
                }
                setCoreOnline(coreId, online);
                if (preference instanceof SwitchPreferenceCompat) {
                    updateSummary(coreId, (SwitchPreferenceCompat) preference, online);
                }
                updateLiveGraphs();
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error changing core state", e);
            }
        }
        return false;
    }

    private void updateSummary(int cpuId, SwitchPreferenceCompat pref, boolean isOnline) {
        Context context = getContext();
        if (context == null) return;

        String freq = formatKHz(getCoreFrequencyKHz(cpuId));
        String type;
        if (cpuId == 0) {
            type = context.getString(R.string.cpu_core_type_prime);
        } else if (cpuId <= 3) {
            type = context.getString(R.string.cpu_core_type_efficiency);
        } else {
            type = context.getString(R.string.cpu_core_type_performance);
        }

        if (cpuId == 0) {
            pref.setSummary(type + (freq.isEmpty() ? "" : " • " + freq) + " • " + context.getString(R.string.cpu_core_0_locked_summary));
        } else if (isOnline) {
            pref.setSummary(type + (freq.isEmpty() ? "" : " • " + freq) + " • " + context.getString(R.string.cpu_core_online_summary));
        } else {
            pref.setSummary(type + " • " + context.getString(R.string.cpu_core_offline_summary));
        }
    }

    public static long getCoreFrequencyKHz(int cpuId) {
        String path = "/sys/devices/system/cpu/cpu" + cpuId + "/cpufreq/scaling_cur_freq";
        File file = new File(path);
        if (!file.exists()) {
            path = "/sys/devices/system/cpu/cpu" + cpuId + "/cpufreq/cpuinfo_cur_freq";
            file = new File(path);
        }
        if (!file.exists()) return 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null) {
                return Long.parseLong(line.trim());
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }

    public static long getMaxFrequencyKHz(int cpuId) {
        String path = "/sys/devices/system/cpu/cpu" + cpuId + "/cpufreq/cpuinfo_max_freq";
        File file = new File(path);
        if (!file.exists()) return 3000000;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null) {
                return Long.parseLong(line.trim());
            }
        } catch (Exception e) {
            // Ignore
        }
        return 3000000;
    }

    private static String formatKHz(long freqKHz) {
        if (freqKHz <= 0) return "";
        return (freqKHz / 1000) + " MHz";
    }

    public static boolean isCoreOnline(int cpuId) {
        if (cpuId == 0) return true;
        String path = "/sys/devices/system/cpu/cpu" + cpuId + "/online";
        File file = new File(path);
        if (!file.exists()) return true;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            return "1".equals(line != null ? line.trim() : "1");
        } catch (IOException e) {
            return true;
        }
    }

    public static boolean setCoreOnline(int cpuId, boolean online) {
        if (cpuId == 0) return false; // Core 0 is locked system core
        String path = "/sys/devices/system/cpu/cpu" + cpuId + "/online";
        File file = new File(path);
        if (!file.exists()) return false;
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "chmod 666 " + path + " && echo " + (online ? "1" : "0") + " > " + path});
            p.waitFor();
            if (p.exitValue() == 0) return true;
        } catch (Exception ignored) {
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(online ? "1" : "0");
            writer.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to write online status to " + path, e);
            return false;
        }
    }
}
