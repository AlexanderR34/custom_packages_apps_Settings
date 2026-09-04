/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.applications

import android.content.Context
import android.provider.Settings
import android.text.BidiFormatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.android.settings.R.string.install_type_instant
import com.android.settings.Utils
import com.android.settingslib.applications.AppUtils
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.LayoutPreference
import com.android.settingslib.widget.preference.layout.R

class AppInfoHeaderPreference(private val packageInfoProvider: PackageInfoProvider) :
    PreferenceMetadata, PreferenceBinding {

    override val key
        get() = KEY

    override val purpose: Int
        get() = com.android.settings.R.string.app_info_header_purpose

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override val sensitivityLevel = SensitivityLevel.DO_NOT_EXPOSE

    override fun createWidget(context: Context): Preference {
        val view = LayoutInflater.from(context).inflate(R.layout.settings_entity_header, null)
        return LayoutPreference(context, view).apply {
            isSelectable = false
            isAllowDividerBelow = true
        }
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        val packageInfo = packageInfoProvider.packageInfo ?: return
        val applicationInfo = packageInfo.applicationInfo ?: return

        preference as LayoutPreference
        fun setText(viewId: Int, text: CharSequence?) {
            preference.findViewById<TextView>(viewId)?.apply {
                setText(text)
                visibility = if (text?.isNotEmpty() == true) View.VISIBLE else View.GONE
            }
        }

        val context = preference.context
        preference
            .findViewById<ImageView>(R.id.entity_header_icon)
            ?.setImageDrawable(Utils.getBadgedIcon(context, applicationInfo))
        
        val titleView = preference.findViewById<TextView>(R.id.entity_header_title)
        titleView?.apply {
            text = applicationInfo.loadLabel(context.packageManager)
            visibility = View.VISIBLE
            setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, com.android.settings.R.drawable.ic_edit_app_name, 0)
            compoundDrawablePadding = (8 * context.resources.displayMetrics.density).toInt()
            setOnClickListener {
                showRenameDialog(context, applicationInfo.packageName) { newName ->
                    text = newName
                }
            }
        }

        // Wrapped the version name to support RTL
        val summary = BidiFormatter.getInstance().unicodeWrap(packageInfo.versionName)
        setText(R.id.entity_header_summary, summary)
        val installType =
            when (AppUtils.isInstant(applicationInfo)) {
                true -> context.getString(install_type_instant)
                else -> null
            }
        setText(R.id.install_type, installType)
        preference.findViewById<View>(android.R.id.button1)?.visibility = View.GONE
        preference.findViewById<View>(android.R.id.button2)?.visibility = View.GONE
    }

    private fun showRenameDialog(context: Context, packageName: String, onUpdated: (CharSequence) -> Unit) {
        val input = EditText(context).apply {
            isSingleLine = true
            hint = context.getString(com.android.settings.R.string.custom_app_name_hint)
        }

        val currentCustom = Settings.System.getString(
            context.contentResolver, "custom_app_label_$packageName"
        )
        if (!currentCustom.isNullOrEmpty()) {
            input.setText(currentCustom)
            input.setSelection(currentCustom.length)
        }

        val container = FrameLayout(context).apply {
            val margin = (24 * context.resources.displayMetrics.density).toInt()
            val topMarginPx = (8 * context.resources.displayMetrics.density).toInt()
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = margin
                rightMargin = margin
                topMargin = topMarginPx
                bottomMargin = topMarginPx
            }
            input.layoutParams = params
            addView(input)
        }

        AlertDialog.Builder(context)
            .setTitle(com.android.settings.R.string.custom_app_name_title)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    Settings.System.putString(
                        context.contentResolver,
                        "custom_app_label_$packageName",
                        newName
                    )
                    onUpdated(newName)
                    Toast.makeText(context, com.android.settings.R.string.custom_app_name_saved, Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton(com.android.settings.R.string.custom_app_name_reset) { _, _ ->
                Settings.System.putString(
                    context.contentResolver,
                    "custom_app_label_$packageName",
                    null
                )
                val originalLabel = try {
                    context.packageManager.getApplicationInfo(packageName, 0).loadLabel(context.packageManager)
                } catch (e: Exception) {
                    packageName
                }
                onUpdated(originalLabel)
                Toast.makeText(context, com.android.settings.R.string.custom_app_name_reset_done, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        const val KEY = "app_info_header"
    }
}
