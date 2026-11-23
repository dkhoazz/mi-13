package com.example.hyperos

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.ArtCompileMode
import com.example.data.DebloatPackageState
import com.example.data.HyperAnimRate
import com.example.data.PermissionStatusSnapshot
import com.example.data.PrivateDnsPreset
import com.example.data.ProfileConfig
import com.example.data.RefreshRateMode
import com.example.data.SettingsRepository
import com.example.data.StockSystemSnapshot
import com.example.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class TweakActionResult(
    val success: Boolean,
    val message: String
)

class HyperOSTweaker(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val shizukuManager: ShizukuManager,
    private val localizedContextProvider: () -> Context = { context }
) {
    private val tweakMutex = Mutex()
    private val validPackageRegex = Regex("^[a-zA-Z0-9._]+$")

    private fun isSafePackageName(pkg: String): Boolean =
        validPackageRegex.matches(pkg) && !ChinaDebloatCatalog.HARD_BLOCKLIST.contains(pkg)

    fun readPermissionStatus(): PermissionStatusSnapshot {
        val hasSecure = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED

        val hasWriteSettings = Settings.System.canWrite(context)

        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val usageMode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        val hasUsageStats = usageMode == AppOpsManager.MODE_ALLOWED

        val hasPostNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val ignoringBattery = pm.isIgnoringBatteryOptimizations(context.packageName)

        return PermissionStatusSnapshot(
            hasWriteSecureSettings = hasSecure,
            hasWriteSettings = hasWriteSettings,
            hasUsageStats = hasUsageStats,
            hasPostNotifications = hasPostNotif,
            isIgnoringBatteryOptimizations = ignoringBattery
        )
    }

    private suspend fun ensureStockSnapshotCaptured() {
        val existing = settingsRepository.getSnapshot()
        if (existing.isCaptured) return

        val cr = context.contentResolver
        val freezerVal = if (shizukuManager.hasPermission()) {
            val res = shizukuManager.executeCommand("device_config get activity_manager_native_boot use_freezer")
            if (res.isSuccess && res.output.isNotBlank() && res.output != "null") res.output.trim() else null
        } else {
            null
        }

        val snapshot = StockSystemSnapshot(
            isCaptured = true,
            userRefreshRate = runCatching { Settings.System.getString(cr, "user_refresh_rate") }.getOrNull(),
            minRefreshRate = runCatching { Settings.System.getString(cr, "min_refresh_rate") }.getOrNull(),
            peakRefreshRate = runCatching { Settings.System.getString(cr, "peak_refresh_rate") }.getOrNull(),
            miuiRefreshRate = runCatching { Settings.System.getString(cr, "miui_refresh_rate") }.getOrNull(),
            speedMode = runCatching { Settings.System.getString(cr, "speed_mode") }.getOrNull(),
            windowAnimationScale = runCatching {
                Settings.Global.getString(cr, Settings.Global.WINDOW_ANIMATION_SCALE)
            }.getOrNull(),
            transitionAnimationScale = runCatching {
                Settings.Global.getString(cr, Settings.Global.TRANSITION_ANIMATION_SCALE)
            }.getOrNull(),
            animatorDurationScale = runCatching {
                Settings.Global.getString(cr, Settings.Global.ANIMATOR_DURATION_SCALE)
            }.getOrNull(),
            miuiHomeAnimRate = runCatching { Settings.System.getString(cr, "miui_home_animation_rate") }.getOrNull(),
            gameDriverApps = runCatching {
                Settings.Global.getString(cr, "updatable_driver_production_opt_in_apps")
            }.getOrNull(),
            privateDnsMode = runCatching { Settings.Global.getString(cr, "private_dns_mode") }.getOrNull(),
            privateDnsSpecifier = runCatching { Settings.Global.getString(cr, "private_dns_specifier") }.getOrNull(),
            wifiScanAlwaysEnabled = runCatching { Settings.Global.getString(cr, "wifi_scan_always_enabled") }.getOrNull(),
            bleScanAlwaysEnabled = runCatching { Settings.Global.getString(cr, "ble_scan_always_enabled") }.getOrNull(),
            cachedAppFreezer = freezerVal
        )
        settingsRepository.saveSnapshot(snapshot)
    }

    private suspend fun writeSystemSetting(key: String, value: String?): Boolean {
        val cr = context.contentResolver
        if (value == null || value == SettingsRepository.UNSET_SENTINEL) {
            if (shizukuManager.hasPermission()) {
                val res = shizukuManager.executeCommand("settings delete system $key")
                if (res.isSuccess) return true
            }
            return runCatching {
                Settings.System.putString(cr, key, null)
            }.getOrDefault(false)
        }

        val directOk = if (Settings.System.canWrite(context)) {
            runCatching { Settings.System.putString(cr, key, value) }.getOrDefault(false)
        } else {
            false
        }
        if (directOk) return true

        if (shizukuManager.hasPermission()) {
            return shizukuManager.executeCommand("settings put system $key $value").isSuccess
        }
        return false
    }

    private suspend fun writeGlobalSetting(key: String, value: String?): Boolean {
        val cr = context.contentResolver
        if (value == null || value == SettingsRepository.UNSET_SENTINEL) {
            if (shizukuManager.hasPermission()) {
                val res = shizukuManager.executeCommand("settings delete global $key")
                if (res.isSuccess) return true
            }
            return false
        }

        val hasSecure = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED

        val directOk = if (hasSecure) {
            runCatching { Settings.Global.putString(cr, key, value) }.getOrDefault(false)
        } else {
            false
        }
        if (directOk) return true

        if (shizukuManager.hasPermission()) {
            return shizukuManager.executeCommand("settings put global $key $value").isSuccess
        }
        return false
    }

    suspend fun setRefreshRate(mode: RefreshRateMode): TweakActionResult = tweakMutex.withLock {
        withContext(Dispatchers.IO) {
            val loc = localizedContextProvider()
            ensureStockSnapshotCaptured()
            val ok: Boolean = when (mode) {
                RefreshRateMode.HZ_120 -> {
                    val a = writeSystemSetting("user_refresh_rate", "120")
                    val b = writeSystemSetting("min_refresh_rate", "120")
                    val c = writeSystemSetting("peak_refresh_rate", "120")
                    val d = writeSystemSetting("miui_refresh_rate", "120")
                    a || b || c || d
                }

                RefreshRateMode.HZ_60 -> {
                    val a = writeSystemSetting("user_refresh_rate", "60")
                    val b = writeSystemSetting("min_refresh_rate", "60")
                    val c = writeSystemSetting("peak_refresh_rate", "60")
                    val d = writeSystemSetting("miui_refresh_rate", "60")
                    a || b || c || d
                }

                RefreshRateMode.DYNAMIC -> {
                    val snap = settingsRepository.getSnapshot()
                    val a = writeSystemSetting("user_refresh_rate", snap.userRefreshRate ?: "120")
                    val b = writeSystemSetting("min_refresh_rate", snap.minRefreshRate)
                    val c = writeSystemSetting("peak_refresh_rate", snap.peakRefreshRate)
                    val d = writeSystemSetting("miui_refresh_rate", snap.miuiRefreshRate ?: "120")
                    a || b || c || d
                }
            }
            if (ok) {
                settingsRepository.updateTweaksState(refreshRate = mode)
                TweakActionResult(true, loc.getString(R.string.msg_rr_ok, loc.getString(mode.labelRes)))
            } else {
                TweakActionResult(false, loc.getString(R.string.msg_rr_fail))
            }
        }
    }

    suspend fun setSpeedMode(enabled: Boolean): TweakActionResult = tweakMutex.withLock {
        withContext(Dispatchers.IO) {
            val loc = localizedContextProvider()
            ensureStockSnapshotCaptured()
            val value = if (enabled) "1" else "0"
            val ok = writeSystemSetting("speed_mode", value)
            if (ok) {
                settingsRepository.updateTweaksState(speedMode = enabled)
                TweakActionResult(
                    true,
                    loc.getString(if (enabled) R.string.msg_speed_on else R.string.msg_speed_off)
                )
            } else {
                TweakActionResult(false, loc.getString(R.string.msg_speed_fail))
            }
        }
    }

    suspend fun setMiuiHomeAnimRate(rate: HyperAnimRate): TweakActionResult = tweakMutex.withLock {
        withContext(Dispatchers.IO) {
            val loc = localizedContextProvider()
            ensureStockSnapshotCaptured()
            val ok = writeSystemSetting("miui_home_animation_rate", rate.settingValue)
            if (ok) {
                settingsRepository.updateTweaksState(miuiHomeAnimRate = rate)
                TweakActionResult(true, loc.getString(R.string.msg_home_anim_ok, loc.getString(rate.labelRes)))
            } else {
                TweakActionResult(false, loc.getString(R.string.msg_home_anim_fail))
            }
        }
    }

    suspend fun setAnimationScale(scale: Float): TweakActionResult = tweakMutex.withLock {
        withContext(Dispatchers.IO) {
            val loc = localizedContextProvider()
            ensureStockSnapshotCaptured()
            val formatted = scale.toString()
            val a = writeGlobalSetting(Settings.Global.WINDOW_ANIMATION_SCALE, formatted)
            val b = writeGlobalSetting(Settings.Global.TRANSITION_ANIMATION_SCALE, formatted)
            val c = writeGlobalSetting(Settings.Global.ANIMATOR_DURATION_SCALE, formatted)
            if (a && b && c) {
                settingsRepository.updateTweaksState(animationScale = scale)
                TweakActionResult(true, loc.getString(R.string.msg_anim_ok, formatted))
            } else {
                TweakActionResult(false, loc.getString(R.string.msg_anim_fail))
            }
        }
    }

    suspend fun setGameDriverOptIn(enabled: Boolean, packages: Set<String>): TweakActionResult = tweakMutex.withLock {
        withContext(Dispatchers.IO) {
            val loc = localizedContextProvider()
            ensureStockSnapshotCaptured()
            val safeCsv = packages.filter { validPackageRegex.matches(it) }.joinToString(",")
            val ok = if (enabled && safeCsv.isNotBlank()) {
                writeGlobalSetting("updatable_driver_production_opt_in_apps", safeCsv)
            } else {
                writeGlobalSetting("updatable_driver_production_opt_in_apps", SettingsRepository.UNSET_SENTINEL)
            }
            if (ok) {
                settingsRepository.updateTweaksState(gameDriverEnabled = enabled)
                TweakActionResult(true, loc.getString(R.string.msg_game_driver_ok))
            } else {
                TweakActionResult(false, loc.getString(R.string.msg_game_driver_fail))
            }
        }
    }

    suspend fun setPhantomProcessGuard(enabled: Boolean): TweakActionResult = tweakMutex.withLock {
        withContext(Dispatchers.IO) {
            val loc = localizedContextProvider()
            ensureStockSnapshotCaptured()
            val cmd = if (enabled) {
                "settings put global settings_enable_monitor_phantom_procs false && " +
                    "device_config set_sync_disabled_for_tests persistent && " +
                    "device_config put activity_manager max_phantom_processes 2147483647"
            } else {
                "settings put global settings_enable_monitor_phantom_procs true && " +
                    "device_config set_sync_disabled_for_tests none && " +
                    "device_config delete activity_manager max_phantom_processes"
            }
            val result = shizukuManager.executeCommand(cmd)
            if (result.isSuccess) {
                settingsRepository.updateTweaksState(phantomGuard = enabled)
                TweakActionResult(
                    true,
                    loc.getString(if (enabled) R.string.msg_phantom_on else R.string.msg_phantom_off)
                )
            } else {
                TweakActionResult(false, loc.getString(R.string.msg_phantom_fail, result.output))
            }
        }
    }

    suspend fun setThermalOverride(enabled: Boolean, currentTempC: Float? = null): TweakActionResult =
        tweakMutex.withLock {
            withContext(Dispatchers.IO) {
                val loc = localizedContextProvider()
                if (enabled && currentTempC != null && currentTempC > 43.0f) {
                    shizukuManager.executeCommand("cmd thermalservice reset")
                    settingsRepository.updateTweaksState(thermalOverride = false)
                    return@withContext TweakActionResult(
                        false,
                        loc.getString(R.string.msg_thermal_blocked, currentTempC)
                    )
                }

                val cmd = if (enabled) {
                    "cmd thermalservice override-status 0"
                } else {
                    "cmd thermalservice reset"
                }
                val result = shizukuManager.executeCommand(cmd)
                if (result.isSuccess) {
                    settingsRepository.updateTweaksState(thermalOverride = enabled)
                    TweakActionResult(
                        true,
                        loc.getString(if (enabled) R.string.msg_thermal_on else R.string.msg_thermal_off)
                    )
                } else {
                    TweakActionResult(false, loc.getString(R.string.msg_thermal_fail, result.output))
                }
            }
        }

    suspend fun setCachedAppFreezer(enabled: Boolean): TweakActionResult = tweakMutex.withLock {
        withContext(Dispatchers.IO) {
            val loc = localizedContextProvider()
            ensureStockSnapshotCaptured()
            val cmd = "device_config put activity_manager_native_boot use_freezer ${if (enabled) "true" else "false"}"
            val result = shizukuManager.executeCommand(cmd)
            if (result.isSuccess) {
                settingsRepository.updateTweaksState(cachedFreezerEnabled = enabled)
                TweakActionResult(true, loc.getString(R.string.msg_freezer_ok))
            } else {
                TweakActionResult(false, loc.getString(R.string.msg_freezer_fail, result.output))
            }
        }
    }

    suspend fun setPrivateDns(preset: PrivateDnsPreset): TweakActionResult = tweakMutex.withLock {
        withContext(Dispatchers.IO) {
            val loc = localizedContextProvider()
            ensureStockSnapshotCaptured()
            val modeOk = writeGlobalSetting("private_dns_mode", preset.mode)
            val specOk = if (preset.host != null) {
                writeGlobalSetting("private_dns_specifier", preset.host)
            } else {
                writeGlobalSetting("private_dns_specifier", SettingsRepository.UNSET_SENTINEL)
                true
            }
            if (modeOk && specOk) {
                settingsRepository.updateTweaksState(privateDnsPreset = preset)
                TweakActionResult(true, loc.getString(R.string.msg_dns_ok, loc.getString(preset.labelRes)))
            } else {
                TweakActionResult(false, loc.getString(R.string.msg_dns_fail))
            }
        }
    }

    suspend fun setWifiScanDisabled(disabled: Boolean): TweakActionResult = tweakMutex.withLock {
        withContext(Dispatchers.IO) {
            val loc = localizedContextProvider()
            ensureStockSnapshotCaptured()
            val ok = writeGlobalSetting("wifi_scan_always_enabled", if (disabled) "0" else "1")
            if (ok) {
                settingsRepository.updateTweaksState(wifiScanDisabled = disabled)
                TweakActionResult(true, loc.getString(R.string.msg_scan_guard_ok))
            } else {
                TweakActionResult(false, loc.getString(R.string.msg_scan_guard_fail))
            }
        }
    }

    suspend fun setBleScanDisabled(disabled: Boolean): TweakActionResult = tweakMutex.withLock {
        withContext(Dispatchers.IO) {
            val loc = localizedContextProvider()
            ensureStockSnapshotCaptured()
            val ok = writeGlobalSetting("ble_scan_always_enabled", if (disabled) "0" else "1")
            if (ok) {
                settingsRepository.updateTweaksState(bleScanDisabled = disabled)
                TweakActionResult(true, loc.getString(R.string.msg_scan_guard_ok))
            } else {
                TweakActionResult(false, loc.getString(R.string.msg_scan_guard_fail))
            }
        }
    }

    suspend fun inspectDebloatCatalog(): List<DebloatPackageState> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val disabledSet: Set<String> = if (shizukuManager.hasPermission()) {
            val res = shizukuManager.executeCommand("pm list packages -d")
            if (res.isSuccess) {
                res.output.lineSequence()
                    .map { it.removePrefix("package:").trim() }
                    .filter { it.isNotBlank() }
                    .toSet()
            } else {
                emptySet()
            }
        } else {
            emptySet()
        }

        ChinaDebloatCatalog.entries
            .filter { !ChinaDebloatCatalog.HARD_BLOCKLIST.contains(it.packageName) }
            .map { entry ->
                val appInfo = runCatching {
                    pm.getApplicationInfo(entry.packageName, PackageManager.MATCH_DISABLED_COMPONENTS)
                }.getOrNull()
                val installed = appInfo != null
                val disabled = if (!installed) {
                    false
                } else if (disabledSet.isNotEmpty()) {
                    disabledSet.contains(entry.packageName)
                } else {
                    !appInfo!!.enabled
                }
                DebloatPackageState(
                    entry = entry,
                    isInstalled = installed,
                    isDisabled = disabled
                )
            }
    }

    suspend fun setPackageFrozen(packageName: String, freeze: Boolean): TweakActionResult = tweakMutex.withLock {
        withContext(Dispatchers.IO) {
            val loc = localizedContextProvider()
            if (!isSafePackageName(packageName)) {
                return@withContext TweakActionResult(false, loc.getString(R.string.msg_pkg_blocked))
            }
            val cmd = if (freeze) "pm disable-user --user 0 $packageName" else "pm enable $packageName"
            val res = shizukuManager.executeCommand(cmd)
            if (res.isSuccess) {
                val current = settingsRepository.preferencesFlow.first().frozenByAppPackages
                val updated = if (freeze) current + packageName else current - packageName
                settingsRepository.updateFrozenByAppPackages(updated)
                TweakActionResult(
                    true,
                    loc.getString(if (freeze) R.string.msg_pkg_frozen else R.string.msg_pkg_restored, packageName)
                )
            } else {
                TweakActionResult(false, loc.getString(R.string.msg_pkg_fail, res.output))
            }
        }
    }

    suspend fun freezeSafePack(currentStates: List<DebloatPackageState>): TweakActionResult = tweakMutex.withLock {
        withContext(Dispatchers.IO) {
            val loc = localizedContextProvider()
            val targets = currentStates.filter {
                it.entry.riskTier == DebloatRiskTier.SAFE &&
                    it.isInstalled &&
                    !it.isDisabled &&
                    isSafePackageName(it.entry.packageName)
            }
            if (targets.isEmpty()) {
                return@withContext TweakActionResult(true, loc.getString(R.string.msg_safe_pack_done, 0))
            }
            val tracked = settingsRepository.preferencesFlow.first().frozenByAppPackages.toMutableSet()
            var count = 0
            for (item in targets) {
                val res = shizukuManager.executeCommand("pm disable-user --user 0 ${item.entry.packageName}")
                if (res.isSuccess) {
                    count++
                    tracked.add(item.entry.packageName)
                } else if (res.exitCode == -1) {
                    return@withContext TweakActionResult(false, loc.getString(R.string.msg_pkg_fail, res.output))
                }
            }
            settingsRepository.updateFrozenByAppPackages(tracked)
            TweakActionResult(true, loc.getString(R.string.msg_safe_pack_done, count))
        }
    }

    suspend fun restoreAllAppFrozen(): TweakActionResult = tweakMutex.withLock {
        withContext(Dispatchers.IO) {
            val loc = localizedContextProvider()
            val tracked = settingsRepository.preferencesFlow.first().frozenByAppPackages
            if (tracked.isEmpty()) {
                return@withContext TweakActionResult(true, loc.getString(R.string.msg_restore_frozen_done, 0))
            }
            val remaining = tracked.toMutableSet()
            var count = 0
            for (pkg in tracked) {
                if (!validPackageRegex.matches(pkg)) continue
                val res = shizukuManager.executeCommand("pm enable $pkg")
                if (res.isSuccess) {
                    count++
                    remaining.remove(pkg)
                }
            }
            settingsRepository.updateFrozenByAppPackages(remaining)
            TweakActionResult(true, loc.getString(R.string.msg_restore_frozen_done, count))
        }
    }

    suspend fun setAppBackgroundRestricted(packageName: String, restricted: Boolean): TweakActionResult =
        tweakMutex.withLock {
            withContext(Dispatchers.IO) {
                val loc = localizedContextProvider()
                if (!isSafePackageName(packageName) || packageName == context.packageName) {
                    return@withContext TweakActionResult(false, loc.getString(R.string.msg_pkg_blocked))
                }
                val cmd = if (restricted) {
                    "am set-standby-bucket $packageName restricted && " +
                        "cmd appops set $packageName RUN_ANY_IN_BACKGROUND ignore && " +
                        "cmd appops set $packageName RUN_IN_BACKGROUND ignore"
                } else {
                    "am set-standby-bucket $packageName active && " +
                        "cmd appops set $packageName RUN_ANY_IN_BACKGROUND allow && " +
                        "cmd appops set $packageName RUN_IN_BACKGROUND allow"
                }
                val res = shizukuManager.executeCommand(cmd)
                if (res.isSuccess) {
                    val current = settingsRepository.preferencesFlow.first().restrictedBgPackages
                    val updated = if (restricted) current + packageName else current - packageName
                    settingsRepository.updateRestrictedBgPackages(updated)
                    TweakActionResult(
                        true,
                        loc.getString(if (restricted) R.string.msg_bg_restricted else R.string.msg_bg_restored, packageName)
                    )
                } else {
                    TweakActionResult(false, loc.getString(R.string.msg_bg_fail, res.output))
                }
            }
        }

    suspend fun compileSinglePackage(packageName: String, mode: ArtCompileMode): TweakActionResult =
        withContext(Dispatchers.IO) {
            val loc = localizedContextProvider()
            if (!validPackageRegex.matches(packageName)) {
                return@withContext TweakActionResult(false, loc.getString(R.string.msg_pkg_blocked))
            }
            val res = shizukuManager.executeCommand("cmd package compile -m ${mode.flag} -f $packageName")
            if (res.isSuccess) {
                TweakActionResult(true, loc.getString(R.string.msg_art_one_ok, packageName, mode.flag))
            } else {
                TweakActionResult(false, loc.getString(R.string.msg_art_one_fail, packageName, res.output))
            }
        }

    suspend fun runBgDexoptJob(): TweakActionResult = withContext(Dispatchers.IO) {
        val loc = localizedContextProvider()
        val res = shizukuManager.executeCommand("cmd package bg-dexopt-job")
        if (res.isSuccess) {
            TweakActionResult(true, loc.getString(R.string.msg_art_bg_dexopt_ok))
        } else {
            TweakActionResult(false, loc.getString(R.string.msg_art_bg_dexopt_fail, res.output))
        }
    }

    suspend fun trimSystemCaches(): TweakActionResult = withContext(Dispatchers.IO) {
        val loc = localizedContextProvider()
        val res = shizukuManager.executeCommand("pm trim-caches 999999999999")
        if (res.isSuccess) {
            TweakActionResult(true, loc.getString(R.string.msg_trim_cache_ok))
        } else {
            TweakActionResult(false, loc.getString(R.string.msg_trim_cache_fail, res.output))
        }
    }

    suspend fun emergencyThermalReset(): Boolean = tweakMutex.withLock {
        withContext(Dispatchers.IO) {
            settingsRepository.updateTweaksState(thermalOverride = false, speedMode = false)
            writeSystemSetting("speed_mode", "0")
            val res = shizukuManager.executeCommand("cmd thermalservice reset")
            res.isSuccess
        }
    }

    suspend fun applyProfile(profile: ProfileConfig, currentTempC: Float): TweakActionResult {
        val loc = localizedContextProvider()
        val prefs = settingsRepository.preferencesFlow.first()
        val rrRes = setRefreshRate(profile.refreshRate)
        val speedRes = setSpeedMode(profile.speedMode)
        val animRes = setAnimationScale(profile.animationScale)
        setGameDriverOptIn(profile.gameDriverOptIn, prefs.selectedGamingPackages)
        if (shizukuManager.hasPermission()) {
            setPhantomProcessGuard(profile.phantomGuard)
            setCachedAppFreezer(profile.cachedFreezer)
            val allowThermal = profile.thermalOverride && currentTempC <= 43.0f
            setThermalOverride(allowThermal, currentTempC)
        } else {
            settingsRepository.updateTweaksState(
                phantomGuard = profile.phantomGuard,
                thermalOverride = false
            )
        }
        val anyFailed = !rrRes.success && !speedRes.success && !animRes.success
        return if (anyFailed) {
            TweakActionResult(false, loc.getString(R.string.msg_profile_partial_fail))
        } else {
            TweakActionResult(true, loc.getString(R.string.msg_profile_applied, profile.resolveName(loc)))
        }
    }

    suspend fun restoreStockDefaults(): TweakActionResult = tweakMutex.withLock {
        withContext(Dispatchers.IO) {
            val loc = localizedContextProvider()
            val snap = settingsRepository.getSnapshot()
            val prefs = settingsRepository.preferencesFlow.first()

            if (snap.isCaptured) {
                writeSystemSetting("user_refresh_rate", snap.userRefreshRate)
                writeSystemSetting("min_refresh_rate", snap.minRefreshRate)
                writeSystemSetting("peak_refresh_rate", snap.peakRefreshRate)
                writeSystemSetting("miui_refresh_rate", snap.miuiRefreshRate)
                writeSystemSetting("speed_mode", snap.speedMode ?: "0")
                writeSystemSetting("miui_home_animation_rate", snap.miuiHomeAnimRate)
                writeGlobalSetting(Settings.Global.WINDOW_ANIMATION_SCALE, snap.windowAnimationScale ?: "1.0")
                writeGlobalSetting(Settings.Global.TRANSITION_ANIMATION_SCALE, snap.transitionAnimationScale ?: "1.0")
                writeGlobalSetting(Settings.Global.ANIMATOR_DURATION_SCALE, snap.animatorDurationScale ?: "1.0")
                writeGlobalSetting("updatable_driver_production_opt_in_apps", snap.gameDriverApps)
                writeGlobalSetting("private_dns_mode", snap.privateDnsMode ?: "opportunistic")
                writeGlobalSetting("private_dns_specifier", snap.privateDnsSpecifier)
                writeGlobalSetting("wifi_scan_always_enabled", snap.wifiScanAlwaysEnabled ?: "1")
                writeGlobalSetting("ble_scan_always_enabled", snap.bleScanAlwaysEnabled ?: "1")
            } else {
                writeSystemSetting("speed_mode", "0")
                writeGlobalSetting(Settings.Global.WINDOW_ANIMATION_SCALE, "1.0")
                writeGlobalSetting(Settings.Global.TRANSITION_ANIMATION_SCALE, "1.0")
                writeGlobalSetting(Settings.Global.ANIMATOR_DURATION_SCALE, "1.0")
            }

            if (shizukuManager.hasPermission()) {
                shizukuManager.executeCommand(
                    "settings put global settings_enable_monitor_phantom_procs true && " +
                        "device_config set_sync_disabled_for_tests none && " +
                        "device_config delete activity_manager max_phantom_processes && " +
                        "cmd thermalservice reset"
                )
                if (snap.cachedAppFreezer != null && snap.cachedAppFreezer != SettingsRepository.UNSET_SENTINEL) {
                    shizukuManager.executeCommand(
                        "device_config put activity_manager_native_boot use_freezer ${snap.cachedAppFreezer}"
                    )
                }
                for (pkg in prefs.frozenByAppPackages) {
                    if (validPackageRegex.matches(pkg)) {
                        shizukuManager.executeCommand("pm enable $pkg")
                    }
                }
                for (pkg in prefs.restrictedBgPackages) {
                    if (validPackageRegex.matches(pkg)) {
                        shizukuManager.executeCommand(
                            "am set-standby-bucket $pkg active && " +
                                "cmd appops set $pkg RUN_ANY_IN_BACKGROUND allow && " +
                                "cmd appops set $pkg RUN_IN_BACKGROUND allow"
                        )
                    }
                }
            }

            settingsRepository.updateFrozenByAppPackages(emptySet())
            settingsRepository.updateRestrictedBgPackages(emptySet())
            settingsRepository.updateTweaksState(
                refreshRate = RefreshRateMode.DYNAMIC,
                speedMode = false,
                animationScale = 1.0f,
                phantomGuard = false,
                thermalOverride = false,
                miuiHomeAnimRate = HyperAnimRate.BALANCED,
                gameDriverEnabled = false,
                cachedFreezerEnabled = false,
                privateDnsPreset = PrivateDnsPreset.OFF,
                wifiScanDisabled = false,
                bleScanDisabled = false
            )
            TweakActionResult(true, loc.getString(R.string.msg_restore_ok))
        }
    }
}
