package com.example

import android.app.Application
import android.content.Context
import com.example.automation.ProfileAutomationEngine
import com.example.data.AppLanguage
import com.example.data.BatteryTelemetry
import com.example.data.SettingsRepository
import com.example.hyperos.HyperOSTweaker
import com.example.shizuku.ShizukuManager
import com.example.util.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HyperApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var shizukuManager: ShizukuManager
        private set
    lateinit var hyperOSTweaker: HyperOSTweaker
        private set
    lateinit var automationEngine: ProfileAutomationEngine
        private set

    private val _batteryTelemetry = MutableStateFlow(BatteryTelemetry())
    val batteryTelemetry: StateFlow<BatteryTelemetry> = _batteryTelemetry.asStateFlow()

    @Volatile
    private var currentLanguage: AppLanguage = AppLanguage.SYSTEM

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        shizukuManager = ShizukuManager(this) { localizedContext() }
        hyperOSTweaker = HyperOSTweaker(this, settingsRepository, shizukuManager) { localizedContext() }
        automationEngine = ProfileAutomationEngine(this, settingsRepository, hyperOSTweaker)

        appScope.launch {
            settingsRepository.preferencesFlow.collect { prefs ->
                currentLanguage = prefs.appLanguage
            }
        }
    }

    fun localizedContext(): Context = LocaleHelper.localizedContext(this, currentLanguage)

    fun updateBatteryTelemetry(telemetry: BatteryTelemetry) {
        _batteryTelemetry.value = telemetry
    }

    override fun onTerminate() {
        shizukuManager.release()
        super.onTerminate()
    }
}
