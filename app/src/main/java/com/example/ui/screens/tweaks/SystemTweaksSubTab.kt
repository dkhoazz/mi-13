package com.example.ui.screens.tweaks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AppPreferencesState
import com.example.data.HyperAnimRate
import com.example.data.PermissionStatusSnapshot
import com.example.data.RefreshRateMode
import com.example.shizuku.ShizukuStatus
import com.example.ui.theme.HyperAmber
import com.example.ui.theme.HyperGreenBright
import com.example.ui.theme.HyperRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SystemTweaksSubTab(
    prefs: AppPreferencesState,
    shizukuStatus: ShizukuStatus,
    permissions: PermissionStatusSnapshot,
    currentTempC: Float,
    onRequestShizuku: () -> Unit,
    onGrantPersistentPermissions: () -> Unit,
    onRefreshRateChange: (RefreshRateMode) -> Unit,
    onSpeedModeChange: (Boolean) -> Unit,
    onMiuiHomeAnimRateChange: (HyperAnimRate) -> Unit,
    onAnimationScaleChange: (Float) -> Unit,
    onGameDriverChange: (Boolean) -> Unit,
    onPhantomGuardChange: (Boolean) -> Unit,
    onThermalOverrideChange: (Boolean) -> Unit,
    onRestoreDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Permission Status Card
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tweaks_perms_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusChip(
                            label = stringResource(
                                when (shizukuStatus) {
                                    ShizukuStatus.READY -> R.string.shizuku_ready
                                    ShizukuStatus.CONNECTING -> R.string.shizuku_connecting
                                    ShizukuStatus.NO_PERMISSION -> R.string.shizuku_no_perm
                                    ShizukuStatus.NOT_RUNNING -> R.string.shizuku_not_running
                                }
                            ),
                            granted = shizukuStatus == ShizukuStatus.READY
                        )
                        StatusChip(
                            label = "WRITE_SECURE_SETTINGS",
                            granted = permissions.hasWriteSecureSettings
                        )
                        StatusChip(
                            label = "WRITE_SETTINGS",
                            granted = permissions.hasWriteSettings
                        )
                        StatusChip(
                            label = "USAGE_STATS",
                            granted = permissions.hasUsageStats
                        )
                    }

                    if (shizukuStatus == ShizukuStatus.NO_PERMISSION) {
                        OutlinedButton(
                            onClick = onRequestShizuku,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("request_shizuku_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(stringResource(R.string.btn_request_shizuku))
                        }
                    }

                    Button(
                        onClick = onGrantPersistentPermissions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("grant_via_shizuku_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_grant_via_shizuku))
                    }
                    Text(
                        text = stringResource(R.string.grant_via_shizuku_desc),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Refresh Rate Control
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tweak_rr_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.tweak_rr_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        RefreshRateMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = prefs.currentRefreshRate == mode,
                                onClick = { onRefreshRateChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = RefreshRateMode.entries.size
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("refresh_rate_${mode.name.lowercase()}")
                            ) {
                                Text(stringResource(mode.labelRes))
                            }
                        }
                    }
                }
            }
        }

        // Performance Mode (speed_mode)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.tweak_speed_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.tweak_speed_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = prefs.currentSpeedMode,
                        onCheckedChange = onSpeedModeChange,
                        modifier = Modifier.testTag("speed_mode_switch")
                    )
                }
            }
        }

        // HyperOS Launcher Animation Speed (miui_home_animation_rate)
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tweak_home_anim_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.tweak_home_anim_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        HyperAnimRate.entries.forEachIndexed { index, rate ->
                            SegmentedButton(
                                selected = prefs.miuiHomeAnimRate == rate,
                                onClick = { onMiuiHomeAnimRateChange(rate) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = HyperAnimRate.entries.size
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("home_anim_${rate.name.lowercase()}")
                            ) {
                                Text(stringResource(rate.labelRes))
                            }
                        }
                    }
                }
            }
        }

        // System Animation Scale
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tweak_anim_title, prefs.currentAnimationScale.toString()),
                        style = MaterialTheme.typography.titleMedium
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.0f, 0.5f, 0.75f, 1.0f).forEach { scale ->
                            FilterChip(
                                selected = prefs.currentAnimationScale == scale,
                                onClick = { onAnimationScaleChange(scale) },
                                label = { Text("${scale}x") },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("anim_scale_$scale")
                            )
                        }
                    }
                }
            }
        }

        // Production Game Driver Opt-In
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SportsEsports,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.tweak_game_driver_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(
                                R.string.tweak_game_driver_desc,
                                prefs.selectedGamingPackages.size
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = prefs.gameDriverEnabled,
                        onCheckedChange = onGameDriverChange,
                        modifier = Modifier.testTag("game_driver_switch")
                    )
                }
            }
        }

        // Phantom Process Killer Guard
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.tweak_phantom_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.tweak_phantom_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = prefs.phantomGuardEnabled,
                        onCheckedChange = onPhantomGuardChange,
                        modifier = Modifier.testTag("phantom_guard_switch")
                    )
                }
            }
        }

        // Thermal Override with >43.0°C Safety Cutoff Warning
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    1.dp,
                    if (currentTempC > 43.0f) HyperRed.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.outline
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentTempC > 43.0f) {
                        HyperRed.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.tweak_thermal_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.tweak_thermal_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = prefs.thermalOverrideEnabled,
                            onCheckedChange = onThermalOverrideChange,
                            modifier = Modifier.testTag("thermal_override_switch")
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = if (currentTempC > 43.0f) HyperRed else HyperAmber
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.tweak_thermal_warning, currentTempC),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (currentTempC > 43.0f) HyperRed else HyperAmber
                        )
                    }
                }
            }
        }

        // Restore Stock Defaults
        item {
            OutlinedButton(
                onClick = onRestoreDefaults,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("restore_defaults_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Rounded.Restore, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_restore_defaults))
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, granted: Boolean) {
    val color = if (granted) HyperGreenBright else HyperRed
    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = {
            Icon(
                imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                contentDescription = null,
                tint = color
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.12f)
        ),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = color.copy(alpha = 0.4f))
    )
}
