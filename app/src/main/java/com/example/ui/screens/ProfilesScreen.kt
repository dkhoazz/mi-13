package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AppLanguage
import com.example.data.AppPreferencesState
import com.example.data.AutomationStatus
import com.example.data.InstalledAppItem
import com.example.data.ProfileConfig
import com.example.data.ProfileId
import com.example.data.RefreshRateMode
import com.example.ui.components.LanguageSwitcherButton
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ProfilesScreen(
    prefs: AppPreferencesState,
    automationStatus: AutomationStatus,
    installedApps: List<InstalledAppItem>,
    appsLoading: Boolean,
    onLanguageSelected: (AppLanguage) -> Unit,
    onApplyProfileNow: (ProfileId) -> Unit,
    onSaveProfileConfig: (ProfileConfig) -> Unit,
    onPerAppToggle: (Boolean) -> Unit,
    onOpenAppSelector: () -> Unit,
    onGamingPackagesSave: (Set<String>) -> Unit,
    onChargeCoolToggle: (Boolean) -> Unit,
    onLowBatteryRuleChange: (Boolean, Int) -> Unit,
    onQuietHoursChange: (Boolean, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var editingProfile by remember { mutableStateOf<ProfileConfig?>(null) }
    var showAppPickerDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.profiles_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = stringResource(R.string.profiles_priority_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                LanguageSwitcherButton(
                    currentLanguage = prefs.appLanguage,
                    onLanguageSelected = onLanguageSelected
                )
            }
        }

        items(ProfileId.entries) { profileId ->
            val config = prefs.profiles[profileId] ?: return@items
            val isActive = automationStatus.activeProfileId == profileId
            val activeReason = if (isActive) automationStatus.triggerReason.resolveText(context) else null
            ProfileCard(
                config = config,
                isActive = isActive,
                activeReason = activeReason,
                onApplyNow = { onApplyProfileNow(profileId) },
                onEdit = { editingProfile = config }
            )
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.profiles_rules_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Per-App Foreground Trigger Card
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.rule_per_app_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.rule_per_app_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = prefs.perAppTriggerEnabled,
                            onCheckedChange = onPerAppToggle,
                            modifier = Modifier.testTag("per_app_trigger_switch")
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            onOpenAppSelector()
                            showAppPickerDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("select_gaming_apps_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Apps, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_select_apps, prefs.selectedGamingPackages.size))
                    }
                }
            }
        }

        // Charge Cool Trigger & Low Battery Trigger
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.rule_charge_cool_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.rule_charge_cool_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = prefs.chargeCoolTriggerEnabled,
                            onCheckedChange = onChargeCoolToggle,
                            modifier = Modifier.testTag("charge_cool_trigger_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.rule_low_batt_title, prefs.lowBatteryProfileThreshold),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.rule_low_batt_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = prefs.lowBatteryProfileTriggerEnabled,
                            onCheckedChange = {
                                onLowBatteryRuleChange(it, prefs.lowBatteryProfileThreshold)
                            },
                            modifier = Modifier.testTag("low_batt_profile_trigger_switch")
                        )
                    }

                    Slider(
                        value = prefs.lowBatteryProfileThreshold.toFloat(),
                        onValueChange = {
                            onLowBatteryRuleChange(prefs.lowBatteryProfileTriggerEnabled, it.roundToInt())
                        },
                        enabled = prefs.lowBatteryProfileTriggerEnabled,
                        valueRange = 10f..40f,
                        steps = 29,
                        modifier = Modifier.testTag("low_batt_profile_slider")
                    )
                }
            }
        }

        // Quiet Hours Schedule Card
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.rule_quiet_hours_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.rule_quiet_hours_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = prefs.quietHoursEnabled,
                            onCheckedChange = {
                                onQuietHoursChange(it, prefs.quietStartMinutes, prefs.quietEndMinutes)
                            },
                            modifier = Modifier.testTag("quiet_hours_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val h = prefs.quietStartMinutes / 60
                                val m = prefs.quietStartMinutes % 60
                                TimePickerDialog(
                                    context,
                                    { _, selH, selM ->
                                        onQuietHoursChange(
                                            prefs.quietHoursEnabled,
                                            selH * 60 + selM,
                                            prefs.quietEndMinutes
                                        )
                                    },
                                    h,
                                    m,
                                    true
                                ).show()
                            },
                            enabled = prefs.quietHoursEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("quiet_hours_start_button")
                        ) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_quiet_start, formatMinutes(prefs.quietStartMinutes)))
                        }

                        OutlinedButton(
                            onClick = {
                                val h = prefs.quietEndMinutes / 60
                                val m = prefs.quietEndMinutes % 60
                                TimePickerDialog(
                                    context,
                                    { _, selH, selM ->
                                        onQuietHoursChange(
                                            prefs.quietHoursEnabled,
                                            prefs.quietStartMinutes,
                                            selH * 60 + selM
                                        )
                                    },
                                    h,
                                    m,
                                    true
                                ).show()
                            },
                            enabled = prefs.quietHoursEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("quiet_hours_end_button")
                        ) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_quiet_end, formatMinutes(prefs.quietEndMinutes)))
                        }
                    }
                }
            }
        }
    }

    editingProfile?.let { profile ->
        ProfileEditDialog(
            initial = profile,
            onDismiss = { editingProfile = null },
            onSave = { updated ->
                onSaveProfileConfig(updated)
                editingProfile = null
            }
        )
    }

    if (showAppPickerDialog) {
        AppSelectorDialog(
            installedApps = installedApps,
            loading = appsLoading,
            selectedPackages = prefs.selectedGamingPackages,
            onDismiss = { showAppPickerDialog = false },
            onSave = { updated ->
                onGamingPackagesSave(updated)
                showAppPickerDialog = false
            }
        )
    }
}

