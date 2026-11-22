package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AppLanguage
import com.example.data.AppPreferencesState
import com.example.data.AutomationStatus
import com.example.data.BatteryTelemetry
import com.example.ui.components.LanguageSwitcherButton
import com.example.ui.theme.GaugePercentStyle
import com.example.ui.theme.HyperAmber
import com.example.ui.theme.HyperGreenBright
import com.example.ui.theme.HyperRed
import com.example.ui.theme.TelemetryNumberStyle
import kotlin.math.roundToInt

@Composable
fun OverviewBatteryScreen(
    telemetry: BatteryTelemetry,
    prefs: AppPreferencesState,
    automationStatus: AutomationStatus,
    onLanguageSelected: (AppLanguage) -> Unit,
    onUpperTargetChange: (Int) -> Unit,
    onLowBatteryAlertChange: (Boolean, Int) -> Unit,
    onTestAlarm: () -> Unit,
    onDismissAlarm: () -> Unit,
    onSnoozeAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeProfile = prefs.profiles[automationStatus.activeProfileId]
    val activeProfileName = activeProfile?.resolveName(context)
        ?: stringResource(automationStatus.activeProfileId.defaultTitleRes)
    val triggerText = automationStatus.triggerReason.resolveText(context)

    val tempColor = when {
        telemetry.temperatureC >= 42.0f -> HyperRed
        telemetry.temperatureC >= 37.0f -> HyperAmber
        else -> HyperGreenBright
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.overview_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                LanguageSwitcherButton(
                    currentLanguage = prefs.appLanguage,
                    onLanguageSelected = onLanguageSelected
                )
            }
        }

        // Active Profile Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    1.dp,
                    if (automationStatus.thermalSafetyTripped) HyperRed.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outline
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (automationStatus.thermalSafetyTripped) {
                        HyperRed.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (automationStatus.thermalSafetyTripped) {
                            Icons.Rounded.Warning
                        } else {
                            Icons.Rounded.Speed
                        },
                        contentDescription = null,
                        tint = if (automationStatus.thermalSafetyTripped) {
                            HyperRed
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.overview_active_profile, activeProfileName),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.overview_triggered_by, triggerText),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Active Ringing Alarm Banner
        item {
            AnimatedVisibility(
                visible = telemetry.isAlarmRinging,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, HyperRed.copy(alpha = 0.6f)),
                    colors = CardDefaults.cardColors(containerColor = HyperRed.copy(alpha = 0.16f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = HyperRed
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = telemetry.alarmReason?.resolveText(context)
                                    ?: stringResource(R.string.overview_alarm_ringing_fallback),
                                style = MaterialTheme.typography.titleMedium,
                                color = HyperRed
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onDismissAlarm,
                                colors = ButtonDefaults.buttonColors(containerColor = HyperRed),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("dismiss_alarm_button")
                            ) {
                                Icon(Icons.Rounded.NotificationsOff, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_dismiss_alarm))
                            }
                            OutlinedButton(
                                onClick = onSnoozeAlarm,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("snooze_alarm_button")
                            ) {
                                Icon(Icons.Rounded.Snooze, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_snooze_alarm))
                            }
                        }
                    }
                }
            }
        }

        // Animated Circular Battery Gauge
        item {
            val gaugeAccent = when {
                telemetry.isCharging && telemetry.level >= prefs.upperChargeTarget -> HyperAmber
                telemetry.isCharging -> HyperGreenBright
                telemetry.level <= 20 -> HyperRed
                else -> MaterialTheme.colorScheme.primary
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    gaugeAccent.copy(alpha = 0.14f),
                                    Color.Transparent
                                ),
                                radius = 420f
                            )
                        )
                        .padding(22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BatteryCircularGauge(
                            level = telemetry.level,
                            isCharging = telemetry.isCharging,
                            chargeSourceText = stringResource(telemetry.chargeSource.labelRes),
                            gaugeColor = gaugeAccent
                        )
                        if (telemetry.isSnoozed) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = HyperAmber.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, HyperAmber.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = stringResource(R.string.overview_snoozed_badge),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HyperAmber,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live 2x2 Telemetry Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryStatCard(
                        icon = Icons.Rounded.Bolt,
                        label = stringResource(R.string.stat_power),
                        value = "${"%.1f".format(telemetry.watts)} W",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryStatCard(
                        icon = Icons.Rounded.ElectricBolt,
                        label = stringResource(R.string.stat_current),
                        value = "${telemetry.currentMa} mA",
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryStatCard(
                        icon = Icons.Rounded.Thermostat,
                        label = stringResource(R.string.stat_temp),
                        value = "${"%.1f".format(telemetry.temperatureC)} °C",
                        accent = tempColor,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryStatCard(
                        icon = Icons.Rounded.BatteryFull,
                        label = stringResource(R.string.stat_voltage),
                        value = "${"%.2f".format(telemetry.voltageV)} V",
                        accent = HyperGreenBright,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Upper Charge Alarm Slider Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.upper_target_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${prefs.upperChargeTarget}%",
                            style = TelemetryNumberStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = stringResource(R.string.upper_target_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = prefs.upperChargeTarget.toFloat(),
                        onValueChange = { onUpperTargetChange(it.roundToInt()) },
                        valueRange = 50f..100f,
                        steps = 49,
                        modifier = Modifier.testTag("upper_charge_target_slider")
                    )
                    Button(
                        onClick = onTestAlarm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("test_alarm_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_test_alarm))
                    }
                }
            }
        }

        // Low Battery Alert Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.low_alert_title, prefs.lowBatteryAlertLevel),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.low_alert_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = prefs.lowBatteryAlertEnabled,
                            onCheckedChange = { enabled ->
                                onLowBatteryAlertChange(enabled, prefs.lowBatteryAlertLevel)
                            },
                            modifier = Modifier.testTag("low_battery_alert_switch")
                        )
                    }
                    Slider(
                        value = prefs.lowBatteryAlertLevel.toFloat(),
                        onValueChange = {
                            onLowBatteryAlertChange(prefs.lowBatteryAlertEnabled, it.roundToInt())
                        },
                        enabled = prefs.lowBatteryAlertEnabled,
                        valueRange = 10f..40f,
                        steps = 29,
                        modifier = Modifier.testTag("low_battery_alert_slider")
                    )
                }
            }
        }
    }
}

@Composable
private fun BatteryCircularGauge(
    level: Int,
    isCharging: Boolean,
    chargeSourceText: String,
    gaugeColor: Color
) {
    val animatedSweep by animateFloatAsState(
        targetValue = (level.coerceIn(0, 100) / 100f) * 360f,
        animationSpec = tween(durationMillis = 700),
        label = "batterySweep"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier.size(196.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 15.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = gaugeColor,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Subtle highlight cap dot at top
            drawCircle(
                color = gaugeColor.copy(alpha = 0.25f),
                radius = strokeWidth * 0.65f,
                center = Offset(size.width / 2f, strokeWidth / 2f)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                contentDescription = chargeSourceText,
                tint = gaugeColor,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "$level%",
                style = GaugePercentStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = chargeSourceText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TelemetryStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.16f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = TelemetryNumberStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
