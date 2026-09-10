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
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.datastore.SettingsStore

@Suppress("UNCHECKED_CAST")
class ButtonNavigationSettingsStyleStore(val context: Context) :
    KeyValueStore, AbstractKeyedDataObservable<String>(), KeyedObserver<String> {
    private val settingsStore: SettingsStore = SettingsSecureStore.get(context)

    override fun contains(key: String) =
        key == DefaultButtonNavigationSettingsStylePreference.KEY ||
            key == HyperOSButtonNavigationSettingsStylePreference.KEY

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        val style = settingsStore.getInt(KEY) ?: 0
        return when (key) {
            DefaultButtonNavigationSettingsStylePreference.KEY -> (style == 0) as T?
            HyperOSButtonNavigationSettingsStylePreference.KEY -> (style == 1) as T?
            else -> false as T?
        }
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value !is Boolean || !value) return
        when (key) {
            DefaultButtonNavigationSettingsStylePreference.KEY ->
                settingsStore.setInt(KEY, 0)
            HyperOSButtonNavigationSettingsStylePreference.KEY ->
                settingsStore.setInt(KEY, 1)
        }
    }

    override fun onFirstObserverAdded() {
        settingsStore.addObserver(KEY, this, HandlerExecutor.main)
    }

    override fun onLastObserverRemoved() {
        settingsStore.removeObserver(KEY, this)
    }

    override fun onKeyChanged(key: String, reason: Int) {
        notifyChange(DataChangeReason.UPDATE)
    }

    companion object {
        const val KEY = "nav_bar_buttons_style"

        val readPermissions: Permissions = SettingsSecureStore.getReadPermissions()
        val writePermissions: Permissions = SettingsSecureStore.getWritePermissions()
    }
}
