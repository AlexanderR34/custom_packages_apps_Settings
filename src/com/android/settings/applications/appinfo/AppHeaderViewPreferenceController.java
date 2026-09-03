/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.appinfo;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.widget.LayoutPreference;

public class AppHeaderViewPreferenceController extends BasePreferenceController
        implements AppInfoDashboardFragment.Callback, LifecycleObserver {

    private static final String KEY_HEADER = "header_view";

    private LayoutPreference mHeader;
    private final AppInfoDashboardFragment mParent;
    private final String mPackageName;
    private final Lifecycle mLifecycle;

    private EntityHeaderController mEntityHeaderController;

    public AppHeaderViewPreferenceController(Context context, AppInfoDashboardFragment parent,
            String packageName, Lifecycle lifecycle) {
        super(context, KEY_HEADER);
        mParent = parent;
        mPackageName = packageName;
        mLifecycle = lifecycle;
        if (mLifecycle != null) {
            mLifecycle.addObserver(this);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mHeader = screen.findPreference(KEY_HEADER);
        final Activity activity = mParent.getActivity();
        mEntityHeaderController = EntityHeaderController
                .newInstance(activity, mParent, mHeader.findViewById(R.id.entity_header))
                .setPackageName(mPackageName)
                .setButtonActions(EntityHeaderController.ActionType.ACTION_NONE,
                        EntityHeaderController.ActionType.ACTION_NONE)
                .bindHeaderButtons();
    }

    @Override
    public void refreshUi() {
        setAppLabelAndIcon(mParent.getPackageInfo(), mParent.getAppEntry());
    }

    // Utility method to set application label and icon.
    private void setAppLabelAndIcon(PackageInfo pkgInfo, AppEntry appEntry) {
        final Activity activity = mParent.getActivity();
        final boolean isInstantApp = AppUtils.isInstant(pkgInfo.applicationInfo);
        mEntityHeaderController
                .setLabel(appEntry)
                .setIcon(appEntry)
                .setIsInstantApp(isInstantApp)
                .done(false /* rebindActions */);

        if (mHeader != null && activity != null) {
            TextView titleView = mHeader.findViewById(R.id.entity_header_title);
            if (titleView != null) {
                titleView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_edit_app_name, 0);
                titleView.setCompoundDrawablePadding((int) (8 * activity.getResources().getDisplayMetrics().density));
                titleView.setOnClickListener(v -> showRenameDialog(activity, mPackageName, appEntry));
            }
        }
    }

    private void showRenameDialog(Activity activity, String packageName, AppEntry appEntry) {
        if (activity == null || packageName == null) return;

        final EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setHint(R.string.custom_app_name_hint);

        String currentCustom = Settings.System.getString(
                activity.getContentResolver(), "custom_app_label_" + packageName);
        if (currentCustom != null && !currentCustom.isEmpty()) {
            input.setText(currentCustom);
            input.setSelection(currentCustom.length());
        } else if (appEntry != null && appEntry.label != null) {
            input.setText(appEntry.label);
            input.setSelection(appEntry.label.length());
        }

        FrameLayout container = new FrameLayout(activity);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = (int) (24 * activity.getResources().getDisplayMetrics().density);
        params.rightMargin = params.leftMargin;
        params.topMargin = (int) (8 * activity.getResources().getDisplayMetrics().density);
        params.bottomMargin = (int) (8 * activity.getResources().getDisplayMetrics().density);
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(activity)
                .setTitle(R.string.custom_app_name_title)
                .setView(container)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        Settings.System.putString(
                                activity.getContentResolver(),
                                "custom_app_label_" + packageName,
                                newName);
                        if (appEntry != null) {
                            appEntry.label = newName;
                        }
                        refreshUi();
                        Toast.makeText(activity, R.string.custom_app_name_saved, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton(R.string.custom_app_name_reset, (dialog, which) -> {
                    Settings.System.putString(
                            activity.getContentResolver(),
                            "custom_app_label_" + packageName,
                            null);
                    if (appEntry != null) {
                        appEntry.label = null;
                    }
                    refreshUi();
                    Toast.makeText(activity, R.string.custom_app_name_reset_done, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
