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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.LanguageSwitcherButton
import com.example.ui.theme.HyperAmber
import com.example.ui.theme.HyperGreenBright

@Composable
fun AntiKillGuideScreen(
    currentLanguage: AppLanguage,
    permissions: PermissionStatusSnapshot,
    shizukuStatus: ShizukuStatus,
    onLanguageSelected: (AppLanguage) -> Unit,
    onRequestPostNotifications: () -> Unit,
    onRequestShizuku: () -> Unit,
    onRefreshStatus: () -> Unit,
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
