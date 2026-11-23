package com.example.ui.screens.tweaks

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DebloatPackageState
import com.example.hyperos.DebloatRiskTier
import com.example.ui.theme.HyperAmber
import com.example.ui.theme.HyperBlue
import com.example.ui.theme.HyperGreenBright
import com.example.ui.theme.HyperRed
import com.example.ui.theme.JetBrainsMonoFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DebloaterSubTab(
    debloatStates: List<DebloatPackageState>,
    isLoading: Boolean,
    appFrozenCount: Int,
    onRefresh: () -> Unit,
    onFreezeSafePack: () -> Unit,
    onRestoreAllAppFrozen: () -> Unit,
    onTogglePackage: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTier by remember { mutableStateOf<DebloatRiskTier?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var pendingCautionPackage by remember { mutableStateOf<DebloatPackageState?>(null) }

    val filtered = debloatStates.filter { state ->
        val matchesTier = selectedTier == null || state.entry.riskTier == selectedTier
        val q = searchQuery.trim()
        val matchesQuery = q.isBlank() || state.entry.packageName.contains(q, ignoreCase = true)
        matchesTier && matchesQuery
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // China ROM Anti-Bootloop Shield Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, HyperBlue.copy(alpha = 0.45f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = HyperBlue
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.debloat_safety_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.debloat_safety_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onFreezeSafePack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("freeze_safe_pack_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.AcUnit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_freeze_safe_pack))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRestoreAllAppFrozen,
                            enabled = appFrozenCount > 0,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("restore_all_frozen_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_restore_all_frozen, appFrozenCount))
                        }

                        OutlinedButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("refresh_debloat_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_refresh_debloat))
                        }
                    }
                }
            }
        }

        // Filter Chips + Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTier == null,
                        onClick = { selectedTier = null },
                        label = { Text(stringResource(R.string.risk_all)) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("debloat_filter_all")
                    )
                    DebloatRiskTier.entries.forEach { tier ->
                        FilterChip(
                            selected = selectedTier == tier,
                            onClick = { selectedTier = tier },
                            label = { Text(stringResource(tier.labelRes)) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("debloat_filter_${tier.name.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.hint_search_apps)) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debloat_search_input")
                )
            }
        }

        if (isLoading && debloatStates.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (filtered.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.empty_apps_search),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(filtered, key = { it.entry.packageName }) { item ->
                val tierColor = when (item.entry.riskTier) {
                    DebloatRiskTier.SAFE -> HyperGreenBright
                    DebloatRiskTier.MODERATE -> HyperAmber
                    DebloatRiskTier.CAUTION -> HyperRed
                }
                val statusLabel = when {
                    !item.isInstalled -> stringResource(R.string.pkg_status_not_installed)
                    item.isDisabled -> stringResource(R.string.pkg_status_frozen)
                    else -> stringResource(R.string.pkg_status_active)
                }
                val statusColor = when {
                    !item.isInstalled -> MaterialTheme.colorScheme.onSurfaceVariant
                    item.isDisabled -> HyperBlue
                    else -> HyperGreenBright
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = tierColor.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, tierColor.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = stringResource(item.entry.riskTier.labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tierColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = statusColor.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = stringResource(item.entry.titleRes),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = item.entry.packageName,
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = JetBrainsMonoFamily,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(item.entry.descRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Switch(
                            checked = item.isDisabled,
                            enabled = item.isInstalled,
                            onCheckedChange = { wantFreeze ->
                                if (wantFreeze && item.entry.riskTier == DebloatRiskTier.CAUTION) {
                                    pendingCautionPackage = item
                                } else {
                                    onTogglePackage(item.entry.packageName, wantFreeze)
                                }
                            },
                            modifier = Modifier.testTag("debloat_switch_${item.entry.packageName}")
                        )
                    }
                }
            }
        }
    }

    pendingCautionPackage?.let { cautionItem ->
        val title = stringResource(cautionItem.entry.titleRes)
        AlertDialog(
            onDismissRequest = { pendingCautionPackage = null },
            title = { Text(stringResource(R.string.caution_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.caution_dialog_body,
                        title,
                        cautionItem.entry.packageName
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onTogglePackage(cautionItem.entry.packageName, true)
                        pendingCautionPackage = null
                    },
                    modifier = Modifier.testTag("confirm_caution_freeze")
                ) {
                    Text(stringResource(R.string.btn_freeze_anyway))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingCautionPackage = null },
                    modifier = Modifier.testTag("cancel_caution_freeze")
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}
