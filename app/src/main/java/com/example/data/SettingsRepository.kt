package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hyperos_optimizer_prefs")

class SettingsRepository(private val context: Context) {

    companion object {
        const val UNSET_SENTINEL = "__UNSET__"

        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_UPPER_CHARGE_TARGET = intPreferencesKey("upper_charge_target")
        private val KEY_LOW_BATTERY_ALERT_ENABLED = booleanPreferencesKey("low_battery_alert_enabled")
        private val KEY_LOW_BATTERY_ALERT_LEVEL = intPreferencesKey("low_battery_alert_level")
        private val KEY_ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
        private val KEY_DEFAULT_PROFILE_ID = stringPreferencesKey("default_profile_id")

        private val KEY_PER_APP_TRIGGER_ENABLED = booleanPreferencesKey("per_app_trigger_enabled")
        private val KEY_GAMING_PACKAGES = stringSetPreferencesKey("gaming_packages")
        private val KEY_CHARGE_COOL_TRIGGER = booleanPreferencesKey("charge_cool_trigger")
        private val KEY_LOW_BATT_PROFILE_TRIGGER = booleanPreferencesKey("low_batt_profile_trigger")
        private val KEY_LOW_BATT_PROFILE_THRESHOLD = intPreferencesKey("low_batt_profile_threshold")
        private val KEY_QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        private val KEY_QUIET_START_MINUTES = intPreferencesKey("quiet_start_minutes")
        private val KEY_QUIET_END_MINUTES = intPreferencesKey("quiet_end_minutes")

        private val KEY_PHANTOM_GUARD = booleanPreferencesKey("phantom_guard_enabled")
        private val KEY_THERMAL_OVERRIDE = booleanPreferencesKey("thermal_override_enabled")
        private val KEY_CURRENT_REFRESH_RATE = stringPreferencesKey("current_refresh_rate")
        private val KEY_CURRENT_SPEED_MODE = booleanPreferencesKey("current_speed_mode")
        private val KEY_CURRENT_ANIM_SCALE = floatPreferencesKey("current_anim_scale")

        private val KEY_SNAP_CAPTURED = booleanPreferencesKey("snap_captured")
        private val KEY_SNAP_USER_RR = stringPreferencesKey("snap_user_rr")
        private val KEY_SNAP_MIN_RR = stringPreferencesKey("snap_min_rr")
        private val KEY_SNAP_PEAK_RR = stringPreferencesKey("snap_peak_rr")
        private val KEY_SNAP_MIUI_RR = stringPreferencesKey("snap_miui_rr")
        private val KEY_SNAP_SPEED_MODE = stringPreferencesKey("snap_speed_mode")
        private val KEY_SNAP_WIN_ANIM = stringPreferencesKey("snap_win_anim")
        private val KEY_SNAP_TRANS_ANIM = stringPreferencesKey("snap_trans_anim")
        private val KEY_SNAP_ANIMATOR_DUR = stringPreferencesKey("snap_animator_dur")
    }

    private fun profileRefreshKey(id: ProfileId) = stringPreferencesKey("prof_${id.name}_rr")
    private fun profileSpeedKey(id: ProfileId) = booleanPreferencesKey("prof_${id.name}_speed")
    private fun profileAnimKey(id: ProfileId) = floatPreferencesKey("prof_${id.name}_anim")
    private fun profilePhantomKey(id: ProfileId) = booleanPreferencesKey("prof_${id.name}_phantom")
    private fun profileThermalKey(id: ProfileId) = booleanPreferencesKey("prof_${id.name}_thermal")
    private fun profileNameKey(id: ProfileId) = stringPreferencesKey("prof_${id.name}_custom_name")

