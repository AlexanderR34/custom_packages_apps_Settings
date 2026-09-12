/*
 * Copyright (C) 2026 Project Diva
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

package com.android.settings.location;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;

import java.util.Locale;

public class LocationMapPickerActivity extends Activity {

    public static final String EXTRA_PACKAGE_NAME = "extra_package_name";
    public static final String EXTRA_APP_TITLE = "extra_app_title";

    private static final String SETTING_SPOOF_PKG_PREFIX = "location_spoof_pkg_";
    private static final String SETTING_SPOOF_COORDS_PREFIX = "location_spoof_coords_";

    private String mPackageName;
    private String mAppTitle;

    private TextView mTvAppTitle;
    private TextView mTvAppPackage;
    private TextView mTvSwitchStatus;
    private ImageView mIvAppIcon;
    private Switch mSwitchSpoofEnable;

    private EditText mEtLatitude;
    private EditText mEtLongitude;
    private EditText mEtAltitude;
    private EditText mEtAccuracy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_map_picker);

        mPackageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        mAppTitle = getIntent().getStringExtra(EXTRA_APP_TITLE);

        if (TextUtils.isEmpty(mPackageName)) {
            finish();
            return;
        }

        mTvAppTitle = findViewById(R.id.tv_app_title);
        mTvAppPackage = findViewById(R.id.tv_app_package);
        mTvSwitchStatus = findViewById(R.id.tv_switch_status);
        mIvAppIcon = findViewById(R.id.iv_app_icon);
        mSwitchSpoofEnable = findViewById(R.id.switch_spoof_enable);

        mEtLatitude = findViewById(R.id.et_latitude);
        mEtLongitude = findViewById(R.id.et_longitude);
        mEtAltitude = findViewById(R.id.et_altitude);
        mEtAccuracy = findViewById(R.id.et_accuracy);

        if (!TextUtils.isEmpty(mAppTitle)) {
            mTvAppTitle.setText(mAppTitle);
        } else {
            mTvAppTitle.setText(mPackageName);
        }
        mTvAppPackage.setText(mPackageName);

        // Load app icon
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(mPackageName, 0);
            Drawable icon = appInfo.loadIcon(pm);
            if (icon != null && mIvAppIcon != null) {
                mIvAppIcon.setImageDrawable(icon);
            }
        } catch (Exception ignored) {}

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Check if spoofing is currently enabled for this app
        boolean isSpoofEnabled = Settings.Secure.getIntForUser(
                getContentResolver(),
                "fake_loc_enabled_" + mPackageName,
                -1,
                UserHandle.USER_CURRENT) == 1;
        if (!isSpoofEnabled && Settings.Secure.getIntForUser(
                getContentResolver(),
                "fake_loc_enabled_" + mPackageName,
                -1,
                UserHandle.USER_CURRENT) == -1) {
            isSpoofEnabled = Settings.Secure.getIntForUser(
                    getContentResolver(),
                    SETTING_SPOOF_PKG_PREFIX + mPackageName,
                    0,
                    UserHandle.USER_CURRENT) == 1;
        }

        mSwitchSpoofEnable.setChecked(isSpoofEnabled);
        updateSwitchStatusText(isSpoofEnabled);

        mSwitchSpoofEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateSwitchStatusText(isChecked);
        });

        // Load existing coordinates if available
        String savedCoords = Settings.Secure.getStringForUser(
                getContentResolver(),
                "fake_loc_coords_" + mPackageName,
                UserHandle.USER_CURRENT);
        if (TextUtils.isEmpty(savedCoords)) {
            savedCoords = Settings.Secure.getStringForUser(
                    getContentResolver(),
                    SETTING_SPOOF_COORDS_PREFIX + mPackageName,
                    UserHandle.USER_CURRENT);
        }

        if (!TextUtils.isEmpty(savedCoords)) {
            try {
                String[] parts = savedCoords.split(",");
                if (parts.length >= 2) {
                    mEtLatitude.setText(parts[0].trim());
                    mEtLongitude.setText(parts[1].trim());
                    if (parts.length > 2) mEtAltitude.setText(parts[2].trim());
                    if (parts.length > 3) mEtAccuracy.setText(parts[3].trim());
                }
            } catch (Exception ignored) {}
        } else {
            // Default preset
            setPresetCoords(40.712800, -74.006000, 10.0, 5.0);
        }

        // Setup preset chips
        setupPreset(R.id.btn_preset_tokyo, 35.689500, 139.691700, 40.0, 5.0);
        setupPreset(R.id.btn_preset_ny, 40.712800, -74.006000, 10.0, 5.0);
        setupPreset(R.id.btn_preset_paris, 48.856600, 2.352200, 35.0, 5.0);
        setupPreset(R.id.btn_preset_madrid, 40.416800, -3.703800, 650.0, 5.0);
        setupPreset(R.id.btn_preset_cdmx, 19.432600, -99.133200, 2240.0, 5.0);
        setupPreset(R.id.btn_preset_london, 51.507400, -0.127800, 15.0, 5.0);
        setupPreset(R.id.btn_preset_sf, 37.774900, -122.419400, 16.0, 5.0);
        setupPreset(R.id.btn_preset_rome, 41.902800, 12.496400, 21.0, 5.0);
        setupPreset(R.id.btn_preset_buenosaires, -34.603700, -58.381600, 25.0, 5.0);
        setupPreset(R.id.btn_preset_sydney, -33.868800, 151.209300, 20.0, 5.0);

        // Save Button
        Button btnSave = findViewById(R.id.btn_save_location);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                saveLocationSettings();
            });
        }

        // Clear / Reset Button
        Button btnClear = findViewById(R.id.btn_clear_location);
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                clearLocationSettings();
            });
        }
    }

    private void updateSwitchStatusText(boolean isEnabled) {
        if (mTvSwitchStatus != null) {
            mTvSwitchStatus.setText(isEnabled
                    ? R.string.location_spoof_status_active_no_coords
                    : R.string.location_spoof_status_disabled);
        }
    }

    private void setupPreset(int btnId, double lat, double lng, double alt, double acc) {
        Button btn = findViewById(btnId);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                setPresetCoords(lat, lng, alt, acc);
            });
        }
    }

    private void setPresetCoords(double lat, double lng, double alt, double acc) {
        mEtLatitude.setText(String.format(Locale.US, "%.6f", lat));
        mEtLongitude.setText(String.format(Locale.US, "%.6f", lng));
        mEtAltitude.setText(String.format(Locale.US, "%.1f", alt));
        mEtAccuracy.setText(String.format(Locale.US, "%.1f", acc));
        if (mSwitchSpoofEnable != null && !mSwitchSpoofEnable.isChecked()) {
            mSwitchSpoofEnable.setChecked(true);
        }
    }

    private void saveLocationSettings() {
        String latStr = mEtLatitude.getText().toString().trim();
        String lngStr = mEtLongitude.getText().toString().trim();
        String altStr = mEtAltitude.getText().toString().trim();
        String accStr = mEtAccuracy.getText().toString().trim();

        if (TextUtils.isEmpty(latStr) || TextUtils.isEmpty(lngStr)) {
            Toast.makeText(this, "Por favor ingresa latitud y longitud válidas", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double lat = Double.parseDouble(latStr);
            double lng = Double.parseDouble(lngStr);
            if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) {
                Toast.makeText(this, "Coordenadas fuera de rango válido", Toast.LENGTH_SHORT).show();
                return;
            }

            double alt = TextUtils.isEmpty(altStr) ? 15.0 : Double.parseDouble(altStr);
            float acc = TextUtils.isEmpty(accStr) ? 3.5f : Float.parseFloat(accStr);

            String coordStr = String.format(Locale.US, "%.6f,%.6f,%.1f,%.1f", lat, lng, alt, acc);
            boolean isEnabled = mSwitchSpoofEnable.isChecked();

            Settings.Secure.putStringForUser(
                    getContentResolver(),
                    "fake_loc_coords_" + mPackageName,
                    coordStr,
                    UserHandle.USER_CURRENT);
            Settings.Secure.putIntForUser(
                    getContentResolver(),
                    "fake_loc_enabled_" + mPackageName,
                    isEnabled ? 1 : 0,
                    UserHandle.USER_CURRENT);

            Settings.Secure.putStringForUser(
                    getContentResolver(),
                    SETTING_SPOOF_COORDS_PREFIX + mPackageName,
                    coordStr,
                    UserHandle.USER_CURRENT);
            Settings.Secure.putIntForUser(
                    getContentResolver(),
                    SETTING_SPOOF_PKG_PREFIX + mPackageName,
                    isEnabled ? 1 : 0,
                    UserHandle.USER_CURRENT);

            String displayName = !TextUtils.isEmpty(mAppTitle) ? mAppTitle : mPackageName;
            Toast.makeText(this, getString(R.string.location_spoof_saved_toast, displayName), Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Formato numérico inválido", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearLocationSettings() {
        Settings.Secure.putStringForUser(
                getContentResolver(),
                "fake_loc_coords_" + mPackageName,
                "",
                UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(
                getContentResolver(),
                "fake_loc_enabled_" + mPackageName,
                0,
                UserHandle.USER_CURRENT);

        Settings.Secure.putStringForUser(
                getContentResolver(),
                SETTING_SPOOF_COORDS_PREFIX + mPackageName,
                "",
                UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(
                getContentResolver(),
                SETTING_SPOOF_PKG_PREFIX + mPackageName,
                0,
                UserHandle.USER_CURRENT);

        mSwitchSpoofEnable.setChecked(false);
        Toast.makeText(this, R.string.location_spoof_cleared_toast, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}
