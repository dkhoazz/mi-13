package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AppLanguage
import com.example.data.PermissionStatusSnapshot
import com.example.data.ShellLogEntry
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.LanguageSwitcherButton
import com.example.ui.theme.HyperAmber
import com.example.ui.theme.HyperGreenBright
import com.example.ui.theme.HyperRed
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun AntiKillGuideScreen(
    currentLanguage: AppLanguage,
    permissions: PermissionStatusSnapshot,
    shizukuStatus: ShizukuStatus,
    commandLogs: List<ShellLogEntry>,
    onLanguageSelected: (AppLanguage) -> Unit,
    onRequestPostNotifications: () -> Unit,
    onRequestShizuku: () -> Unit,
    onRefreshStatus: () -> Unit,
    onClearCommandLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
                        text = stringResource(R.string.antikill_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = stringResource(R.string.antikill_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                LanguageSwitcherButton(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected
                )
            }
        }

        item {
            FilledTonalButton(
                onClick = onRefreshStatus,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("recheck_permissions_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.btn_recheck))
            }
        }

        // Interactive Permission Checklist
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
                    Text(
                        text = stringResource(R.string.checklist_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    PermissionCheckRow(
                        title = stringResource(R.string.perm_notif_title),
                        subtitle = stringResource(R.string.perm_notif_desc),
                        granted = permissions.hasPostNotifications,
                        buttonLabel = stringResource(R.string.btn_grant),
                        buttonTag = "perm_notif_button",
                        onClick = onRequestPostNotifications
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    PermissionCheckRow(
                        title = stringResource(R.string.perm_battery_title),
                        subtitle = stringResource(R.string.perm_battery_desc),
                        granted = permissions.isIgnoringBatteryOptimizations,
                        buttonLabel = stringResource(R.string.btn_open_settings),
                        buttonTag = "perm_battery_button",
                        onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    PermissionCheckRow(
                        title = stringResource(R.string.perm_usage_title),
                        subtitle = stringResource(R.string.perm_usage_desc),
                        granted = permissions.hasUsageStats,
                        buttonLabel = stringResource(R.string.btn_open_settings),
                        buttonTag = "perm_usage_button",
                        onClick = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            runCatching { context.startActivity(intent) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    PermissionCheckRow(
                        title = stringResource(R.string.perm_write_settings_title),
                        subtitle = stringResource(R.string.perm_write_settings_desc),
                        granted = permissions.hasWriteSettings,
                        buttonLabel = stringResource(R.string.btn_open_settings),
                        buttonTag = "perm_write_settings_button",
                        onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    PermissionCheckRow(
                        title = stringResource(R.string.perm_shizuku_title),
                        subtitle = stringResource(R.string.perm_shizuku_desc),
                        granted = shizukuStatus == ShizukuStatus.READY,
                        buttonLabel = stringResource(
                            if (shizukuStatus == ShizukuStatus.NOT_RUNNING) R.string.btn_open_shizuku
                            else R.string.btn_grant
                        ),
                        buttonTag = "perm_shizuku_button",
                        onClick = {
                            if (shizukuStatus == ShizukuStatus.NOT_RUNNING) {
                                val launch = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                if (launch != null) {
                                    runCatching { context.startActivity(launch) }
                                }
                            } else {
                                onRequestShizuku()
                            }
                        }
                    )
                }
            }
        }

        // Step-by-step HyperOS 3.0.309.0 Anti-Kill Guide
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.guide_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    GuideStepItem(
                        step = stringResource(R.string.guide_step1_title),
                        detail = stringResource(R.string.guide_step1_desc)
                    )
                    GuideStepItem(
                        step = stringResource(R.string.guide_step2_title),
                        detail = stringResource(R.string.guide_step2_desc)
                    )
                    GuideStepItem(
                        step = stringResource(R.string.guide_step3_title),
                        detail = stringResource(R.string.guide_step3_desc)
                    )
                    GuideStepItem(
                        step = stringResource(R.string.guide_step4_title),
                        detail = stringResource(R.string.guide_step4_desc)
                    )
                    GuideStepItem(
                        step = stringResource(R.string.guide_step5_title),
                        detail = stringResource(R.string.guide_step5_desc)
                    )

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("open_app_info_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_open_app_info))
                    }
                }
            }
        }

        // Shizuku Command Console
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Rounded.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.console_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (commandLogs.isNotEmpty()) {
                            TextButton(
                                onClick = onClearCommandLogs,
                                modifier = Modifier.testTag("clear_console_button")
                            ) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.btn_clear_console))
                            }
                        }
                    }

                    if (commandLogs.isEmpty()) {
                        Text(
                            text = stringResource(R.string.console_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(commandLogs, key = { it.id }) { log ->
            val ok = log.exitCode == 0
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(
                    1.dp,
                    if (ok) HyperGreenBright.copy(alpha = 0.35f) else HyperRed.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "EXIT ${log.exitCode}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (ok) HyperGreenBright else HyperRed
                        )
                    }
                    Text(
                        text = "$ ${log.command}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (log.output.isNotBlank()) {
                        Text(
                            text = log.output,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCheckRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    buttonLabel: String,
    buttonTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            contentDescription = null,
            tint = if (granted) HyperGreenBright else HyperAmber
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        if (!granted) {
            FilledTonalButton(
                onClick = onClick,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(buttonTag)
            ) {
                Text(buttonLabel)
            }
        } else {
            Text(
                text = stringResource(R.string.status_granted),
                style = MaterialTheme.typography.labelMedium,
                color = HyperGreenBright,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GuideStepItem(step: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = step,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
