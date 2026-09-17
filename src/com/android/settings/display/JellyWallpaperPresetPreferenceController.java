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

package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

public class JellyWallpaperPresetPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public static final String KEY = "jelly_wallpaper_preset";
    public static final String SETTING_KEY = "jelly_wallpaper_preset";
    public static final int DEFAULT_PRESET = 0;
    public static final int PRESET_CUSTOM = 8;

    public JellyWallpaperPresetPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        if (preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            int preset = Settings.System.getInt(mContext.getContentResolver(), SETTING_KEY, DEFAULT_PRESET);
            listPref.setValue(String.valueOf(preset));
            listPref.setSummary(listPref.getEntry());

            boolean isEnabled = Settings.System.getInt(
                    mContext.getContentResolver(), "jelly_wallpaper_enabled", 0) != 0;
            listPref.setEnabled(isEnabled);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int val = Integer.parseInt((String) newValue);
        Settings.System.putInt(mContext.getContentResolver(), SETTING_KEY, val);

        // Si se selecciona un preset predefinido, sincronizar los parámetros físicos
        if (val == 0) { // Gelatina clásica (Classic Jelly)
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_stiffness", 45);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_damping", 85);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_radius", 10);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_elasticity", 70);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_snapback_recoil", 75);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_internal_tension", 65);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_max_stretch", 70);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_surface_mass", 50);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_wave_speed", 70);
        } else if (val == 1) { // Goma firme (Firm Rubber)
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_stiffness", 80);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_damping", 70);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_radius", 10);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_elasticity", 40);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_snapback_recoil", 90);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_internal_tension", 80);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_max_stretch", 45);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_surface_mass", 75);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_wave_speed", 85);
        } else if (val == 2) { // Agua / Líquido fluido (Liquid / Water)
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_stiffness", 25);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_damping", 95);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_radius", 10);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_elasticity", 90);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_snapback_recoil", 40);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_internal_tension", 40);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_max_stretch", 90);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_surface_mass", 35);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_wave_speed", 95);
        } else if (val == 3) { // Muelle de acero (Steel Spring)
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_stiffness", 85);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_damping", 65);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_radius", 10);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_elasticity", 60);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_snapback_recoil", 80);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_internal_tension", 60);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_max_stretch", 50);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_surface_mass", 50);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_wave_speed", 75);
        } else if (val == 4) { // Seda / Tela colgante (Silk Cloth)
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_stiffness", 20);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_damping", 60);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_radius", 10);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_elasticity", 60);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_snapback_recoil", 30);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_internal_tension", 85);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_max_stretch", 80);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_surface_mass", 25);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_wave_speed", 50);
        } else if (val == 5) { // Miel / Slime viscoso (Viscous Slime / Honey)
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_stiffness", 30);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_damping", 120);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_radius", 10);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_elasticity", 30);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_snapback_recoil", 20);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_internal_tension", 30);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_max_stretch", 60);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_surface_mass", 100);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_wave_speed", 30);
        } else if (val == 6) { // Espuma viscoelástica (Memory Foam)
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_stiffness", 60);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_damping", 140);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_radius", 10);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_elasticity", 20);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_snapback_recoil", 15);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_internal_tension", 55);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_max_stretch", 40);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_surface_mass", 85);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_wave_speed", 20);
        } else if (val == 7) { // Cristal templado (Rigid Glass)
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_stiffness", 100);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_damping", 150);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_radius", 10);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_elasticity", 10);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_snapback_recoil", 100);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_internal_tension", 100);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_max_stretch", 15);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_surface_mass", 90);
            Settings.System.putInt(mContext.getContentResolver(), "jelly_wallpaper_wave_speed", 100);
        }

        if (preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            int index = listPref.findIndexOfValue((String) newValue);
            if (index >= 0) {
                listPref.setSummary(listPref.getEntries()[index]);
            }
        }
        return true;
    }
}
