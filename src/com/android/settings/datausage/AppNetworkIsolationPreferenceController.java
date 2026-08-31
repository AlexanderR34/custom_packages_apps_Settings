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

public class AppNetworkIsolationPreferenceController extends TogglePreferenceController {

    private final NetworkPolicyManager mPolicyManager;
    private int mUid;

    public AppNetworkIsolationPreferenceController(Context context, String key) {
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
            return false;
        }
        int policy = mPolicyManager.getUidPolicy(mUid);
        int isolationMask = NetworkPolicyManager.POLICY_REJECT_CELLULAR
                | NetworkPolicyManager.POLICY_REJECT_WIFI;
        return (policy & isolationMask) == isolationMask;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mPolicyManager == null || mUid == 0) {
            return false;
        }
        int policy = mPolicyManager.getUidPolicy(mUid);
        int isolationMask = NetworkPolicyManager.POLICY_REJECT_CELLULAR
                | NetworkPolicyManager.POLICY_REJECT_WIFI;
        if (isChecked) {
            policy |= isolationMask;
        } else {
            policy &= ~isolationMask;
        }
        mPolicyManager.setUidPolicy(mUid, policy);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
