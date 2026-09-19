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

package com.android.settings.connecteddevice.usb;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Switch;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.GlifLayout;

/**
 * Setup Wizard activity displayed during initial setup to configure
 * default USB behavior and USB Debugging (ADB).
 */
public class UsbSetupWizardActivity extends FragmentActivity {

    public static final String PREFS_NAME = "usb_setup_prefs";
    public static final String KEY_COMPLETED = "usb_setup_completed";

    private UsbBackend mUsbBackend;
    private Switch mAdbSwitch;
    private RadioGroup mRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(SetupWizardUtils.getTheme(this, getIntent()));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.usb_setup_wizard_activity);

        mUsbBackend = new UsbBackend(this);
        mAdbSwitch = findViewById(R.id.usb_adb_switch);
        mRadioGroup = findViewById(R.id.usb_functions_radio_group);

        // 1. Initial ADB state
        boolean adbEnabled = Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED, 0) != 0;
        if (mAdbSwitch != null) {
            mAdbSwitch.setChecked(adbEnabled);
        }

        View adbContainer = findViewById(R.id.adb_container);
        if (adbContainer != null && mAdbSwitch != null) {
            adbContainer.setOnClickListener(v -> mAdbSwitch.toggle());
        }

        // 2. Initial Default USB Function state
        long defaultFunctions = mUsbBackend.getDefaultUsbFunctions();
        if ((defaultFunctions & UsbManager.FUNCTION_MTP) != 0) {
            mRadioGroup.check(R.id.usb_function_mtp);
        } else if ((defaultFunctions & UsbManager.FUNCTION_RNDIS) != 0) {
            mRadioGroup.check(R.id.usb_function_tethering);
        } else if ((defaultFunctions & UsbManager.FUNCTION_PTP) != 0) {
            mRadioGroup.check(R.id.usb_function_ptp);
        } else if ((defaultFunctions & UsbManager.FUNCTION_UVC) != 0) {
            mRadioGroup.check(R.id.usb_function_webcam);
        } else {
            mRadioGroup.check(R.id.usb_function_none);
        }

        // 3. Footer Bar Mixin ("Next" / "Done")
        GlifLayout glifLayout = findViewById(R.id.setup_wizard_layout);
        if (glifLayout != null) {
            FooterBarMixin footerBarMixin = glifLayout.getMixin(FooterBarMixin.class);
            if (footerBarMixin != null) {
                footerBarMixin.setPrimaryButton(
                        new FooterButton.Builder(this)
                                .setText(R.string.next_label)
                                .setListener(this::onNextClicked)
                                .setButtonType(FooterButton.ButtonType.NEXT)
                                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                                .build()
                );
            }
        }
    }

    private void onNextClicked(View view) {
        // Save ADB setting
        if (mAdbSwitch != null) {
            boolean enableAdb = mAdbSwitch.isChecked();
            Settings.Global.putInt(getContentResolver(), Settings.Global.ADB_ENABLED, enableAdb ? 1 : 0);
        }

        // Save Default USB Function
        int checkedId = mRadioGroup != null ? mRadioGroup.getCheckedRadioButtonId() : -1;
        long targetFunction = UsbManager.FUNCTION_NONE;
        if (checkedId == R.id.usb_function_mtp) {
            targetFunction = UsbManager.FUNCTION_MTP;
        } else if (checkedId == R.id.usb_function_tethering) {
            targetFunction = UsbManager.FUNCTION_RNDIS;
        } else if (checkedId == R.id.usb_function_ptp) {
            targetFunction = UsbManager.FUNCTION_PTP;
        } else if (checkedId == R.id.usb_function_webcam) {
            targetFunction = UsbManager.FUNCTION_UVC;
        }

        if (mUsbBackend != null) {
            mUsbBackend.setDefaultUsbFunctions(targetFunction);
        }

        // Mark completed in preferences
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_COMPLETED, true)
                .apply();

        setResult(RESULT_OK);
        finish();
    }
}
