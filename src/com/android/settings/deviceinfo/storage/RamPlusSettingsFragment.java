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

package com.android.settings.deviceinfo.storage;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.SystemProperties;
import android.widget.CompoundButton;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for RAM Plus (Virtual RAM / ZRAM) configuration.
 */
public class RamPlusSettingsFragment extends SettingsPreferenceFragment
        implements CompoundButton.OnCheckedChangeListener, SelectorWithWidgetPreference.OnClickListener {

    private static final String KEY_SWITCH = "ram_plus_switch";
    private static final String KEY_CATEGORY = "ram_plus_options_category";

    private MainSwitchPreference mMainSwitch;
    private PreferenceCategory mOptionsCategory;
    private final List<SelectorWithWidgetPreference> mOptionPreferences = new ArrayList<>();

    private boolean mIsEnabled;
    private int mCurrentSizeGb;

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.ram_plus_settings);

        mMainSwitch = findPreference(KEY_SWITCH);
        mOptionsCategory = findPreference(KEY_CATEGORY);

        loadCurrentState();
        setupSwitch();
        populateOptions();
        updateUiState();
    }

    private void loadCurrentState() {
        mIsEnabled = SystemProperties.getBoolean(RamPlusPreferenceController.PROP_RAM_PLUS_ENABLED, true);
        mCurrentSizeGb = SystemProperties.getInt(RamPlusPreferenceController.PROP_RAM_PLUS_SIZE, 4);
    }

    private void setupSwitch() {
        if (mMainSwitch != null) {
            mMainSwitch.setChecked(mIsEnabled);
            mMainSwitch.addOnSwitchChangeListener(this);
        }
    }

    private int[] getAvailableRamPlusSizes() {
        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            long totalBytes = stat.getTotalBytes();
            double totalGb = (double) totalBytes / (1024L * 1024L * 1024L);

            if (totalGb >= 380.0) {
                // 512GB or 1TB internal storage -> up to 12GB
                return new int[]{2, 4, 6, 8, 12};
            } else if (totalGb >= 190.0) {
                // 256GB internal storage -> up to 8GB
                return new int[]{2, 4, 6, 8};
            } else {
                // <=128GB internal storage -> up to 6GB
                return new int[]{2, 4, 6};
            }
        } catch (Exception e) {
            return new int[]{2, 4, 6, 8};
        }
    }

    private void populateOptions() {
        if (mOptionsCategory == null) {
            return;
        }

        mOptionsCategory.removeAll();
        mOptionPreferences.clear();

        int[] sizes = getAvailableRamPlusSizes();
        Context prefContext = getPrefContext();

        for (int size : sizes) {
            SelectorWithWidgetPreference pref = new SelectorWithWidgetPreference(prefContext);
            pref.setKey("ram_plus_option_" + size);
            pref.setTitle(getString(R.string.ram_plus_gb_format, size));
            pref.setOnClickListener(this);
            mOptionsCategory.addPreference(pref);
            mOptionPreferences.add(pref);
        }
    }

    private void updateUiState() {
        if (mMainSwitch != null) {
            mMainSwitch.setChecked(mIsEnabled);
        }

        int[] sizes = getAvailableRamPlusSizes();
        boolean validSelection = false;
        for (int size : sizes) {
            if (size == mCurrentSizeGb) {
                validSelection = true;
                break;
            }
        }
        if (!validSelection && sizes.length > 0) {
            mCurrentSizeGb = sizes[0];
        }

        for (int i = 0; i < mOptionPreferences.size() && i < sizes.length; i++) {
            SelectorWithWidgetPreference pref = mOptionPreferences.get(i);
            int size = sizes[i];
            pref.setChecked(mIsEnabled && (size == mCurrentSizeGb));
            pref.setEnabled(mIsEnabled);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked == mIsEnabled) {
            return;
        }
        showRestartDialog(isChecked, mCurrentSizeGb);
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference emiter) {
        String key = emiter.getKey();
        if (key == null || !key.startsWith("ram_plus_option_")) {
            return;
        }

        int selectedSize = Integer.parseInt(key.replace("ram_plus_option_", ""));
        if (selectedSize == mCurrentSizeGb && mIsEnabled) {
            return;
        }

        showRestartDialog(true, selectedSize);
    }

    private long getTotalRamBytes() {
        try {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager am = (ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.getMemoryInfo(mi);
                return mi.totalMem;
            }
        } catch (Exception ignored) {
        }
        return 8L * 1024 * 1024 * 1024;
    }

    private void applyRamPlusSettings(boolean enabled, int sizeGb) {
        mIsEnabled = enabled;
        mCurrentSizeGb = sizeGb;

        SystemProperties.set(RamPlusPreferenceController.PROP_RAM_PLUS_ENABLED, enabled ? "1" : "0");
        SystemProperties.set(RamPlusPreferenceController.PROP_RAM_PLUS_SIZE, String.valueOf(sizeGb));

        if (enabled) {
            long totalRam = getTotalRamBytes();
            int percent = (int) Math.round(((double) sizeGb * 1024L * 1024L * 1024L / totalRam) * 100);
            if (percent < 10) percent = 10;
            if (percent > 80) percent = 80;
            SystemProperties.set("persist.sys.zram_size_percent", String.valueOf(percent));
        } else {
            SystemProperties.set("persist.sys.zram_size_percent", "10");
        }
    }

    private void showRestartDialog(final boolean newEnabled, final int newSizeGb) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.ram_plus_reboot_dialog_title)
                .setMessage(R.string.ram_plus_reboot_dialog_message)
                .setPositiveButton(R.string.ram_plus_reboot_now, (dialog, which) -> {
                    applyRamPlusSettings(newEnabled, newSizeGb);
                    PowerManager pm = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
                    if (pm != null) {
                        pm.reboot(null);
                    }
                })
                .setNegativeButton(R.string.ram_plus_reboot_later, (dialog, which) -> {
                    applyRamPlusSettings(newEnabled, newSizeGb);
                    updateUiState();
                })
                .setOnCancelListener(dialog -> {
                    updateUiState();
                })
                .show();
    }
}
