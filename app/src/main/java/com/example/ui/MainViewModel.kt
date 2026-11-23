package com.example.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HyperApp
import com.example.R
import com.example.data.AppLanguage
import com.example.data.AppPreferencesState
import com.example.data.ArtCompileMode
import com.example.data.ArtCompileProgress
import com.example.data.AutomationStatus
import com.example.data.BatteryTelemetry
import com.example.data.DebloatPackageState
import com.example.data.HyperAnimRate
import com.example.data.InstalledAppItem
import com.example.data.PermissionStatusSnapshot
import com.example.data.PrivateDnsPreset
import com.example.data.ProfileConfig
import com.example.data.ProfileId
import com.example.data.RefreshRateMode
import com.example.data.ShellLogEntry
import com.example.service.BatteryMonitorService
import com.example.shizuku.ShizukuStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HyperApp

    val preferences: StateFlow<AppPreferencesState> = app.settingsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferencesState())

    val batteryTelemetry: StateFlow<BatteryTelemetry> = app.batteryTelemetry

    val automationStatus: StateFlow<AutomationStatus> = app.automationEngine.status

    val shizukuStatus: StateFlow<ShizukuStatus> = app.shizukuManager.status

    val commandLogs: StateFlow<List<ShellLogEntry>> = app.shizukuManager.commandLogs

    private val _permissions = MutableStateFlow(app.hyperOSTweaker.readPermissionStatus())
    val permissions: StateFlow<PermissionStatusSnapshot> = _permissions.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private val _appsLoading = MutableStateFlow(false)
    val appsLoading: StateFlow<Boolean> = _appsLoading.asStateFlow()

    private val _debloatStates = MutableStateFlow<List<DebloatPackageState>>(emptyList())
    val debloatStates: StateFlow<List<DebloatPackageState>> = _debloatStates.asStateFlow()

    private val _debloatLoading = MutableStateFlow(false)
    val debloatLoading: StateFlow<Boolean> = _debloatLoading.asStateFlow()

    private val _artProgress = MutableStateFlow(ArtCompileProgress())
    val artProgress: StateFlow<ArtCompileProgress> = _artProgress.asStateFlow()

    private var artBatchJob: Job? = null

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            app.settingsRepository.updateAppLanguage(language)
        }
    }

    fun refreshPermissionsAndShizuku() {
        app.shizukuManager.refreshStatus()
        _permissions.value = app.hyperOSTweaker.readPermissionStatus()
    }

    fun requestShizukuPermission() {
        app.shizukuManager.requestPermission()
    }

    fun grantSelfPermissionsViaShizuku() {
        viewModelScope.launch {
            val loc = app.localizedContext()
            if (!app.shizukuManager.isBinderAlive()) {
                _snackbarMessages.tryEmit(loc.getString(R.string.msg_shizuku_not_running))
                return@launch
            }
            if (!app.shizukuManager.hasPermission()) {
                app.shizukuManager.requestPermission()
                _snackbarMessages.tryEmit(loc.getString(R.string.msg_shizuku_grant_prompt))
                return@launch
            }
            val result = app.shizukuManager.grantPersistentPermissions()
            refreshPermissionsAndShizuku()
            if (result.isSuccess) {
                _snackbarMessages.tryEmit(loc.getString(R.string.msg_perms_granted_ok))
            } else {
                _snackbarMessages.tryEmit(loc.getString(R.string.msg_perms_granted_fail, result.output))
            }
        }
    }

    fun setUpperChargeTarget(percent: Int) {
        viewModelScope.launch {
            app.settingsRepository.updateUpperChargeTarget(percent)
        }
    }

    fun setLowBatteryAlert(enabled: Boolean, level: Int) {
        viewModelScope.launch {
            app.settingsRepository.updateLowBatteryAlert(enabled, level)
        }
    }

    fun testAlarm() {
        sendServiceAction(BatteryMonitorService.ACTION_TEST_ALARM)
    }

    fun dismissAlarm() {
        sendServiceAction(BatteryMonitorService.ACTION_DISMISS_ALARM)
    }

    fun snoozeAlarm() {
        sendServiceAction(BatteryMonitorService.ACTION_SNOZE_ALARM)
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(app, BatteryMonitorService::class.java).apply {
            this.action = action
        }
        try {
            app.startForegroundService(intent)
        } catch (_: Throwable) {
            try {
                app.startService(intent)
            } catch (_: Throwable) {
            }
        }
    }

    fun applyProfileNow(profileId: ProfileId) {
        viewModelScope.launch {
            val loc = app.localizedContext()
            app.automationEngine.manuallyApplyProfile(profileId)
            val name = preferences.value.profiles[profileId]?.resolveName(loc)
                ?: loc.getString(profileId.defaultTitleRes)
            _snackbarMessages.tryEmit(loc.getString(R.string.msg_profile_applied, name))
        }
    }

    fun saveProfileConfig(config: ProfileConfig) {
        viewModelScope.launch {
            val loc = app.localizedContext()
            app.settingsRepository.updateProfileConfig(config)
            if (automationStatus.value.activeProfileId == config.id) {
                app.hyperOSTweaker.applyProfile(config, batteryTelemetry.value.temperatureC)
            }
            _snackbarMessages.tryEmit(loc.getString(R.string.msg_profile_saved, config.resolveName(loc)))
        }
    }

    fun setPerAppTrigger(enabled: Boolean) {
        viewModelScope.launch { app.settingsRepository.updatePerAppTrigger(enabled) }
    }

    fun setGamingPackages(packages: Set<String>) {
        viewModelScope.launch {
            app.settingsRepository.updateGamingPackages(packages)
            if (preferences.value.gameDriverEnabled) {
                app.hyperOSTweaker.setGameDriverOptIn(true, packages)
            }
        }
    }

    fun setChargeCoolTrigger(enabled: Boolean) {
        viewModelScope.launch { app.settingsRepository.updateChargeCoolTrigger(enabled) }
    }

    fun setLowBatteryProfileTrigger(enabled: Boolean, threshold: Int) {
        viewModelScope.launch { app.settingsRepository.updateLowBatteryProfileTrigger(enabled, threshold) }
    }

    fun setQuietHours(enabled: Boolean, startMinutes: Int, endMinutes: Int) {
        viewModelScope.launch { app.settingsRepository.updateQuietHours(enabled, startMinutes, endMinutes) }
    }

    fun setRefreshRate(mode: RefreshRateMode) {
        viewModelScope.launch {
            val res = app.hyperOSTweaker.setRefreshRate(mode)
            _snackbarMessages.tryEmit(res.message)
        }
    }

    fun setSpeedMode(enabled: Boolean) {
        viewModelScope.launch {
            val res = app.hyperOSTweaker.setSpeedMode(enabled)
            _snackbarMessages.tryEmit(res.message)
        }
    }

    fun setMiuiHomeAnimRate(rate: HyperAnimRate) {
        viewModelScope.launch {
            val res = app.hyperOSTweaker.setMiuiHomeAnimRate(rate)
            _snackbarMessages.tryEmit(res.message)
        }
    }

    fun setAnimationScale(scale: Float) {
        viewModelScope.launch {
            val res = app.hyperOSTweaker.setAnimationScale(scale)
            _snackbarMessages.tryEmit(res.message)
        }
    }

    fun setGameDriverEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val res = app.hyperOSTweaker.setGameDriverOptIn(enabled, preferences.value.selectedGamingPackages)
            _snackbarMessages.tryEmit(res.message)
        }
    }

    fun setPhantomGuard(enabled: Boolean) {
        viewModelScope.launch {
            val res = app.hyperOSTweaker.setPhantomProcessGuard(enabled)
            _snackbarMessages.tryEmit(res.message)
        }
    }

    fun setThermalOverride(enabled: Boolean) {
        viewModelScope.launch {
            val res = app.hyperOSTweaker.setThermalOverride(enabled, batteryTelemetry.value.temperatureC)
            _snackbarMessages.tryEmit(res.message)
        }
    }

    fun setCachedAppFreezer(enabled: Boolean) {
        viewModelScope.launch {
            val res = app.hyperOSTweaker.setCachedAppFreezer(enabled)
            _snackbarMessages.tryEmit(res.message)
        }
    }

    fun setPrivateDns(preset: PrivateDnsPreset) {
        viewModelScope.launch {
            val res = app.hyperOSTweaker.setPrivateDns(preset)
            _snackbarMessages.tryEmit(res.message)
        }
    }

    fun setWifiScanDisabled(disabled: Boolean) {
        viewModelScope.launch {
            val res = app.hyperOSTweaker.setWifiScanDisabled(disabled)
            _snackbarMessages.tryEmit(res.message)
        }
    }

    fun setBleScanDisabled(disabled: Boolean) {
        viewModelScope.launch {
            val res = app.hyperOSTweaker.setBleScanDisabled(disabled)
            _snackbarMessages.tryEmit(res.message)
        }
    }

    fun toggleAppBackgroundRestriction(packageName: String, restricted: Boolean) {
        viewModelScope.launch {
            val res = app.hyperOSTweaker.setAppBackgroundRestricted(packageName, restricted)
            _snackbarMessages.tryEmit(res.message)
        }
    }

    fun refreshDebloatStates(force: Boolean = true) {
        if (!force && _debloatStates.value.isNotEmpty()) return
        if (_debloatLoading.value) return
        viewModelScope.launch {
            _debloatLoading.value = true
            _debloatStates.value = app.hyperOSTweaker.inspectDebloatCatalog()
            _debloatLoading.value = false
        }
    }

    fun toggleDebloatPackage(packageName: String, freeze: Boolean) {
        viewModelScope.launch {
            val res = app.hyperOSTweaker.setPackageFrozen(packageName, freeze)
            _snackbarMessages.tryEmit(res.message)
            refreshDebloatStates(force = true)
        }
    }

    fun freezeSafePack() {
        viewModelScope.launch {
            if (_debloatStates.value.isEmpty()) {
                _debloatStates.value = app.hyperOSTweaker.inspectDebloatCatalog()
            }
            val res = app.hyperOSTweaker.freezeSafePack(_debloatStates.value)
            _snackbarMessages.tryEmit(res.message)
            refreshDebloatStates(force = true)
        }
    }

    fun restoreAllAppFrozen() {
        viewModelScope.launch {
            val res = app.hyperOSTweaker.restoreAllAppFrozen()
            _snackbarMessages.tryEmit(res.message)
            refreshDebloatStates(force = true)
        }
    }

    fun compileSingleApp(packageName: String, mode: ArtCompileMode) {
        if (_artProgress.value.isRunning) return
        viewModelScope.launch {
            _artProgress.value = ArtCompileProgress(
                isRunning = true,
                currentPackage = packageName,
                completed = 0,
                total = 1
            )
            val res = app.hyperOSTweaker.compileSinglePackage(packageName, mode)
            _artProgress.value = ArtCompileProgress(isRunning = false)
            _snackbarMessages.tryEmit(res.message)
        }
    }

    fun compileAllUserApps(mode: ArtCompileMode) {
        if (_artProgress.value.isRunning) return
        artBatchJob = viewModelScope.launch {
            loadInstalledAppsSynchronouslyIfEmpty()
            val targets = _installedApps.value
            if (targets.isEmpty()) return@launch

            var okCount = 0
            _artProgress.value = ArtCompileProgress(
                isRunning = true,
                currentPackage = targets.first().appName,
                completed = 0,
                total = targets.size
            )
            try {
                targets.forEachIndexed { idx, item ->
                    _artProgress.value = ArtCompileProgress(
                        isRunning = true,
                        currentPackage = item.appName,
                        completed = idx,
                        total = targets.size
                    )
                    val res = app.hyperOSTweaker.compileSinglePackage(item.packageName, mode)
                    if (res.success) okCount++
                }
                val loc = app.localizedContext()
                _snackbarMessages.tryEmit(
                    loc.getString(R.string.msg_art_batch_done, okCount, targets.size, mode.flag)
                )
            } catch (_: CancellationException) {
            } finally {
                _artProgress.value = ArtCompileProgress(isRunning = false)
            }
        }
    }

    fun cancelArtCompilation() {
        artBatchJob?.cancel()
        artBatchJob = null
        _artProgress.value = ArtCompileProgress(isRunning = false)
    }

    fun runBgDexoptJob() {
        if (_artProgress.value.isRunning) return
        viewModelScope.launch {
            _artProgress.value = ArtCompileProgress(
                isRunning = true,
                currentPackage = "bg-dexopt-job",
                completed = 0,
                total = 1
            )
            val res = app.hyperOSTweaker.runBgDexoptJob()
            _artProgress.value = ArtCompileProgress(isRunning = false)
            _snackbarMessages.tryEmit(res.message)
        }
    }

    fun trimSystemCaches() {
        viewModelScope.launch {
            val res = app.hyperOSTweaker.trimSystemCaches()
            _snackbarMessages.tryEmit(res.message)
        }
    }

    fun clearCommandLogs() {
        app.shizukuManager.clearCommandLogs()
    }

    fun restoreStockDefaults() {
        viewModelScope.launch {
            val res = app.hyperOSTweaker.restoreStockDefaults()
            _snackbarMessages.tryEmit(res.message)
            refreshDebloatStates(force = true)
        }
    }

    fun loadInstalledAppsIfNeeded() {
        if (_installedApps.value.isNotEmpty() || _appsLoading.value) return
        viewModelScope.launch {
            loadInstalledAppsSynchronouslyIfEmpty()
        }
    }

    private suspend fun loadInstalledAppsSynchronouslyIfEmpty() {
        if (_installedApps.value.isNotEmpty()) return
        _appsLoading.value = true
        val list = withContext(Dispatchers.IO) {
            val pm = app.packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            apps.filter { info ->
                pm.getLaunchIntentForPackage(info.packageName) != null &&
                    info.packageName != app.packageName &&
                    ((info.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                        (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
            }.map { info ->
                val label = runCatching { pm.getApplicationLabel(info).toString() }
                    .getOrDefault(info.packageName)
                val bmp = runCatching {
                    pm.getApplicationIcon(info).toBitmap(width = 96, height = 96)
                }.getOrNull()
                InstalledAppItem(
                    packageName = info.packageName,
                    appName = label,
                    iconBitmap = bmp
                )
            }.sortedBy { it.appName.lowercase() }
        }
        _installedApps.value = list
        _appsLoading.value = false
    }
}
