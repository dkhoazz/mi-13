package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.HyperApp
import com.example.MainActivity
import com.example.R
import com.example.data.AlarmReason
import com.example.data.AppLanguage
import com.example.data.AppPreferencesState
import com.example.data.BatteryTelemetry
import com.example.data.ChargeSourceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class BatteryMonitorService : Service() {

    companion object {
        const val ACTION_START = "com.example.hyperbattery.action.START"
        const val ACTION_DISMISS_ALARM = "com.example.hyperbattery.action.DISMISS_ALARM"
        const val ACTION_SNOZE_ALARM = "com.example.hyperbattery.action.SNOZE_ALARM"
        const val ACTION_TEST_ALARM = "com.example.hyperbattery.action.TEST_ALARM"

        private const val CHANNEL_MONITOR = "channel_battery_monitor"
        private const val CHANNEL_ALARM = "channel_battery_alarm"
        private const val CHANNEL_THERMAL = "channel_thermal_safety"

        private const val NOTIF_ID_MONITOR = 1001
        private const val NOTIF_ID_ALARM = 1002
        private const val NOTIF_ID_LOW_BATT = 1003
        private const val NOTIF_ID_THERMAL = 1004
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var app: HyperApp
    private lateinit var notificationManager: NotificationManager
    private lateinit var batteryManager: BatteryManager

    private var latestPrefs = AppPreferencesState()
    private var lastAppliedLanguage: AppLanguage? = null
    private var mediaPlayer: MediaPlayer? = null
    private var alarmRinging = false
    private var currentAlarmReason: AlarmReason? = null
    private var dismissedThisChargeCycle = false
    private var snoozeUntilMs = 0L
    private var lowAlertLatched = false
    private var overheatAlertLatched = false
    private var thermalCutoffNotifLatched = false

    private var pollJob: Job? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                processBatteryIntent(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        app = application as HyperApp
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        createNotificationChannels(app.localizedContext())

        app.automationEngine.onThermalCutoffTriggered = { tempC ->
            val loc = app.localizedContext()
            postThermalSafetyNotification(
                loc.getString(R.string.notif_thermal_cutoff_title, tempC),
                loc.getString(R.string.notif_thermal_cutoff_body),
                isCutoffAlert = true
            )
        }
        app.automationEngine.start()

        serviceScope.launch {
            app.settingsRepository.preferencesFlow.collect { prefs ->
                latestPrefs = prefs
                if (lastAppliedLanguage != prefs.appLanguage) {
                    lastAppliedLanguage = prefs.appLanguage
                    createNotificationChannels(app.localizedContext())
                }
                evaluateAlarmRules(app.batteryTelemetry.value)
                updateMonitorNotification(app.batteryTelemetry.value)
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val sticky = ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        if (sticky != null) {
            processBatteryIntent(sticky)
        }

        pollJob = serviceScope.launch {
            while (isActive) {
                val stickyIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                if (stickyIntent != null) {
                    processBatteryIntent(stickyIntent)
                }
                delay(2500L)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        when (intent?.action) {
            ACTION_DISMISS_ALARM -> {
                dismissedThisChargeCycle = true
                snoozeUntilMs = 0L
                stopAlarmSoundAndVibration()
                notificationManager.cancel(NOTIF_ID_ALARM)
            }

            ACTION_SNOZE_ALARM -> {
                snoozeUntilMs = System.currentTimeMillis() + 5 * 60 * 1000L
                stopAlarmSoundAndVibration()
                notificationManager.cancel(NOTIF_ID_ALARM)
                updateTelemetryAlarmState(ringing = false, snoozed = true, reason = null)
            }

            ACTION_TEST_ALARM -> {
                val loc = app.localizedContext()
                triggerChargeAlarm(
                    reason = AlarmReason.TestAlarm,
                    title = loc.getString(R.string.notif_test_alarm_title),
                    body = loc.getString(R.string.notif_test_alarm_body)
                )
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopAlarmSoundAndVibration()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Throwable) {
        }
        pollJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startAsForeground() {
        val notification = buildMonitorNotification(app.batteryTelemetry.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID_MONITOR,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID_MONITOR, notification)
        }
    }

    private fun createNotificationChannels(loc: Context) {
        val monitorChannel = NotificationChannel(
            CHANNEL_MONITOR,
            loc.getString(R.string.notif_channel_monitor_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = loc.getString(R.string.notif_channel_monitor_desc)
            setShowBadge(false)
        }

        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            loc.getString(R.string.notif_channel_alarm_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = loc.getString(R.string.notif_channel_alarm_desc)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val thermalChannel = NotificationChannel(
            CHANNEL_THERMAL,
            loc.getString(R.string.notif_channel_thermal_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = loc.getString(R.string.notif_channel_thermal_desc)
            enableVibration(true)
        }

        notificationManager.createNotificationChannel(monitorChannel)
        notificationManager.createNotificationChannel(alarmChannel)
        notificationManager.createNotificationChannel(thermalChannel)
    }

    private fun processBatteryIntent(intent: Intent) {
        val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val level = if (rawLevel >= 0) ((rawLevel * 100f) / scale).toInt().coerceIn(0, 100) else 0

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val isCharging = plugged != 0 ||
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val chargeSource = when {
            !isCharging -> ChargeSourceType.BATTERY
            plugged == BatteryManager.BATTERY_PLUGGED_AC -> ChargeSourceType.AC
            plugged == BatteryManager.BATTERY_PLUGGED_USB -> ChargeSourceType.USB
            plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargeSourceType.WIRELESS
            else -> ChargeSourceType.GENERIC
        }

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 300)
        val temperatureC = tempTenths / 10f

        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 3850)
        val voltageV = voltageMv / 1000f

        val rawCurrent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val rawAbs = abs(rawCurrent)
        val currentMa = if (rawCurrent == Int.MIN_VALUE) 0 else if (rawAbs > 10_000) rawAbs / 1000 else rawAbs
        val watts = (voltageV * currentMa) / 1000f

        val telemetry = BatteryTelemetry(
            level = level,
            isCharging = isCharging,
            chargeSource = chargeSource,
            temperatureC = temperatureC,
            voltageV = voltageV,
            currentMa = currentMa,
            watts = watts,
            isAlarmRinging = alarmRinging,
            isSnoozed = snoozeUntilMs > System.currentTimeMillis(),
            alarmReason = currentAlarmReason
        )

        app.updateBatteryTelemetry(telemetry)
        app.automationEngine.onBatteryTelemetryChanged(telemetry)
        evaluateAlarmRules(telemetry)
        updateMonitorNotification(telemetry)
    }

    private fun evaluateAlarmRules(t: BatteryTelemetry) {
        val loc = app.localizedContext()
        val target = latestPrefs.upperChargeTarget

        if (!t.isCharging || t.level <= target - 2) {
            if (dismissedThisChargeCycle || alarmRinging || snoozeUntilMs > 0L) {
                dismissedThisChargeCycle = false
                snoozeUntilMs = 0L
                if (alarmRinging) {
                    stopAlarmSoundAndVibration()
                    notificationManager.cancel(NOTIF_ID_ALARM)
                }
            }
        }

        val now = System.currentTimeMillis()
        if (t.isCharging && t.level >= target && !dismissedThisChargeCycle && now >= snoozeUntilMs) {
            if (!alarmRinging) {
                val reason = AlarmReason.ChargeTargetReached(t.level, target)
                triggerChargeAlarm(
                    reason = reason,
                    title = loc.getString(R.string.notif_charge_alarm_title, t.level, target),
                    body = loc.getString(R.string.notif_charge_alarm_body)
                )
            }
        }

        if (latestPrefs.lowBatteryAlertEnabled && !t.isCharging && t.level in 1..latestPrefs.lowBatteryAlertLevel) {
            if (!lowAlertLatched) {
                lowAlertLatched = true
                postLowBatteryNotification(loc, t.level, latestPrefs.lowBatteryAlertLevel)
            }
        } else if (t.isCharging || t.level >= latestPrefs.lowBatteryAlertLevel + 3) {
            lowAlertLatched = false
        }

        if (t.isCharging && t.temperatureC > 42.0f) {
            if (!overheatAlertLatched) {
                overheatAlertLatched = true
                postThermalSafetyNotification(
                    loc.getString(R.string.notif_overheat_charge_title, t.temperatureC),
                    loc.getString(R.string.notif_overheat_charge_body),
                    isCutoffAlert = false
                )
                playWarningChime()
            }
        } else if (t.temperatureC < 40.5f) {
            overheatAlertLatched = false
        }

        if (t.temperatureC <= 40.5f) {
            thermalCutoffNotifLatched = false
        }
    }

    private fun triggerChargeAlarm(reason: AlarmReason, title: String, body: String) {
        alarmRinging = true
        currentAlarmReason = reason
        updateTelemetryAlarmState(ringing = true, snoozed = false, reason = reason)
        postAlarmNotification(title, body)
        startAlarmSoundAndVibration()
    }

    private fun updateTelemetryAlarmState(ringing: Boolean, snoozed: Boolean, reason: AlarmReason?) {
        val current = app.batteryTelemetry.value
        app.updateBatteryTelemetry(
            current.copy(
                isAlarmRinging = ringing,
                isSnoozed = snoozed,
                alarmReason = reason
            )
        )
    }

    private fun startAlarmSoundAndVibration() {
        stopAlarmSoundAndVibration(updateState = false)

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        if (alarmUri != null) {
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(applicationContext, alarmUri)
                    isLooping = true
                    prepare()
                    start()
                }
                mediaPlayer = player
            } catch (_: Throwable) {
            }
        }

        try {
            val pattern = longArrayOf(0, 600, 350, 600, 350)
            val effect = VibrationEffect.createWaveform(pattern, 0)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(effect, audioAttrs)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(effect, audioAttrs)
            }
        } catch (_: Throwable) {
        }
    }

    private fun stopAlarmSoundAndVibration(updateState: Boolean = true) {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Throwable) {
        }
        mediaPlayer = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.cancel()
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.cancel()
            }
        } catch (_: Throwable) {
        }

        if (updateState) {
            alarmRinging = false
            currentAlarmReason = null
            updateTelemetryAlarmState(
                ringing = false,
                snoozed = snoozeUntilMs > System.currentTimeMillis(),
                reason = null
            )
        }
    }

    private fun playWarningChime() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
            val ringtone = RingtoneManager.getRingtone(applicationContext, uri)
            ringtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            ringtone?.play()
        } catch (_: Throwable) {
        }
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun serviceActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, BatteryMonitorService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildMonitorNotification(t: BatteryTelemetry): Notification {
        val loc = app.localizedContext()
        val activeId = app.automationEngine.status.value.activeProfileId
        val profileName = latestPrefs.profiles[activeId]?.resolveName(loc)
            ?: loc.getString(latestPrefs.activeProfileId.defaultTitleRes)
        val sourceText = loc.getString(t.chargeSource.labelRes)
        val title = loc.getString(R.string.notif_monitor_title, t.level, sourceText, t.temperatureC)
        val content = if (t.isCharging) {
            loc.getString(
                R.string.notif_monitor_charging,
                t.watts,
                t.currentMa,
                latestPrefs.upperChargeTarget,
                profileName
            )
        } else {
            loc.getString(
                R.string.notif_monitor_discharging,
                t.voltageV,
                t.currentMa,
                profileName
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_battery_monitor_stat)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppPendingIntent())
            .build()
    }

    private fun updateMonitorNotification(t: BatteryTelemetry) {
        notificationManager.notify(NOTIF_ID_MONITOR, buildMonitorNotification(t))
    }

    private fun postAlarmNotification(title: String, body: String) {
        val loc = app.localizedContext()
        val dismissPi = serviceActionPendingIntent(ACTION_DISMISS_ALARM, 101)
        val snoozePi = serviceActionPendingIntent(ACTION_SNOZE_ALARM, 102)

        val notification = NotificationCompat.Builder(this, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_battery_monitor_stat)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openAppPendingIntent())
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                loc.getString(R.string.btn_dismiss_alarm),
                dismissPi
            )
            .addAction(
                android.R.drawable.ic_popup_sync,
                loc.getString(R.string.btn_snooze_alarm),
                snoozePi
            )
            .build()

        notificationManager.notify(NOTIF_ID_ALARM, notification)
    }

    private fun postLowBatteryNotification(loc: Context, level: Int, threshold: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_battery_monitor_stat)
            .setContentTitle(loc.getString(R.string.notif_low_batt_title, level))
            .setContentText(loc.getString(R.string.notif_low_batt_body, threshold))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent())
            .build()
        notificationManager.notify(NOTIF_ID_LOW_BATT, notification)
    }

    private fun postThermalSafetyNotification(title: String, body: String, isCutoffAlert: Boolean) {
        if (isCutoffAlert) {
            if (thermalCutoffNotifLatched) return
            thermalCutoffNotifLatched = true
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_THERMAL)
            .setSmallIcon(R.drawable.ic_battery_monitor_stat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent())
            .build()

        notificationManager.notify(NOTIF_ID_THERMAL, notification)
    }
}
