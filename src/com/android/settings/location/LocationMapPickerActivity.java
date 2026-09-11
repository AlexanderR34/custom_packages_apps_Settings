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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
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

    private WebView mWebView;
    private TextView mTvCoords;
    private TextView mTvAppTitle;
    private TextView mTvAppPackage;

    private double mCurrentLat = 40.7128;
    private double mCurrentLng = -74.0060;

    @SuppressLint("SetJavaScriptEnabled")
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
        mTvCoords = findViewById(R.id.tv_current_coords);
        mWebView = findViewById(R.id.map_webview);

        if (!TextUtils.isEmpty(mAppTitle)) {
            mTvAppTitle.setText(mAppTitle);
        }
        mTvAppPackage.setText(mPackageName);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

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
                    mCurrentLat = Double.parseDouble(parts[0].trim());
                    mCurrentLng = Double.parseDouble(parts[1].trim());
                }
            } catch (Exception ignored) {}
        }

        updateCoordsDisplay(mCurrentLat, mCurrentLng);

        // Setup WebView with Leaflet OpenStreetMap
        if (mWebView != null) {
            try {
                WebSettings webSettings = mWebView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setAllowFileAccess(true);
                webSettings.setLoadsImagesAutomatically(true);

                mWebView.addJavascriptInterface(new MapBridge(), "AndroidBridge");
                mWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        setMapLocation(mCurrentLat, mCurrentLng, 14);
                    }
                });

                loadMapHtml();
            } catch (Exception e) {
                android.util.Log.e("LocationMapPicker", "Failed to initialize WebView: " + e.getMessage(), e);
            }
        }

        // Preset chips
        setupPreset(R.id.btn_preset_tokyo, 35.6895, 139.6917);
        setupPreset(R.id.btn_preset_ny, 40.7128, -74.0060);
        setupPreset(R.id.btn_preset_paris, 48.8566, 2.3522);
        setupPreset(R.id.btn_preset_madrid, 40.4168, -3.7038);
        setupPreset(R.id.btn_preset_cdmx, 19.4326, -99.1332);
        setupPreset(R.id.btn_preset_london, 51.5074, -0.1278);

        // Save Button
        Button btnSave = findViewById(R.id.btn_save_location);
        btnSave.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
            String coordStr = String.format(Locale.US, "%.6f,%.6f,15.0,3.5", mCurrentLat, mCurrentLng);
            Settings.Secure.putStringForUser(
                    getContentResolver(),
                    "fake_loc_coords_" + mPackageName,
                    coordStr,
                    UserHandle.USER_CURRENT);
            Settings.Secure.putIntForUser(
                    getContentResolver(),
                    "fake_loc_enabled_" + mPackageName,
                    1,
                    UserHandle.USER_CURRENT);

            Settings.Secure.putStringForUser(
                    getContentResolver(),
                    SETTING_SPOOF_COORDS_PREFIX + mPackageName,
                    coordStr,
                    UserHandle.USER_CURRENT);
            Settings.Secure.putIntForUser(
                    getContentResolver(),
                    SETTING_SPOOF_PKG_PREFIX + mPackageName,
                    1,
                    UserHandle.USER_CURRENT);

            Toast.makeText(this, getString(R.string.location_spoof_saved_toast,
                    !TextUtils.isEmpty(mAppTitle) ? mAppTitle : mPackageName), Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });

        // Clear / Reset Button
        Button btnClear = findViewById(R.id.btn_clear_location);
        btnClear.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
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

            Toast.makeText(this, R.string.location_spoof_cleared_toast, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });
    }

    private void setupPreset(int btnId, double lat, double lng) {
        Button btn = findViewById(btnId);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                setMapLocation(lat, lng, 14);
            });
        }
    }

    private void setMapLocation(double lat, double lng, int zoom) {
        mCurrentLat = lat;
        mCurrentLng = lng;
        updateCoordsDisplay(lat, lng);
        mWebView.post(() -> mWebView.evaluateJavascript(
                String.format(Locale.US, "if (window.map) { window.map.setView([%.6f, %.6f], %d); }", lat, lng, zoom),
                null));
    }

    private void updateCoordsDisplay(double lat, double lng) {
        if (mTvCoords != null) {
            mTvCoords.setText(String.format(Locale.US, "%.6f, %.6f", lat, lng));
        }
    }

    private class MapBridge {
        @JavascriptInterface
        public void onCenterChanged(double lat, double lng) {
            runOnUiThread(() -> {
                mCurrentLat = lat;
                mCurrentLng = lng;
                updateCoordsDisplay(lat, lng);
            });
        }
    }

    private void loadMapHtml() {
        String html = "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'/>\n"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>\n"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>\n"
                + "<style>\n"
                + "  html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #1a1a1a; }\n"
                + "</style>\n"
                + "</head>\n"
                + "<body>\n"
                + "<div id='map'></div>\n"
                + "<script>\n"
                + "  var map = L.map('map', { zoomControl: false, attributionControl: false }).setView([" + mCurrentLat + ", " + mCurrentLng + "], 14);\n"
                + "  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {\n"
                + "    maxZoom: 19\n"
                + "  }).addTo(map);\n"
                + "  map.on('move', function() {\n"
                + "    var center = map.getCenter();\n"
                + "    if (window.AndroidBridge && window.AndroidBridge.onCenterChanged) {\n"
                + "      window.AndroidBridge.onCenterChanged(center.lat, center.lng);\n"
                + "    }\n"
                + "  });\n"
                + "</script>\n"
                + "</body>\n"
                + "</html>";

        mWebView.loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null);
    }
}