    val preferencesFlow: Flow<AppPreferencesState> = context.dataStore.data.map { prefs ->
        val defaults = defaultProfilesMap()
        val profiles = ProfileId.entries.associateWith { id ->
            val def = defaults.getValue(id)
            val rr = prefs[profileRefreshKey(id)]?.let {
                runCatching { RefreshRateMode.valueOf(it) }.getOrNull()
            } ?: def.refreshRate
            ProfileConfig(
                id = id,
                customName = prefs[profileNameKey(id)],
                refreshRate = rr,
                speedMode = prefs[profileSpeedKey(id)] ?: def.speedMode,
                animationScale = prefs[profileAnimKey(id)] ?: def.animationScale,
                phantomGuard = prefs[profilePhantomKey(id)] ?: def.phantomGuard,
                thermalOverride = prefs[profileThermalKey(id)] ?: def.thermalOverride
            )
        }

        val activeId = prefs[KEY_ACTIVE_PROFILE_ID]?.let {
            runCatching { ProfileId.valueOf(it) }.getOrNull()
        } ?: ProfileId.BALANCED

        val defaultId = prefs[KEY_DEFAULT_PROFILE_ID]?.let {
            runCatching { ProfileId.valueOf(it) }.getOrNull()
        } ?: ProfileId.BALANCED

        val currentRr = prefs[KEY_CURRENT_REFRESH_RATE]?.let {
            runCatching { RefreshRateMode.valueOf(it) }.getOrNull()
        } ?: RefreshRateMode.DYNAMIC

        val language = prefs[KEY_APP_LANGUAGE]?.let {
            runCatching { AppLanguage.valueOf(it) }.getOrNull()
        } ?: AppLanguage.SYSTEM

        AppPreferencesState(
            appLanguage = language,
            upperChargeTarget = (prefs[KEY_UPPER_CHARGE_TARGET] ?: 80).coerceIn(50, 100),
            lowBatteryAlertEnabled = prefs[KEY_LOW_BATTERY_ALERT_ENABLED] ?: true,
            lowBatteryAlertLevel = (prefs[KEY_LOW_BATTERY_ALERT_LEVEL] ?: 20).coerceIn(10, 40),
            activeProfileId = activeId,
            defaultProfileId = defaultId,
            perAppTriggerEnabled = prefs[KEY_PER_APP_TRIGGER_ENABLED] ?: true,
            selectedGamingPackages = prefs[KEY_GAMING_PACKAGES] ?: emptySet(),
            chargeCoolTriggerEnabled = prefs[KEY_CHARGE_COOL_TRIGGER] ?: true,
            lowBatteryProfileTriggerEnabled = prefs[KEY_LOW_BATT_PROFILE_TRIGGER] ?: true,
            lowBatteryProfileThreshold = (prefs[KEY_LOW_BATT_PROFILE_THRESHOLD] ?: 20).coerceIn(10, 40),
            quietHoursEnabled = prefs[KEY_QUIET_HOURS_ENABLED] ?: true,
            quietStartMinutes = prefs[KEY_QUIET_START_MINUTES] ?: (23 * 60 + 30),
            quietEndMinutes = prefs[KEY_QUIET_END_MINUTES] ?: (6 * 60 + 30),
            phantomGuardEnabled = prefs[KEY_PHANTOM_GUARD] ?: false,
            thermalOverrideEnabled = prefs[KEY_THERMAL_OVERRIDE] ?: false,
            currentRefreshRate = currentRr,
            currentSpeedMode = prefs[KEY_CURRENT_SPEED_MODE] ?: false,
            currentAnimationScale = prefs[KEY_CURRENT_ANIM_SCALE] ?: 1.0f,
            profiles = profiles
        )
    }

    suspend fun updateAppLanguage(language: AppLanguage) {
        context.dataStore.edit { it[KEY_APP_LANGUAGE] = language.name }
    }

    suspend fun getSnapshot(): StockSystemSnapshot {
        val prefs = context.dataStore.data.first()
        return StockSystemSnapshot(
            isCaptured = prefs[KEY_SNAP_CAPTURED] ?: false,
            userRefreshRate = prefs[KEY_SNAP_USER_RR],
            minRefreshRate = prefs[KEY_SNAP_MIN_RR],
            peakRefreshRate = prefs[KEY_SNAP_PEAK_RR],
            miuiRefreshRate = prefs[KEY_SNAP_MIUI_RR],
            speedMode = prefs[KEY_SNAP_SPEED_MODE],
            windowAnimationScale = prefs[KEY_SNAP_WIN_ANIM],
            transitionAnimationScale = prefs[KEY_SNAP_TRANS_ANIM],
            animatorDurationScale = prefs[KEY_SNAP_ANIMATOR_DUR]
        )
    }

