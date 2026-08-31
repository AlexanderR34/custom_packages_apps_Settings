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

package com.android.settings.deviceinfo.firmwareversion

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
import com.android.settingslib.DeviceInfoUtils
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
class KernelVersionPreference :
    PersistentPreference<String>,
    PreferenceMetadata,
    PreferenceSummaryProvider,
    PreferenceBinding,
    Preference.OnPreferenceClickListener {

    private val hits = LongArray(ACTIVITY_TRIGGER_COUNT)

    override val key: String
        get() = "kernel_version"

    override val purpose: Int
        get() = R.string.kernel_version_purpose

    override val title: Int
        get() = R.string.kernel_version

    override val supportsWrite = false

    override val valueType = String::class.javaObjectType

    override fun storage(context: Context): KeyValueStore = createSummaryStorage(context, key)

    override fun getSummary(context: Context): CharSequence? =
        DeviceInfoUtils.getFormattedKernelVersion(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isSelectable = true
        preference.isCopyingEnabled = true
        preference.onPreferenceClickListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        if (Utils.isMonkeyRunning()) return true

        for (index in 1..<ACTIVITY_TRIGGER_COUNT) hits[index - 1] = hits[index]
        hits[ACTIVITY_TRIGGER_COUNT - 1] = SystemClock.uptimeMillis()
        if (hits[ACTIVITY_TRIGGER_COUNT - 1] - hits[0] <= DELAY_TIMER_MILLIS) {
            launchN64EasterEgg(preference.context)
            return true
        }
        return false
    }

    private fun launchN64EasterEgg(context: Context) {
        val targetComponents = arrayOf(
            ComponentName("org.mupen64plusae.v3.alpha", "paulscode.android.mupen64plusae.SplashActivity"),
            ComponentName("org.mupen64plusae.v3.alpha", "paulscode.android.mupen64plusae.GalleryActivity"),
            ComponentName("org.lunaris.easteregg", "org.lunaris.easteregg.BridgeActivity"),
            ComponentName("org.mupen64plusae.v3.alpha", "org.mupen64plusae.v3.alpha.MenuActivity"),
            ComponentName("org.mupen64plusae.v3.fzurita", "org.mupen64plusae.v3.fzurita.MenuActivity"),
            ComponentName("org.mupen64plusae.v3.fzurita.pro", "org.mupen64plusae.v3.fzurita.pro.MenuActivity"),
            ComponentName("com.retroarch.aarch64", "com.retroarch.browser.retroactivity.RetroActivityFuture"),
            ComponentName("com.retroarch", "com.retroarch.browser.retroactivity.RetroActivityFuture")
        )

        for (component in targetComponents) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    this.component = component
                    addCategory(Intent.CATEGORY_DEFAULT)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(intent)
                return
            } catch (ignored: ActivityNotFoundException) {
            } catch (e: Exception) {
                Log.e("KernelVersionPref", "Error launching $component", e)
            }
        }

        // Fallback: Launch intent by package
        val candidatePackages = arrayOf(
            "org.lunaris.easteregg",
            "org.mupen64plusae.v3.alpha",
            "org.mupen64plusae.v3.fzurita",
            "org.mupen64plusae.v3.fzurita.pro",
            "com.retroarch.aarch64",
            "com.retroarch"
        )
        for (pkg in candidatePackages) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    context.startActivity(launchIntent)
                    return
                }
            } catch (ignored: Exception) {
            }
        }

        Toast.makeText(context, "N64 Emulator Easter Egg", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val DELAY_TIMER_MILLIS = 1200L
        const val ACTIVITY_TRIGGER_COUNT = 3
    }

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

}
// LINT.ThenChange(KernelVersionPreferenceController.java)
