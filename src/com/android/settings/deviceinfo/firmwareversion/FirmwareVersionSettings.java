/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.deviceinfo.firmwareversion;

import android.animation.ValueAnimator;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.flags.Flags;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.LayoutPreference;
import com.google.android.material.card.MaterialCardView;

@SearchIndexable
public class FirmwareVersionSettings extends DashboardFragment {

    private ValueAnimator clock;

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@NonNull Context context) {
        return FirmwareVersionScreen.KEY;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.firmware_version;
    }

    @Override
    protected String getLogTag() {
        return "FirmwareVersionSettings";
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_FIRMWARE_VERSION;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Extraemos el contenedor principal usando la Key que pusimos en el XML
        LayoutPreference customLayoutPref = findPreference("project_diva_custom_layout");
        if (customLayoutPref != null) {
            View root = customLayoutPref.findViewById(R.id.main);
            if (root != null) {
                // Inicializamos la animación del destello y los clicks usando el root
                setupAndroidVersionMenu(root);
                setupKernelEasterEgg(root);
                animateGlowRandomly(root);
                
                // Cargar la información del dispositivo real
                populateDynamicData(root);
            }
        }
    }

    private void setupKernelEasterEgg(View root) {
        View btnKernel = root.findViewById(R.id.btn_kernel_version);
        if (btnKernel == null) {
            btnKernel = root.findViewById(R.id.tv_kernel_version);
        }
        if (btnKernel != null) {
            final long[] kernelHits = new long[3];
            btnKernel.setOnClickListener(view -> {
                System.arraycopy(kernelHits, 1, kernelHits, 0, kernelHits.length - 1);
                kernelHits[kernelHits.length - 1] = android.os.SystemClock.uptimeMillis();
                if (kernelHits[0] >= (android.os.SystemClock.uptimeMillis() - 600)) {
                    Context context = getContext();
                    if (context != null) {
                        try {
                            Intent directIntent = new Intent(Intent.ACTION_MAIN);
                            directIntent.setClassName("org.mupen64plusae.v3.alpha", "paulscode.android.mupen64plusae.SplashActivity");
                            directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            context.startActivity(directIntent);
                        } catch (Exception e) {
                            try {
                                Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage("org.mupen64plusae.v3.alpha");
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(launchIntent);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            });
        }
    }

    private void populateDynamicData(View root) {
        TextView tvDevice = root.findViewById(R.id.tv_device_model);
        if (tvDevice != null) tvDevice.setText(Build.MANUFACTURER + " " + Build.MODEL + " | " + Build.DEVICE);

        TextView tvAndroid = root.findViewById(R.id.tv_android_version);
        if (tvAndroid != null) tvAndroid.setText(Build.VERSION.RELEASE);

        TextView tvDiva = root.findViewById(R.id.tv_diva_version);
        if (tvDiva != null) tvDiva.setText(SystemProperties.get("ro.diva.version", "1.0 DESTELLO AZUL"));

        TextView tvMaintainer = root.findViewById(R.id.tv_maintainer);
        // Utilizando tu nombre como fallback en caso de que no exista la propiedad
        if (tvMaintainer != null) tvMaintainer.setText(SystemProperties.get("ro.diva.maintainer", "Alexander Reyes"));

        TextView tvSecurity = root.findViewById(R.id.tv_security_patch);
        if (tvSecurity != null) tvSecurity.setText(DeviceInfoUtils.getSecurityPatch());

        TextView tvVendorSecurity = root.findViewById(R.id.tv_vendor_security_patch);
        if (tvVendorSecurity != null) tvVendorSecurity.setText(SystemProperties.get("ro.vendor.build.security_patch", "No disponible"));

        TextView tvBaseband = root.findViewById(R.id.tv_baseband);
        if (tvBaseband != null) tvBaseband.setText(SystemProperties.get("gsm.version.baseband", "Desconocido"));

        TextView tvKernel = root.findViewById(R.id.tv_kernel_version);
        if (tvKernel != null) tvKernel.setText(DeviceInfoUtils.getFormattedKernelVersion(getContext()));

        TextView tvBuildDate = root.findViewById(R.id.tv_build_date);
        if (tvBuildDate != null) tvBuildDate.setText(SystemProperties.get("ro.build.date", ""));

        TextView tvBuildNumber = root.findViewById(R.id.tv_build_number);
        if (tvBuildNumber != null) tvBuildNumber.setText(Build.DISPLAY);
    }

    private void setupAndroidVersionMenu(View root) {
        MaterialCardView btnAndroidVersion = root.findViewById(R.id.btn_android_version);
        if (btnAndroidVersion != null) {
            btnAndroidVersion.setOnClickListener(view -> {
                PopupMenu popupMenu = new PopupMenu(getContext(), view);
                popupMenu.getMenuInflater().inflate(R.menu.menu_android_version, popupMenu.getMenu());
                
                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getItemId() == R.id.action_easter_egg) {
                        // Lanza el Easter Egg (PlatLogoActivity) nativo del sistema
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.setClassName("android", "com.android.internal.app.PlatLogoActivity");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return true;
                    } else if (menuItem.getItemId() == R.id.action_system_info) {
                        // Espacio libre para lógica o diálogos extra
                        return true;
                    }
                    return false;
                });
                popupMenu.show();
            });
        }
    }

    private void animateGlowRandomly(View root) {
        final View glowView = root.findViewById(R.id.glow_view);
        if (glowView == null) return;

        glowView.post(() -> {
            clock = ValueAnimator.ofFloat(0f, 1000000f);
            clock.setDuration(1000000);
            clock.setRepeatCount(ValueAnimator.INFINITE);
            clock.setInterpolator(new LinearInterpolator());

            clock.addUpdateListener(animation -> {
                float t = (float) animation.getAnimatedValue();
                
                float x = (float) (350f * Math.sin(t / 1500.0));
                float y = (float) (250f * Math.cos(t / 2100.0));
                float scale = (float) (1.2f + 0.3f * Math.sin(t / 2700.0));

                glowView.setTranslationX(x);
                glowView.setTranslationY(y);
                glowView.setScaleX(scale);
                glowView.setScaleY(scale);
                glowView.setRotation(t / 100f);
            });
            
            clock.start();
        });
    }

    @Override
    public void onDestroyView() {
        // Frenamos la animación infinitamente repetida al salir para evitar fugas de memoria
        if (clock != null) {
            clock.cancel();
        }
        super.onDestroyView();
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(
                    Flags.catalystSettingsSearch() ? 0 : R.xml.firmware_version);
}
