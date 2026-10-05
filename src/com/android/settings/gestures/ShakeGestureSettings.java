/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.gestures;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SearchIndexable
public class ShakeGestureSettings extends DashboardFragment implements
        SelectorWithWidgetPreference.OnClickListener {

    private static final String TAG = "ShakeGestureSettings";

    public static final String KEY_MAIN_SWITCH = "shake_gesture_main_switch";
    public static final String KEY_ACTION_SCREENSHOT = "shake_action_screenshot";
    public static final String KEY_ACTION_ASSISTANT = "shake_action_assistant";
    public static final String KEY_ACTION_MEDIA = "shake_action_media";
    public static final String KEY_ACTION_RECENTS = "shake_action_recents";
    public static final String KEY_ACTION_NOTIFICATIONS = "shake_action_notifications";
    public static final String KEY_ACTION_TORCH = "shake_action_torch";
    public static final String KEY_ACTION_APP = "shake_action_app";
    public static final String KEY_SENSITIVITY = "shake_gesture_sensitivity_slider";

    private MainSwitchPreference mMainSwitch;
    private SelectorWithWidgetPreference mPrefScreenshot;
    private SelectorWithWidgetPreference mPrefAssistant;
    private SelectorWithWidgetPreference mPrefMedia;
    private SelectorWithWidgetPreference mPrefRecents;
    private SelectorWithWidgetPreference mPrefNotifications;
    private SelectorWithWidgetPreference mPrefTorch;
    private SelectorWithWidgetPreference mPrefApp;
    private SeekBarPreference mPrefSensitivity;

    private final Map<String, SelectorWithWidgetPreference> mActionMap = new HashMap<>();

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.shake_gesture_settings;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mMainSwitch = findPreference(KEY_MAIN_SWITCH);
        mPrefScreenshot = findPreference(KEY_ACTION_SCREENSHOT);
        mPrefAssistant = findPreference(KEY_ACTION_ASSISTANT);
        mPrefMedia = findPreference(KEY_ACTION_MEDIA);
        mPrefRecents = findPreference(KEY_ACTION_RECENTS);
        mPrefNotifications = findPreference(KEY_ACTION_NOTIFICATIONS);
        mPrefTorch = findPreference(KEY_ACTION_TORCH);
        mPrefApp = findPreference(KEY_ACTION_APP);
        mPrefSensitivity = findPreference(KEY_SENSITIVITY);

        mActionMap.put("screenshot", mPrefScreenshot);
        mActionMap.put("assistant", mPrefAssistant);
        mActionMap.put("media_play_pause", mPrefMedia);
        mActionMap.put("recents", mPrefRecents);
        mActionMap.put("notifications", mPrefNotifications);
        mActionMap.put("torch", mPrefTorch);
        mActionMap.put("app", mPrefApp);

        for (SelectorWithWidgetPreference pref : mActionMap.values()) {
            if (pref != null) {
                pref.setOnClickListener(this);
            }
        }

        if (mPrefApp != null) {
            mPrefApp.setExtraWidgetOnClickListener(v -> showAppPickerDialog());
        }

        if (mMainSwitch != null) {
            mMainSwitch.addOnSwitchChangeListener((switchView, isChecked) -> {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SHAKE_GESTURE_ENABLED, isChecked ? 1 : 0);
                updateEnabledStates(isChecked);
            });
        }

        if (mPrefSensitivity != null) {
            mPrefSensitivity.setContinuousUpdates(true);
            mPrefSensitivity.setOnPreferenceChangeListener((preference, newValue) -> {
                int val = (Integer) newValue;
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SHAKE_GESTURE_SENSITIVITY, val);
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        final boolean isEnabled = Settings.System.getInt(getContentResolver(),
                Settings.System.SHAKE_GESTURE_ENABLED, 0) == 1;
        if (mMainSwitch != null) {
            mMainSwitch.setChecked(isEnabled);
        }

        final String currentAction = Settings.System.getString(getContentResolver(),
                Settings.System.SHAKE_GESTURE_ACTION);
        final String actionKey = (currentAction != null && !currentAction.isEmpty())
                ? currentAction : "torch";

        for (Map.Entry<String, SelectorWithWidgetPreference> entry : mActionMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().setChecked(entry.getKey().equals(actionKey));
            }
        }

        updateAppSummary();

        final int sensitivity = Settings.System.getInt(getContentResolver(),
                Settings.System.SHAKE_GESTURE_SENSITIVITY, 3);
        if (mPrefSensitivity != null) {
            mPrefSensitivity.setProgress(sensitivity);
        }

        updateEnabledStates(isEnabled);
    }

    private void updateEnabledStates(boolean isEnabled) {
        for (SelectorWithWidgetPreference pref : mActionMap.values()) {
            if (pref != null) {
                pref.setEnabled(isEnabled);
            }
        }
        if (mPrefSensitivity != null) {
            mPrefSensitivity.setEnabled(isEnabled);
        }
    }

    private void updateAppSummary() {
        if (mPrefApp == null) return;
        final String appPkg = Settings.System.getString(getContentResolver(),
                Settings.System.SHAKE_GESTURE_APP);
        if (appPkg != null && !appPkg.isEmpty()) {
            PackageManager pm = getPackageManager();
            try {
                ApplicationInfo info = pm.getApplicationInfo(appPkg, 0);
                CharSequence label = pm.getApplicationLabel(info);
                mPrefApp.setSummary(label);
            } catch (Exception e) {
                mPrefApp.setSummary(appPkg);
            }
        } else {
            mPrefApp.setSummary(R.string.shake_action_app_no_app);
        }
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
        for (Map.Entry<String, SelectorWithWidgetPreference> entry : mActionMap.entrySet()) {
            if (entry.getValue() == preference) {
                entry.getValue().setChecked(true);
                Settings.System.putString(getContentResolver(),
                        Settings.System.SHAKE_GESTURE_ACTION, entry.getKey());
                if ("app".equals(entry.getKey())) {
                    String appPkg = Settings.System.getString(getContentResolver(),
                            Settings.System.SHAKE_GESTURE_APP);
                    if (appPkg == null || appPkg.isEmpty()) {
                        showAppPickerDialog();
                    }
                }
            } else if (entry.getValue() != null) {
                entry.getValue().setChecked(false);
            }
        }
    }

    private void showAppPickerDialog() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);

        final List<AppItem> appList = new ArrayList<>();
        for (ResolveInfo ri : resolveInfos) {
            String pkg = ri.activityInfo.packageName;
            CharSequence label = ri.loadLabel(pm);
            Drawable icon = ri.loadIcon(pm);
            appList.add(new AppItem(pkg, label.toString(), icon));
        }

        Collections.sort(appList, Comparator.comparing(a -> a.label.toLowerCase()));

        ArrayAdapter<AppItem> adapter = new ArrayAdapter<AppItem>(getContext(),
                android.R.layout.select_dialog_item, appList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    view = LayoutInflater.from(getContext()).inflate(
                            R.layout.preference_app_picker_item, parent, false);
                }
                AppItem item = getItem(position);
                if (item != null) {
                    ImageView iconView = view.findViewById(R.id.app_icon);
                    TextView titleView = view.findViewById(R.id.app_title);
                    if (iconView != null) iconView.setImageDrawable(item.icon);
                    if (titleView != null) titleView.setText(item.label);
                }
                return view;
            }
        };

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.shake_gesture_select_app_title)
                .setAdapter(adapter, (dialog, which) -> {
                    AppItem chosen = appList.get(which);
                    Settings.System.putString(getContentResolver(),
                            Settings.System.SHAKE_GESTURE_APP, chosen.packageName);
                    Settings.System.putString(getContentResolver(),
                            Settings.System.SHAKE_GESTURE_ACTION, "app");
                    updateUI();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static class AppItem {
        final String packageName;
        final String label;
        final Drawable icon;

        AppItem(String pkg, String label, Drawable icon) {
            this.packageName = pkg;
            this.label = label;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.shake_gesture_settings;
                    return Arrays.asList(sir);
                }
            };
}
