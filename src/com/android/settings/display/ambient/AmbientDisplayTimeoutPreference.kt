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

package com.android.settings.display.ambient

import android.content.Context
import android.provider.Settings.Secure.DOZE_ALWAYS_ON
import android.provider.Settings.Secure.DOZE_ALWAYS_ON_TIMEOUT
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.shared.data.StringToIntDataStoreWrapper
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.DiscreteStringValue
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.preference.PreferenceBinding

class AmbientDisplayTimeoutPreference(context: Context) :
    PersistentPreference<String>,
    DiscreteStringValue,
    PreferenceBinding,
    PreferenceSummaryProvider,
    Preference.OnPreferenceChangeListener {

    private val dataStore by lazy {
        StringToIntDataStoreWrapper(
            SettingsSecureStore.get(context).apply {
                setDefaultValue(KEY, DEFAULT_TIMEOUT)
            }
        )
    }

    private val dozeAlwaysOnDataStore = AmbientDisplayStorage(context)

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.doze_always_on_timeout_purpose

    override val title: Int
        get() = R.string.doze_always_on_timeout_title

    override val values: Int
        get() = R.array.doze_always_on_timeout_values

    override val valuesDescription: Int
        get() = R.array.doze_always_on_timeout_entries

    override val valueType: Class<String>
        get() = String::class.javaObjectType

    override val sensitivityLevel: Int
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun dependencies(context: Context) = arrayOf(AmbientDisplayMainSwitchPreference.KEY)

    override fun getEnabledDescription(): String = "Always-on display must be enabled."

    override fun getEnabledStability() = PreconditionStability.UNSTABLE

    override fun isEnabled(context: Context) =
        dozeAlwaysOnDataStore.getBoolean(DOZE_ALWAYS_ON) == true

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    override fun getSummary(context: Context): CharSequence? = "%s"

    override fun createWidget(context: Context) = DropDownPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference as DropDownPreference
        val currentValue = storage(preference.context).getString(KEY) ?: DEFAULT_TIMEOUT.toString()
        preference.setValue(currentValue)
        preference.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (newValue == (preference as DropDownPreference).value) {
            return false
        }
        val strVal = newValue as? String ?: return false
        preference.setValue(strVal)
        storage(preference.context).setString(KEY, strVal)
        return true
    }

    companion object {
        const val KEY = DOZE_ALWAYS_ON_TIMEOUT
        private const val DEFAULT_TIMEOUT = 0
    }
}
