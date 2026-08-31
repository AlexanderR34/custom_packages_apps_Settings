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

package com.android.settings.datausage;

import android.content.Context;
import android.net.NetworkPolicyManager;

import com.android.settings.core.TogglePreferenceController;

/**
 * Controller for per-app cellular data and Wi-Fi network isolation restrictions.
 */
public class AppNetworkRestrictionPreferenceController extends TogglePreferenceController {

    private final NetworkPolicyManager mPolicyManager;
    private int mUid;

    public AppNetworkRestrictionPreferenceController(Context context, String key) {
        super(context, key);
        mPolicyManager = NetworkPolicyManager.from(context);
    }

    public void setUid(int uid) {
        mUid = uid;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        if (mPolicyManager == null || mUid == 0) {
            return true;
        }
        int policy = mPolicyManager.getUidPolicy(mUid);
        if (getPreferenceKey().equals("app_cellular_data_restriction")) {
            return (policy & NetworkPolicyManager.POLICY_REJECT_CELLULAR) == 0;
        } else if (getPreferenceKey().equals("app_wifi_data_restriction")) {
            return (policy & NetworkPolicyManager.POLICY_REJECT_WIFI) == 0;
        }
        return true;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mPolicyManager == null || mUid == 0) {
            return false;
        }
        int policy = mPolicyManager.getUidPolicy(mUid);
        if (getPreferenceKey().equals("app_cellular_data_restriction")) {
            if (isChecked) {
                policy &= ~NetworkPolicyManager.POLICY_REJECT_CELLULAR;
            } else {
                policy |= NetworkPolicyManager.POLICY_REJECT_CELLULAR;
            }
        } else if (getPreferenceKey().equals("app_wifi_data_restriction")) {
            if (isChecked) {
                policy &= ~NetworkPolicyManager.POLICY_REJECT_WIFI;
            } else {
                policy |= NetworkPolicyManager.POLICY_REJECT_WIFI;
            }
        }
        mPolicyManager.setUidPolicy(mUid, policy);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