@Composable
private fun ProfileCard(
    config: ProfileConfig,
    isActive: Boolean,
    activeReason: String?,
    onApplyNow: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val icon = when (config.id) {
        ProfileId.GAMING -> Icons.Rounded.SportsEsports
        ProfileId.BALANCED -> Icons.Rounded.Balance
        ProfileId.BATTERY_SAVER -> Icons.Rounded.Bedtime
        ProfileId.COOL_CHARGING -> Icons.Rounded.AcUnit
    }
    val profileTitle = config.resolveName(context)
    val rrLabel = stringResource(config.refreshRate.labelRes)
    val speedLabel = stringResource(if (config.speedMode) R.string.profile_speed_on else R.string.profile_speed_off)
    val baseSummary = stringResource(R.string.profile_summary_fmt, rrLabel, speedLabel, config.animationScale.toString())
    val summaryText = buildString {
        append(baseSummary)
        if (config.phantomGuard) append(context.getString(R.string.profile_tag_phantom))
        if (config.thermalOverride) append(context.getString(R.string.profile_tag_thermal))
        if (config.gameDriverOptIn) append(context.getString(R.string.profile_tag_gamedriver))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.outline
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profileTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isActive) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (activeReason != null) {
                        Text(
                            text = stringResource(R.string.profile_in_use, activeReason),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("edit_profile_${config.id.name.lowercase()}")
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.cd_edit_profile))
                }
            }

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onApplyNow,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("apply_profile_${config.id.name.lowercase()}")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_apply_now))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileEditDialog(
    initial: ProfileConfig,
    onDismiss: () -> Unit,
    onSave: (ProfileConfig) -> Unit
) {
    val context = LocalContext.current
    var refreshRate by remember { mutableStateOf(initial.refreshRate) }
    var speedMode by remember { mutableStateOf(initial.speedMode) }
    var animScale by remember { mutableFloatStateOf(initial.animationScale) }
    var phantomGuard by remember { mutableStateOf(initial.phantomGuard) }
    var thermalOverride by remember { mutableStateOf(initial.thermalOverride) }
    var gameDriverOptIn by remember { mutableStateOf(initial.gameDriverOptIn) }
    var cachedFreezer by remember { mutableStateOf(initial.cachedFreezer) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.dialog_edit_title, initial.resolveName(context))) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.label_refresh_rate), style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RefreshRateMode.entries.forEach { mode ->
                        FilterChip(
                            selected = refreshRate == mode,
                            onClick = { refreshRate = mode },
                            label = { Text(stringResource(mode.labelRes)) }
                        )
                    }
                }

                Text(stringResource(R.string.label_anim_scale), style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.0f, 0.5f, 0.75f, 1.0f).forEach { scale ->
                        FilterChip(
                            selected = animScale == scale,
                            onClick = { animScale = scale },
                            label = { Text("${scale}x") }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.label_speed_mode), modifier = Modifier.weight(1f))
                    Switch(checked = speedMode, onCheckedChange = { speedMode = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.label_phantom_guard), modifier = Modifier.weight(1f))
                    Switch(checked = phantomGuard, onCheckedChange = { phantomGuard = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.label_thermal_override), modifier = Modifier.weight(1f))
                    Switch(checked = thermalOverride, onCheckedChange = { thermalOverride = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.label_game_driver), modifier = Modifier.weight(1f))
                    Switch(checked = gameDriverOptIn, onCheckedChange = { gameDriverOptIn = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.label_cached_freezer), modifier = Modifier.weight(1f))
                    Switch(checked = cachedFreezer, onCheckedChange = { cachedFreezer = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            refreshRate = refreshRate,
                            speedMode = speedMode,
                            animationScale = animScale,
                            phantomGuard = phantomGuard,
                            thermalOverride = thermalOverride,
                            gameDriverOptIn = gameDriverOptIn,
                            cachedFreezer = cachedFreezer
                        )
                    )
                },
                modifier = Modifier.testTag("profile_edit_save_button")
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
private fun AppSelectorDialog(
    installedApps: List<InstalledAppItem>,
    loading: Boolean,
    selectedPackages: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var workingSelection by remember { mutableStateOf(selectedPackages) }

    val filtered = remember(installedApps, query) {
        if (query.isBlank()) installedApps
        else installedApps.filter {
            it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.dialog_apps_title, workingSelection.size)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.hint_search_apps)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_selector_search_input")
                )
                Spacer(Modifier.height(10.dp))
                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.empty_apps_search),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                    ) {
                        items(filtered, key = { it.packageName }) { item ->
                            val checked = workingSelection.contains(item.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .clickable {
                                        workingSelection = if (checked) {
                                            workingSelection - item.packageName
                                        } else {
                                            workingSelection + item.packageName
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.iconBitmap != null) {
                                    Image(
                                        bitmap = item.iconBitmap.asImageBitmap(),
                                        contentDescription = item.appName,
                                        modifier = Modifier.size(36.dp)
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {}
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.appName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = item.packageName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        workingSelection = if (isChecked) {
                                            workingSelection + item.packageName
                                        } else {
                                            workingSelection - item.packageName
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(workingSelection) },
                modifier = Modifier.testTag("app_selector_done_button")
            ) {
                Text(stringResource(R.string.btn_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}

private fun formatMinutes(totalMinutes: Int): String {
    val h = (totalMinutes / 60) % 24
    val m = totalMinutes % 60
    return String.format(Locale.getDefault(), "%02d:%02d", h, m)
}
