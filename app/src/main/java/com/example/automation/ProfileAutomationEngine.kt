package com.example.automation

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.PowerManager
import android.os.Process
import com.example.data.AppPreferencesState
import com.example.data.AutomationStatus
import com.example.data.BatteryTelemetry
import com.example.data.ProfileId
import com.example.data.SettingsRepository
import com.example.data.TriggerReason
import com.example.hyperos.HyperOSTweaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

class ProfileAutomationEngine(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val tweaker: HyperOSTweaker
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val powerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val appOpsManager =
        context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    private val _status = MutableStateFlow(AutomationStatus())
    val status: StateFlow<AutomationStatus> = _status.asStateFlow()

    private var lastAppliedProfileId: ProfileId? = null
    private var lastForegroundPkg: String? = null
    private var lastEventQueryTime: Long = System.currentTimeMillis() - 60_000L
    private var latestPrefs: AppPreferencesState? = null
    private var latestTelemetry: BatteryTelemetry = BatteryTelemetry()
    private var thermalCutoffActive = false
    private var pollingJob: Job? = null

    var onThermalCutoffTriggered: ((Float) -> Unit)? = null

    fun start() {
        if (pollingJob?.isActive == true) return
        scope.launch {
            settingsRepository.preferencesFlow.collect { prefs ->
                latestPrefs = prefs
                evaluateAndApply()
            }
        }
        pollingJob = scope.launch {
            while (isActive) {
                if (powerManager.isInteractive) {
                    val fg = detectForegroundPackage()
                    if (fg != null && fg != lastForegroundPkg) {
                        lastForegroundPkg = fg
                        evaluateAndApply()
                    }
                }
                delay(2000L)
            }
        }
    }

    fun onBatteryTelemetryChanged(telemetry: BatteryTelemetry) {
        latestTelemetry = telemetry
        scope.launch {
            evaluateAndApply()
        }
    }

    suspend fun manuallyApplyProfile(profileId: ProfileId) {
        val prefs = latestPrefs ?: settingsRepository.preferencesFlow.first()
        val config = prefs.profiles[profileId] ?: return
        lastAppliedProfileId = profileId
        settingsRepository.updateActiveProfile(profileId, updateDefault = true)
        tweaker.applyProfile(config, latestTelemetry.temperatureC)
        _status.value = _status.value.copy(
            activeProfileId = profileId,
            triggerReason = TriggerReason.Manual
        )
    }

    private fun hasUsageStatsPermission(): Boolean {
        val mode = appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun detectForegroundPackage(): String? {
        if (!hasUsageStatsPermission()) return lastForegroundPkg
        val now = System.currentTimeMillis()
        val begin = (lastEventQueryTime - 5000L).coerceAtLeast(now - 120_000L)
        val events = try {
            usageStatsManager.queryEvents(begin, now)
        } catch (_: Throwable) {
            return lastForegroundPkg
        }
        val event = UsageEvents.Event()
        var detectedPkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                val pkg = event.packageName
                if (!pkg.isNullOrBlank()) {
                    detectedPkg = pkg
                }
            }
        }
        lastEventQueryTime = now
        return detectedPkg ?: lastForegroundPkg
    }

    private fun isWithinQuietHours(startMinutes: Int, endMinutes: Int): Boolean {
        val cal = Calendar.getInstance()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return if (startMinutes <= endMinutes) {
            nowMinutes in startMinutes until endMinutes
        } else {
            nowMinutes >= startMinutes || nowMinutes < endMinutes
        }
    }

    suspend fun evaluateAndApply() {
        val prefs = latestPrefs ?: settingsRepository.preferencesFlow.first()
        val tempC = latestTelemetry.temperatureC

        // 1. Highest Priority: Thermal Safety Override (> 43.0°C)
        if (tempC > 43.0f) {
            if (!thermalCutoffActive || prefs.thermalOverrideEnabled) {
                thermalCutoffActive = true
                tweaker.emergencyThermalReset()
                onThermalCutoffTriggered?.invoke(tempC)
            }
            val safeTarget = if (latestTelemetry.isCharging) ProfileId.COOL_CHARGING else ProfileId.BATTERY_SAVER
            applyIfChanged(prefs, safeTarget, TriggerReason.ThermalSafety, isThermalTripped = true)
            return
        } else if (thermalCutoffActive && tempC <= 40.5f) {
            thermalCutoffActive = false
        }

        // 2. Charging Trigger
        if (prefs.chargeCoolTriggerEnabled && latestTelemetry.isCharging) {
            applyIfChanged(prefs, ProfileId.COOL_CHARGING, TriggerReason.ChargingCool, isThermalTripped = false)
            return
        }

        // 3. Per-App Foreground Trigger
        val fgPkg = lastForegroundPkg
        if (prefs.perAppTriggerEnabled &&
            !fgPkg.isNullOrBlank() &&
            prefs.selectedGamingPackages.contains(fgPkg)
        ) {
            applyIfChanged(prefs, ProfileId.GAMING, TriggerReason.ForegroundApp(fgPkg), isThermalTripped = false)
            return
        }

        // 4. Low Battery Trigger
        if (prefs.lowBatteryProfileTriggerEnabled &&
            !latestTelemetry.isCharging &&
            latestTelemetry.level in 1..prefs.lowBatteryProfileThreshold
        ) {
            applyIfChanged(
                prefs,
                ProfileId.BATTERY_SAVER,
                TriggerReason.LowBattery(latestTelemetry.level, prefs.lowBatteryProfileThreshold),
                isThermalTripped = false
            )
            return
        }

        // 5. Time Schedule (Quiet Hours)
        if (prefs.quietHoursEnabled && isWithinQuietHours(prefs.quietStartMinutes, prefs.quietEndMinutes)) {
            applyIfChanged(prefs, ProfileId.BATTERY_SAVER, TriggerReason.QuietHours, isThermalTripped = false)
            return
        }

        // 6. Default Profile
        applyIfChanged(prefs, prefs.defaultProfileId, TriggerReason.UserDefault, isThermalTripped = false)
    }

    private suspend fun applyIfChanged(
        prefs: AppPreferencesState,
        targetId: ProfileId,
        reason: TriggerReason,
        isThermalTripped: Boolean
    ) {
        _status.value = AutomationStatus(
            activeProfileId = targetId,
            triggerReason = reason,
            foregroundPackage = lastForegroundPkg,
            thermalSafetyTripped = isThermalTripped
        )
        if (lastAppliedProfileId != targetId) {
            lastAppliedProfileId = targetId
            val config = prefs.profiles[targetId] ?: return
            settingsRepository.updateActiveProfile(targetId, updateDefault = false)
            tweaker.applyProfile(config, latestTelemetry.temperatureC)
        }
    }
}
