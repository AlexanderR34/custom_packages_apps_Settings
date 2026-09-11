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

package com.android.settings.gestures

import android.content.Context
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.BooleanValuePreferenceBinding
import com.android.settingslib.preference.forEachRecursively

sealed class ButtonNavigationSettingsStylePreference(
    val store: ButtonNavigationSettingsStyleStore
) :
    BooleanValuePreference,
    BooleanValuePreferenceBinding,
    ButtonNavigationSettingsStyleRadioButton.OnClickListener {
    abstract val titleRes: Int
    abstract val icons: List<Int>
    abstract val labels: List<Int>

    override fun createWidget(context: Context) =
        ButtonNavigationSettingsStyleRadioButton(context, titleRes, icons, labels)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isPersistent = false
        (preference as ButtonNavigationSettingsStyleRadioButton).also {
            it.isChecked = store.getBoolean(key) == true
            it.listener = this
        }
        preference.isPersistent = true
    }

    override fun storage(context: Context): KeyValueStore = store

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun onRadioButtonClicked(source: ButtonNavigationSettingsStyleRadioButton) {
        source.parent?.forEachRecursively {
            if (it is ButtonNavigationSettingsStyleRadioButton) {
                it.isChecked = it == source
            }
        }
        store.setValue(key, Boolean::class.javaObjectType, true)
    }

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    override fun getReadPermissions(context: Context): Permissions? =
        ButtonNavigationSettingsStyleStore.readPermissions

    override fun getWritePermissions(context: Context): Permissions? =
        ButtonNavigationSettingsStyleStore.readPermissions

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override val indexable: Boolean = false
}

class DefaultButtonNavigationSettingsStylePreference(store: ButtonNavigationSettingsStyleStore) :
    ButtonNavigationSettingsStylePreference(store) {
    override val key
        get() = KEY

    override val purpose: Int
        get() = R.string.navbar_style_preference_default_purpose

    override val titleRes: Int
        get() = R.string.navbar_style_aosp

    override val icons
        get() =
            listOf(
                R.drawable.ic_sysbar_back,
                R.drawable.ic_sysbar_home,
                R.drawable.ic_sysbar_recents,
            )

    override val labels
        get() =
            listOf(
                R.string.navbar_back_button,
                R.string.navbar_home_button,
                R.string.navbar_recent_button,
            )

    companion object {
        const val KEY = "navbar_style_preference_default"
    }
}

class HyperOSButtonNavigationSettingsStylePreference(store: ButtonNavigationSettingsStyleStore) :
    ButtonNavigationSettingsStylePreference(store) {
    override val key
        get() = KEY

    override val purpose: Int
        get() = R.string.navbar_style_preference_hyperos_purpose

    override val titleRes: Int
        get() = R.string.navbar_style_hyperos

    override val icons
        get() =
            listOf(
                R.drawable.ic_sysbar_back_hyperos,
                R.drawable.ic_sysbar_home_hyperos,
                R.drawable.ic_sysbar_recent_hyperos,
            )

    override val labels
        get() =
            listOf(
                R.string.navbar_back_button,
                R.string.navbar_home_button,
                R.string.navbar_recent_button,
            )

    companion object {
        const val KEY = "navbar_style_preference_hyperos"
    }
}
