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
import com.example.data.PermissionStatusSnapshot
import com.example.data.ProfileConfig
import com.example.data.RefreshRateMode
import com.example.data.SettingsRepository
import com.example.data.StockSystemSnapshot
import com.example.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
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
            }.getOrNull()
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
        val rrRes = setRefreshRate(profile.refreshRate)
        val speedRes = setSpeedMode(profile.speedMode)
        val animRes = setAnimationScale(profile.animationScale)
        if (shizukuManager.hasPermission()) {
            setPhantomProcessGuard(profile.phantomGuard)
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
            if (snap.isCaptured) {
                writeSystemSetting("user_refresh_rate", snap.userRefreshRate)
                writeSystemSetting("min_refresh_rate", snap.minRefreshRate)
                writeSystemSetting("peak_refresh_rate", snap.peakRefreshRate)
                writeSystemSetting("miui_refresh_rate", snap.miuiRefreshRate)
                writeSystemSetting("speed_mode", snap.speedMode ?: "0")
                writeGlobalSetting(Settings.Global.WINDOW_ANIMATION_SCALE, snap.windowAnimationScale ?: "1.0")
                writeGlobalSetting(Settings.Global.TRANSITION_ANIMATION_SCALE, snap.transitionAnimationScale ?: "1.0")
                writeGlobalSetting(Settings.Global.ANIMATOR_DURATION_SCALE, snap.animatorDurationScale ?: "1.0")
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
            }

            settingsRepository.updateTweaksState(
                refreshRate = RefreshRateMode.DYNAMIC,
                speedMode = false,
                animationScale = 1.0f,
                phantomGuard = false,
                thermalOverride = false
            )
            TweakActionResult(true, loc.getString(R.string.msg_restore_ok))
        }
    }
}
