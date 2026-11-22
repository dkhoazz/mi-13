package com.example.data

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.StringRes
import com.example.R

enum class AppLanguage(val code: String, @StringRes val labelRes: Int, @StringRes val shortRes: Int) {
    SYSTEM("system", R.string.lang_system, R.string.lang_short_system),
    VI("vi", R.string.lang_vi, R.string.lang_short_vi),
    EN("en", R.string.lang_en, R.string.lang_short_en)
}

enum class RefreshRateMode(@StringRes val labelRes: Int) {
    DYNAMIC(R.string.rr_dynamic),
    HZ_60(R.string.rr_60hz),
    HZ_120(R.string.rr_120hz)
}

enum class ProfileId(
    @StringRes val defaultTitleRes: Int,
    @StringRes val subtitleRes: Int
) {
    GAMING(R.string.profile_gaming_title, R.string.profile_gaming_sub),
    BALANCED(R.string.profile_balanced_title, R.string.profile_balanced_sub),
    BATTERY_SAVER(R.string.profile_saver_title, R.string.profile_saver_sub),
    COOL_CHARGING(R.string.profile_cool_title, R.string.profile_cool_sub)
}

data class ProfileConfig(
    val id: ProfileId,
    val customName: String? = null,
    val refreshRate: RefreshRateMode,
    val speedMode: Boolean,
    val animationScale: Float,
    val phantomGuard: Boolean,
    val thermalOverride: Boolean
) {
    fun resolveName(context: Context): String =
        customName?.takeIf { it.isNotBlank() } ?: context.getString(id.defaultTitleRes)
}

data class StockSystemSnapshot(
    val isCaptured: Boolean = false,
    val userRefreshRate: String? = null,
    val minRefreshRate: String? = null,
    val peakRefreshRate: String? = null,
    val miuiRefreshRate: String? = null,
    val speedMode: String? = null,
    val windowAnimationScale: String? = null,
    val transitionAnimationScale: String? = null,
    val animatorDurationScale: String? = null
)

data class AppPreferencesState(
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val upperChargeTarget: Int = 80,
    val lowBatteryAlertEnabled: Boolean = true,
    val lowBatteryAlertLevel: Int = 20,
    val activeProfileId: ProfileId = ProfileId.BALANCED,
    val defaultProfileId: ProfileId = ProfileId.BALANCED,
    val perAppTriggerEnabled: Boolean = true,
    val selectedGamingPackages: Set<String> = emptySet(),
    val chargeCoolTriggerEnabled: Boolean = true,
    val lowBatteryProfileTriggerEnabled: Boolean = true,
    val lowBatteryProfileThreshold: Int = 20,
    val quietHoursEnabled: Boolean = true,
    val quietStartMinutes: Int = 23 * 60 + 30,
    val quietEndMinutes: Int = 6 * 60 + 30,
    val phantomGuardEnabled: Boolean = false,
    val thermalOverrideEnabled: Boolean = false,
    val currentRefreshRate: RefreshRateMode = RefreshRateMode.DYNAMIC,
    val currentSpeedMode: Boolean = false,
    val currentAnimationScale: Float = 1.0f,
    val profiles: Map<ProfileId, ProfileConfig> = defaultProfilesMap()
)

fun defaultProfilesMap(): Map<ProfileId, ProfileConfig> = mapOf(
    ProfileId.GAMING to ProfileConfig(
        id = ProfileId.GAMING,
        refreshRate = RefreshRateMode.HZ_120,
        speedMode = true,
        animationScale = 0.5f,
        phantomGuard = true,
        thermalOverride = true
    ),
    ProfileId.BALANCED to ProfileConfig(
        id = ProfileId.BALANCED,
        refreshRate = RefreshRateMode.DYNAMIC,
        speedMode = false,
        animationScale = 0.75f,
        phantomGuard = true,
        thermalOverride = false
    ),
    ProfileId.BATTERY_SAVER to ProfileConfig(
        id = ProfileId.BATTERY_SAVER,
        refreshRate = RefreshRateMode.HZ_60,
        speedMode = false,
        animationScale = 1.0f,
        phantomGuard = false,
        thermalOverride = false
    ),
    ProfileId.COOL_CHARGING to ProfileConfig(
        id = ProfileId.COOL_CHARGING,
        refreshRate = RefreshRateMode.HZ_60,
        speedMode = false,
        animationScale = 0.5f,
        phantomGuard = false,
        thermalOverride = false
    )
)

enum class ChargeSourceType(@StringRes val labelRes: Int) {
    BATTERY(R.string.charge_source_battery),
    AC(R.string.charge_source_ac),
    USB(R.string.charge_source_usb),
    WIRELESS(R.string.charge_source_wireless),
    GENERIC(R.string.charge_source_generic)
}

sealed interface AlarmReason {
    data class ChargeTargetReached(val level: Int, val target: Int) : AlarmReason
    data object TestAlarm : AlarmReason

    fun resolveText(context: Context): String = when (this) {
        is ChargeTargetReached -> context.getString(R.string.notif_charge_alarm_title, level, target)
        TestAlarm -> context.getString(R.string.notif_test_alarm_title)
    }
}

sealed interface TriggerReason {
    data object DefaultInit : TriggerReason
    data object Manual : TriggerReason
    data object ThermalSafety : TriggerReason
    data object ChargingCool : TriggerReason
    data class ForegroundApp(val packageName: String) : TriggerReason
    data class LowBattery(val level: Int, val threshold: Int) : TriggerReason
    data object QuietHours : TriggerReason
    data object UserDefault : TriggerReason

    fun resolveText(context: Context): String = when (this) {
        DefaultInit -> context.getString(R.string.trigger_default)
        Manual -> context.getString(R.string.trigger_manual)
        ThermalSafety -> context.getString(R.string.trigger_thermal)
        ChargingCool -> context.getString(R.string.trigger_charging)
        is ForegroundApp -> context.getString(R.string.trigger_app, packageName)
        is LowBattery -> context.getString(R.string.trigger_low_battery, level, threshold)
        QuietHours -> context.getString(R.string.trigger_quiet_hours)
        UserDefault -> context.getString(R.string.trigger_user_default)
    }
}

data class BatteryTelemetry(
    val level: Int = 0,
    val isCharging: Boolean = false,
    val chargeSource: ChargeSourceType = ChargeSourceType.BATTERY,
    val temperatureC: Float = 30.0f,
    val voltageV: Float = 3.85f,
    val currentMa: Int = 0,
    val watts: Float = 0.0f,
    val isAlarmRinging: Boolean = false,
    val isSnoozed: Boolean = false,
    val alarmReason: AlarmReason? = null
)

data class AutomationStatus(
    val activeProfileId: ProfileId = ProfileId.BALANCED,
    val triggerReason: TriggerReason = TriggerReason.DefaultInit,
    val foregroundPackage: String? = null,
    val thermalSafetyTripped: Boolean = false
)

data class PermissionStatusSnapshot(
    val hasWriteSecureSettings: Boolean = false,
    val hasWriteSettings: Boolean = false,
    val hasUsageStats: Boolean = false,
    val hasPostNotifications: Boolean = true,
    val isIgnoringBatteryOptimizations: Boolean = false
)

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val iconBitmap: Bitmap? = null
)
