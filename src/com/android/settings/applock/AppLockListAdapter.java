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

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppLockListAdapter extends RecyclerView.Adapter<AppLockListAdapter.ViewHolder> {

    private final Context context;
    private final List<AppLockItem> fullList;
    private final List<AppLockItem> filteredList;
    private final Set<String> lockedAppsSet;

    public AppLockListAdapter(Context context, List<AppLockItem> items) {
        this.context = context;
        this.fullList = items;
        this.filteredList = new ArrayList<>(items);
        this.lockedAppsSet = loadLockedApps(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.app_lock_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppLockItem item = filteredList.get(position);
        holder.appName.setText(item.getAppName());
        holder.appPackage.setText(item.getPackageName());
        holder.appIcon.setImageDrawable(item.getIcon());

        updateLockIcon(holder.btnLock, item.isLocked());
        updateEyeIcon(holder.btnHide, item.isHidden());

        // Alternar bloqueo (Candado abierto/cerrado)
        holder.btnLock.setOnClickListener(v -> {
            boolean newLockState = !item.isLocked();
            item.setLocked(newLockState);
            updateLockIcon(holder.btnLock, newLockState);
            toggleAppLock(item.getPackageName(), newLockState);
        });

        // Alternar ocultación del lanzador (Ojo visible/no visible)
        holder.btnHide.setOnClickListener(v -> {
            boolean newHideState = !item.isHidden();
            item.setHidden(newHideState);
            updateEyeIcon(holder.btnHide, newHideState);
            toggleAppHide(item.getPackageName(), newHideState);
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(fullList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (AppLockItem item : fullList) {
                if (item.getAppName().toLowerCase().contains(lowerQuery)
                        || item.getPackageName().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    private void updateLockIcon(ImageButton btn, boolean isLocked) {
        btn.setImageResource(isLocked ? R.drawable.ic_app_lock_closed : R.drawable.ic_app_lock_open);
    }

    private void updateEyeIcon(ImageButton btn, boolean isHidden) {
        btn.setImageResource(isHidden ? R.drawable.ic_app_eye_hidden : R.drawable.ic_app_eye_visible);
    }

    private void toggleAppLock(String pkg, boolean lock) {
        if (lock) {
            lockedAppsSet.add(pkg);
        } else {
            lockedAppsSet.remove(pkg);
        }
        String joined = String.join(",", lockedAppsSet);
        Settings.Secure.putString(context.getContentResolver(), "app_lock_list", joined);
    }

    private void toggleAppHide(String pkg, boolean hide) {
        try {
            int state = hide
                    ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                    : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
            context.getPackageManager().setApplicationEnabledSetting(pkg, state, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Set<String> loadLockedApps(Context context) {
        String raw = Settings.Secure.getString(context.getContentResolver(), "app_lock_list");
        if (raw == null || raw.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(raw.split(",")));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        TextView appPackage;
        ImageButton btnLock;
        ImageButton btnHide;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            appPackage = itemView.findViewById(R.id.app_package);
            btnLock = itemView.findViewById(R.id.btn_lock);
            btnHide = itemView.findViewById(R.id.btn_hide);
        }
    }
}