    suspend fun saveSnapshot(snapshot: StockSystemSnapshot) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SNAP_CAPTURED] = true
            prefs[KEY_SNAP_USER_RR] = snapshot.userRefreshRate ?: UNSET_SENTINEL
            prefs[KEY_SNAP_MIN_RR] = snapshot.minRefreshRate ?: UNSET_SENTINEL
            prefs[KEY_SNAP_PEAK_RR] = snapshot.peakRefreshRate ?: UNSET_SENTINEL
            prefs[KEY_SNAP_MIUI_RR] = snapshot.miuiRefreshRate ?: UNSET_SENTINEL
            prefs[KEY_SNAP_SPEED_MODE] = snapshot.speedMode ?: UNSET_SENTINEL
            prefs[KEY_SNAP_WIN_ANIM] = snapshot.windowAnimationScale ?: UNSET_SENTINEL
            prefs[KEY_SNAP_TRANS_ANIM] = snapshot.transitionAnimationScale ?: UNSET_SENTINEL
            prefs[KEY_SNAP_ANIMATOR_DUR] = snapshot.animatorDurationScale ?: UNSET_SENTINEL
        }
    }

    suspend fun updateUpperChargeTarget(percent: Int) {
        context.dataStore.edit { it[KEY_UPPER_CHARGE_TARGET] = percent.coerceIn(50, 100) }
    }

    suspend fun updateLowBatteryAlert(enabled: Boolean, level: Int) {
        context.dataStore.edit {
            it[KEY_LOW_BATTERY_ALERT_ENABLED] = enabled
            it[KEY_LOW_BATTERY_ALERT_LEVEL] = level.coerceIn(10, 40)
        }
    }

    suspend fun updateActiveProfile(profileId: ProfileId, updateDefault: Boolean = false) {
        context.dataStore.edit {
            it[KEY_ACTIVE_PROFILE_ID] = profileId.name
            if (updateDefault) {
                it[KEY_DEFAULT_PROFILE_ID] = profileId.name
            }
        }
    }

    suspend fun updateProfileConfig(config: ProfileConfig) {
        context.dataStore.edit {
            if (config.customName.isNullOrBlank()) {
                it.remove(profileNameKey(config.id))
            } else {
                it[profileNameKey(config.id)] = config.customName
            }
            it[profileRefreshKey(config.id)] = config.refreshRate.name
            it[profileSpeedKey(config.id)] = config.speedMode
            it[profileAnimKey(config.id)] = config.animationScale
            it[profilePhantomKey(config.id)] = config.phantomGuard
            it[profileThermalKey(config.id)] = config.thermalOverride
        }
    }

    suspend fun updatePerAppTrigger(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PER_APP_TRIGGER_ENABLED] = enabled }
    }

    suspend fun updateGamingPackages(packages: Set<String>) {
        context.dataStore.edit { it[KEY_GAMING_PACKAGES] = packages }
    }

    suspend fun updateChargeCoolTrigger(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CHARGE_COOL_TRIGGER] = enabled }
    }

    suspend fun updateLowBatteryProfileTrigger(enabled: Boolean, threshold: Int) {
        context.dataStore.edit {
            it[KEY_LOW_BATT_PROFILE_TRIGGER] = enabled
            it[KEY_LOW_BATT_PROFILE_THRESHOLD] = threshold.coerceIn(10, 40)
        }
    }

    suspend fun updateQuietHours(enabled: Boolean, startMinutes: Int, endMinutes: Int) {
        context.dataStore.edit {
            it[KEY_QUIET_HOURS_ENABLED] = enabled
            it[KEY_QUIET_START_MINUTES] = startMinutes
            it[KEY_QUIET_END_MINUTES] = endMinutes
        }
    }

    suspend fun updateTweaksState(
        refreshRate: RefreshRateMode? = null,
        speedMode: Boolean? = null,
        animationScale: Float? = null,
        phantomGuard: Boolean? = null,
        thermalOverride: Boolean? = null
    ) {
        context.dataStore.edit { prefs ->
            refreshRate?.let { prefs[KEY_CURRENT_REFRESH_RATE] = it.name }
            speedMode?.let { prefs[KEY_CURRENT_SPEED_MODE] = it }
            animationScale?.let { prefs[KEY_CURRENT_ANIM_SCALE] = it }
            phantomGuard?.let { prefs[KEY_PHANTOM_GUARD] = it }
            thermalOverride?.let { prefs[KEY_THERMAL_OVERRIDE] = it }
        }
    }
}
