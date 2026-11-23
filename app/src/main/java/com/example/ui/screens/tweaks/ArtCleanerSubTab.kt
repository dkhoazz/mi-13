package com.example.ui.screens.tweaks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ArtCompileMode
import com.example.data.ArtCompileProgress
import com.example.data.InstalledAppItem
import com.example.ui.theme.HyperBlue
import com.example.ui.theme.HyperTeal
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun ArtCleanerSubTab(
    installedApps: List<InstalledAppItem>,
    appsLoading: Boolean,
    artProgress: ArtCompileProgress,
    onTrimCaches: () -> Unit,
    onRunBgDexopt: () -> Unit,
    onCompileAllApps: (ArtCompileMode) -> Unit,
    onCompileSingleApp: (String, ArtCompileMode) -> Unit,
    onCancelCompile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(ArtCompileMode.SPEED_PROFILE) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = installedApps.filter { app ->
        val q = searchQuery.trim()
        q.isBlank() ||
            app.appName.contains(q, ignoreCase = true) ||
            app.packageName.contains(q, ignoreCase = true)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // System Cache Trimmer Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CleaningServices,
                            contentDescription = null,
                            tint = HyperTeal
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.cleaner_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.cleaner_card_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onTrimCaches,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("trim_caches_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.CleaningServices, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_trim_caches))
                    }
                }
            }
        }

        // ART Compilation Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = HyperBlue
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.art_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.art_card_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ArtCompileMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = { Text(stringResource(mode.labelRes)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("art_mode_${mode.name.lowercase()}")
                            )
                        }
                    }

                    Text(
                        text = stringResource(selectedMode.descRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onRunBgDexopt,
                        enabled = !artProgress.isRunning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("run_bg_dexopt_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_run_bg_dexopt))
                    }

                    Button(
                        onClick = { onCompileAllApps(selectedMode) },
                        enabled = !artProgress.isRunning && installedApps.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("compile_all_apps_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.btn_compile_all_apps, installedApps.size))
                    }
                }
            }
        }

        // Running ART Compilation Progress Banner
        if (artProgress.isRunning) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, HyperBlue.copy(alpha = 0.6f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.art_progress_fmt,
                                artProgress.currentPackage ?: "",
                                artProgress.completed,
                                artProgress.total
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = JetBrainsMonoFamily
                        )
                        if (artProgress.total > 1) {
                            LinearProgressIndicator(
                                progress = {
                                    (artProgress.completed.toFloat() / artProgress.total.toFloat())
                                        .coerceIn(0f, 1f)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        OutlinedButton(
                            onClick = onCancelCompile,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("cancel_art_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_cancel))
                        }
                    }
                }
            }
        }

        // Search Bar for User Apps
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.hint_search_apps)) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("art_search_input")
            )
        }

        if (appsLoading && installedApps.isEmpty()) {
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
        } else if (filteredApps.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.empty_apps_search),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(filteredApps, key = { it.packageName }) { appItem ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (appItem.iconBitmap != null) {
                            Image(
                                bitmap = appItem.iconBitmap.asImageBitmap(),
                                contentDescription = appItem.appName,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Apps,
                                contentDescription = appItem.appName,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appItem.appName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = appItem.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = { onCompileSingleApp(appItem.packageName, selectedMode) },
                            enabled = !artProgress.isRunning,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("compile_app_${appItem.packageName}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.btn_compile_one))
                        }
                    }
                }
            }
        }
    }
}
