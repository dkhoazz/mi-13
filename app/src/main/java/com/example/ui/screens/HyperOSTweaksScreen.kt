package com.example.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AppLanguage
import com.example.data.AppPreferencesState
import com.example.data.ArtCompileMode
import com.example.data.ArtCompileProgress
import com.example.data.DebloatPackageState
import com.example.data.HyperAnimRate
import com.example.data.InstalledAppItem
import com.example.data.PermissionStatusSnapshot
import com.example.data.PrivateDnsPreset
import com.example.data.RefreshRateMode
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.LanguageSwitcherButton
import com.example.ui.screens.tweaks.ArtCleanerSubTab
import com.example.ui.screens.tweaks.DebloaterSubTab
import com.example.ui.screens.tweaks.RamNetworkSubTab
import com.example.ui.screens.tweaks.SystemTweaksSubTab
import com.example.ui.theme.AmoledBackground

private enum class DeepTuneSubTab(@StringRes val labelRes: Int) {
    SYSTEM(R.string.subtab_system),
    DEBLOATER(R.string.subtab_debloater),
    ART_CACHE(R.string.subtab_art_cache),
    RAM_NET(R.string.subtab_ram_net)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HyperOSTweaksScreen(
    prefs: AppPreferencesState,
    shizukuStatus: ShizukuStatus,
    permissions: PermissionStatusSnapshot,
    currentTempC: Float,
    debloatStates: List<DebloatPackageState>,
    debloatLoading: Boolean,
    installedApps: List<InstalledAppItem>,
    appsLoading: Boolean,
    artProgress: ArtCompileProgress,
    onLanguageSelected: (AppLanguage) -> Unit,
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
    onRefreshDebloat: (Boolean) -> Unit,
    onFreezeSafePack: () -> Unit,
    onRestoreAllAppFrozen: () -> Unit,
    onToggleDebloatPackage: (String, Boolean) -> Unit,
    onLoadInstalledApps: () -> Unit,
    onTrimCaches: () -> Unit,
    onRunBgDexopt: () -> Unit,
    onCompileAllApps: (ArtCompileMode) -> Unit,
    onCompileSingleApp: (String, ArtCompileMode) -> Unit,
    onCancelArtCompile: () -> Unit,
    onCachedFreezerChange: (Boolean) -> Unit,
    onPrivateDnsChange: (PrivateDnsPreset) -> Unit,
    onWifiScanDisabledChange: (Boolean) -> Unit,
    onBleScanDisabledChange: (Boolean) -> Unit,
    onToggleBgRestrict: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSubTab by rememberSaveable { mutableStateOf(DeepTuneSubTab.SYSTEM) }

    LaunchedEffect(activeSubTab) {
        when (activeSubTab) {
            DeepTuneSubTab.DEBLOATER -> onRefreshDebloat(false)
            DeepTuneSubTab.ART_CACHE, DeepTuneSubTab.RAM_NET -> onLoadInstalledApps()
            DeepTuneSubTab.SYSTEM -> Unit
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tweaks_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.tweaks_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            LanguageSwitcherButton(
                currentLanguage = prefs.appLanguage,
                onLanguageSelected = onLanguageSelected
            )
        }

        PrimaryScrollableTabRow(
            selectedTabIndex = activeSubTab.ordinal,
            containerColor = AmoledBackground,
            edgePadding = 16.dp
        ) {
            DeepTuneSubTab.entries.forEach { tab ->
                Tab(
                    selected = activeSubTab == tab,
                    onClick = { activeSubTab = tab },
                    text = { Text(stringResource(tab.labelRes)) },
                    modifier = Modifier.testTag("tweaks_subtab_${tab.name.lowercase()}")
                )
            }
        }

        when (activeSubTab) {
            DeepTuneSubTab.SYSTEM -> SystemTweaksSubTab(
                prefs = prefs,
                shizukuStatus = shizukuStatus,
                permissions = permissions,
                currentTempC = currentTempC,
                onRequestShizuku = onRequestShizuku,
                onGrantPersistentPermissions = onGrantPersistentPermissions,
                onRefreshRateChange = onRefreshRateChange,
                onSpeedModeChange = onSpeedModeChange,
                onMiuiHomeAnimRateChange = onMiuiHomeAnimRateChange,
                onAnimationScaleChange = onAnimationScaleChange,
                onGameDriverChange = onGameDriverChange,
                onPhantomGuardChange = onPhantomGuardChange,
                onThermalOverrideChange = onThermalOverrideChange,
                onRestoreDefaults = onRestoreDefaults,
                modifier = Modifier.weight(1f)
            )

            DeepTuneSubTab.DEBLOATER -> DebloaterSubTab(
                debloatStates = debloatStates,
                isLoading = debloatLoading,
                appFrozenCount = prefs.frozenByAppPackages.size,
                onRefresh = { onRefreshDebloat(true) },
                onFreezeSafePack = onFreezeSafePack,
                onRestoreAllAppFrozen = onRestoreAllAppFrozen,
                onTogglePackage = onToggleDebloatPackage,
                modifier = Modifier.weight(1f)
            )

            DeepTuneSubTab.ART_CACHE -> ArtCleanerSubTab(
                installedApps = installedApps,
                appsLoading = appsLoading,
                artProgress = artProgress,
                onTrimCaches = onTrimCaches,
                onRunBgDexopt = onRunBgDexopt,
                onCompileAllApps = onCompileAllApps,
                onCompileSingleApp = onCompileSingleApp,
                onCancelCompile = onCancelArtCompile,
                modifier = Modifier.weight(1f)
            )

            DeepTuneSubTab.RAM_NET -> RamNetworkSubTab(
                prefs = prefs,
                installedApps = installedApps,
                appsLoading = appsLoading,
                onCachedFreezerChange = onCachedFreezerChange,
                onPrivateDnsChange = onPrivateDnsChange,
                onWifiScanDisabledChange = onWifiScanDisabledChange,
                onBleScanDisabledChange = onBleScanDisabledChange,
                onToggleBgRestrict = onToggleBgRestrict,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
