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

package com.android.settings.applock;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AppLockSettingsFragment extends InstrumentedFragment {

    private TextView tvPinStatus;
    private Button btnSetPin;
    private EditText searchBox;
    private RecyclerView recyclerView;
    private AppLockListAdapter adapter;

    @Override
    public int getMetricsCategory() {
        return METRICS_CATEGORY_UNKNOWN;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.app_lock_settings_fragment, container, false);

        tvPinStatus = view.findViewById(R.id.tv_pin_status);
        btnSetPin = view.findViewById(R.id.btn_set_pin);
        searchBox = view.findViewById(R.id.search_box);
        recyclerView = view.findViewById(R.id.apps_recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        updatePinStatus();

        btnSetPin.setOnClickListener(v -> showSetPinDialog());

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadApps();

        return view;
    }

    private void updatePinStatus() {
        String pin = Settings.Secure.getString(requireContext().getContentResolver(), "app_lock_password");
        if (pin != null && !pin.isEmpty()) {
            tvPinStatus.setText(R.string.app_lock_security_pin_configured);
            btnSetPin.setText(R.string.app_lock_change_pin_button);
        } else {
            tvPinStatus.setText(R.string.app_lock_security_pin_summary);
            btnSetPin.setText(R.string.app_lock_set_pin_button);
        }
    }

    private void showSetPinDialog() {
        Context context = requireContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.app_lock_security_pin_title);

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Ej. 1234");
        builder.setView(input);

        builder.setPositiveButton(R.string.reboot_prompt_now, (dialog, which) -> {
            String pin = input.getText().toString().trim();
            if (pin.length() >= 4) {
                Settings.Secure.putString(context.getContentResolver(), "app_lock_password", pin);
                Toast.makeText(context, R.string.app_lock_pin_saved, Toast.LENGTH_SHORT).show();
                updatePinStatus();
            } else {
                Toast.makeText(context, R.string.app_lock_pin_too_short, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.reboot_prompt_later, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void loadApps() {
        new AsyncTask<Void, Void, List<AppLockItem>>() {
            @Override
            protected List<AppLockItem> doInBackground(Void... voids) {
                Context context = getContext();
                if (context == null) return new ArrayList<>();

                PackageManager pm = context.getPackageManager();
                List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
                Set<String> lockedSet = AppLockListAdapter.loadLockedApps(context);

                List<AppLockItem> items = new ArrayList<>();
                for (ApplicationInfo appInfo : packages) {
                    // Ignorar la propia app de Ajustes
                    if (appInfo.packageName.equals(context.getPackageName())) {
                        continue;
                    }
                    String label = pm.getApplicationLabel(appInfo).toString();
                    boolean isLocked = lockedSet.contains(appInfo.packageName);
                    int enabledState = pm.getApplicationEnabledSetting(appInfo.packageName);
                    boolean isHidden = (enabledState == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER);

                    items.add(new AppLockItem(
                            appInfo.packageName,
                            label,
                            pm.getApplicationIcon(appInfo),
                            isLocked,
                            isHidden
                    ));
                }

                items.sort((a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName()));
                return items;
            }

            @Override
            protected void onPostExecute(List<AppLockItem> items) {
                if (getContext() != null) {
                    adapter = new AppLockListAdapter(getContext(), items);
                    recyclerView.setAdapter(adapter);
                }
            }
        }.execute();
    }
}
