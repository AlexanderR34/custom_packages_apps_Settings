/*
 * SPDX-FileCopyrightText: crDroid Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.clover.spoofing

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GameSpoofing : SettingsPreferenceFragment() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var enabled = false
    private var gameConfigs = mutableListOf<GameConfig>()

    data class GameConfig(
        val packageName: String,
        val appName: String,
        val props: Map<String, String>,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.game_spoofing)

        findPreference<SwitchPreferenceCompat>("gs_enabled")?.setOnPreferenceChangeListener { _, newValue ->
            enabled = newValue as Boolean
            saveConfig()
            true
        }

        findPreference<Preference>("gs_add_game")?.setOnPreferenceClickListener {
            showAddGameDialog()
            true
        }

        findPreference<Preference>("gs_reload")?.setOnPreferenceClickListener {
            loadConfig()
            true
        }

        loadConfig()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun loadConfig() {
        scope.launch {
            val result = withContext(Dispatchers.IO) { readGamePropsConfig() }
            enabled = result.first
            val pm = requireContext().packageManager
            gameConfigs = result.second.map { game ->
                val label = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(game.packageName, 0)).toString()
                } catch (_: PackageManager.NameNotFoundException) {
                    game.packageName
                }
                game.copy(appName = label)
            }.toMutableList()

            findPreference<SwitchPreferenceCompat>("gs_enabled")?.isChecked = enabled
            populateGameList()
        }
    }

    private fun populateGameList() {
        val category = findPreference<PreferenceCategory>("gs_games_category") ?: return
        category.removeAll()

        if (gameConfigs.isEmpty()) {
            category.addPreference(Preference(requireContext()).apply {
                title = getString(R.string.gs_no_apps)
                summary = getString(R.string.gs_no_apps_summary)
                isSelectable = false
            })
            return
        }

        for (game in gameConfigs) {
            val propsText = game.props.entries.joinToString(", ") { "${it.key}=${it.value}" }
            category.addPreference(Preference(requireContext()).apply {
                title = game.appName
                summary = "${game.packageName}\n$propsText"
                setOnPreferenceClickListener {
                    showGameOptionsDialog(game)
                    true
                }
            })
        }
    }

    private fun showGameOptionsDialog(game: GameConfig) {
        val options = arrayOf(
            getString(R.string.gs_edit_props),
            getString(R.string.gs_change_profile),
            getString(R.string.gs_remove),
        )
        AlertDialog.Builder(requireContext())
            .setTitle(game.appName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditPropsDialog(game)
                    1 -> showProfileSelector(game)
                    2 -> showDeleteGameDialog(game)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showAddGameDialog() {
        scope.launch {
            val progress = AlertDialog.Builder(requireContext())
                .setMessage(R.string.gs_loading_apps)
                .setCancelable(false)
                .show()

            try {
                val (labels, packages) = withContext(Dispatchers.IO) { getInstalledApps() }
                progress.dismiss()

                val configured = gameConfigs.map { it.packageName }.toSet()
                val availableIdx = packages.indices.filter { packages[it] !in configured }
                val availableLabels = availableIdx.map { labels[it] }.toTypedArray()

                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.gs_select_app)
                    .setItems(availableLabels) { _, which ->
                        val idx = availableIdx[which]
                        showProfileSelector(
                            GameConfig(packages[idx], labels[idx], emptyMap())
                        )
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } catch (e: Exception) {
                progress.dismiss()
                toast(getString(R.string.gs_failed, e.message ?: ""))
            }
        }
    }

    private fun showProfileSelector(game: GameConfig) {
        val profileNames = PRESET_PROFILES.map { it.first }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.gs_select_profile)
            .setItems(profileNames) { _, which ->
                val props = PRESET_PROFILES[which].second
                val newGame = game.copy(props = props)

                val existing = gameConfigs.indexOfFirst { it.packageName == newGame.packageName }
                if (existing >= 0) {
                    gameConfigs[existing] = newGame
                } else {
                    gameConfigs.add(newGame)
                }
                saveConfig()
                populateGameList()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showEditPropsDialog(game: GameConfig) {
        val ctx = requireContext()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, 0)
        }

        val fields = mutableMapOf<String, EditText>()
        for (key in STANDARD_KEYS) {
            val label = TextView(ctx).apply {
                text = key
                textSize = 12f
                setPadding(0, (6 * resources.displayMetrics.density).toInt(), 0, (2 * resources.displayMetrics.density).toInt())
            }
            val valueField = EditText(ctx).apply {
                setText(game.props[key] ?: "")
                hint = key
            }
            container.addView(label)
            container.addView(valueField)
            fields[key] = valueField
        }

        AlertDialog.Builder(ctx)
            .setTitle(getString(R.string.gs_edit_app, game.appName))
            .setView(container)
            .setPositiveButton(R.string.gs_save) { _, _ ->
                val newProps = mutableMapOf<String, String>()
                for ((key, field) in fields) {
                    val v = field.text.toString().trim()
                    if (v.isNotEmpty()) newProps[key] = v
                }
                val idx = gameConfigs.indexOfFirst { it.packageName == game.packageName }
                if (idx >= 0) {
                    gameConfigs[idx] = game.copy(props = newProps)
                    saveConfig()
                    populateGameList()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDeleteGameDialog(game: GameConfig) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.gs_remove_title)
            .setMessage(getString(R.string.gs_remove_message, game.appName))
            .setPositiveButton(R.string.gs_remove) { _, _ ->
                gameConfigs.removeAll { it.packageName == game.packageName }
                saveConfig()
                populateGameList()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun saveConfig() {
        scope.launch {
            withContext(Dispatchers.IO) { writeGamePropsConfig(enabled, gameConfigs) }
            toast(getString(R.string.gs_config_saved))
        }
    }

    private fun readGamePropsConfig(): Pair<Boolean, List<GameConfig>> {
        val cr = context?.contentResolver ?: return false to emptyList()
        val content = Settings.Secure.getString(cr, GAMEPROPS_CONFIG_KEY)
            ?: return false to emptyList()
        return try {
            val json = JSONObject(content)
            val isEnabled = json.optBoolean("enabled", false)
            val games = mutableListOf<GameConfig>()
            val gamesObj = json.optJSONObject("games")
            gamesObj?.keys()?.forEach { pkg ->
                val propsObj = gamesObj.getJSONObject(pkg)
                val props = mutableMapOf<String, String>()
                propsObj.keys().forEach { k -> props[k] = propsObj.getString(k) }
                games.add(GameConfig(pkg, pkg, props))
            }
            isEnabled to games
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load config", e)
            false to emptyList()
        }
    }

    private fun writeGamePropsConfig(isEnabled: Boolean, games: List<GameConfig>) {
        val cr = context?.contentResolver ?: return
        try {
            val json = JSONObject()
            json.put("enabled", isEnabled)
            val gamesObj = JSONObject()
            games.forEach { game ->
                val propsObj = JSONObject()
                game.props.forEach { (k, v) -> propsObj.put(k, v) }
                gamesObj.put(game.packageName, propsObj)
            }
            json.put("games", gamesObj)
            Settings.Secure.putString(
                cr,
                GAMEPROPS_CONFIG_KEY,
                json.toString(2)
            )
            Log.i(TAG, "Config saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save config", e)
        }
    }

    private fun getInstalledApps(): Pair<Array<String>, Array<String>> {
        val pm = requireContext().packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }
        return apps.map { pm.getApplicationLabel(it).toString() }.toTypedArray() to
            apps.map { it.packageName }.toTypedArray()
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun getMetricsCategory(): Int = com.android.internal.logging.nano.MetricsProto.MetricsEvent.DASHBOARD_SUMMARY

    companion object {
        private const val TAG = "GameSpoofing"
        private const val GAMEPROPS_CONFIG_KEY = "spoof_gameprops_config"

        private val STANDARD_KEYS = listOf("MODEL", "MANUFACTURER", "BRAND", "DEVICE")

        private val PRESET_PROFILES = listOf(
            "Realme 14 5G (120 FPS)" to mapOf(
                "MODEL" to "RMX5010",
                "MANUFACTURER" to "realme",
                "BRAND" to "realme",
                "DEVICE" to "RMX5010",
            ),
            "Realme GT 5 Pro (144 FPS)" to mapOf(
                "MODEL" to "RMX3888",
                "MANUFACTURER" to "realme",
                "BRAND" to "realme",
                "DEVICE" to "RMX3888",
            ),
            "ROG Phone 8 Pro (165 FPS)" to mapOf(
                "MODEL" to "ASUS_AI2401_A",
                "MANUFACTURER" to "asus",
                "BRAND" to "asus",
                "DEVICE" to "ASUS_AI2401_A",
            ),
            "Galaxy S24 Ultra (120 FPS)" to mapOf(
                "MODEL" to "SM-S928B",
                "MANUFACTURER" to "samsung",
                "BRAND" to "samsung",
                "DEVICE" to "e3q",
            ),
            "Xiaomi 14 Pro (120 FPS)" to mapOf(
                "MODEL" to "23116PN5BC",
                "MANUFACTURER" to "Xiaomi",
                "BRAND" to "Xiaomi",
                "DEVICE" to "shennong",
            ),
            "OnePlus 12 (120 FPS)" to mapOf(
                "MODEL" to "PJD110",
                "MANUFACTURER" to "OnePlus",
                "BRAND" to "OnePlus",
                "DEVICE" to "OP595DL1",
            ),
            "iQOO 12 Pro (144 FPS)" to mapOf(
                "MODEL" to "V2329A",
                "MANUFACTURER" to "vivo",
                "BRAND" to "vivo",
                "DEVICE" to "V2329A",
            ),
            "Black Shark 5 Pro (144 FPS)" to mapOf(
                "MODEL" to "SHARK KTUS-H",
                "MANUFACTURER" to "blackshark",
                "BRAND" to "blackshark",
                "DEVICE" to "katyusha",
            ),
            "Lenovo Legion Y700 (144 FPS)" to mapOf(
                "MODEL" to "Lenovo TB-9707F",
                "MANUFACTURER" to "Lenovo",
                "BRAND" to "Lenovo",
                "DEVICE" to "TB-9707F",
            ),
            "iPad Pro 11 (120 FPS)" to mapOf(
                "MODEL" to "iPad13,8",
                "MANUFACTURER" to "Apple",
                "BRAND" to "Apple",
                "DEVICE" to "iPad13,8",
            ),
        )
    }
}
