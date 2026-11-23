package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.service.BatteryMonitorService
import com.example.ui.MainViewModel
import com.example.ui.screens.AntiKillGuideScreen
import com.example.ui.screens.HyperOSTweaksScreen
import com.example.ui.screens.OverviewBatteryScreen
import com.example.ui.screens.ProfilesScreen
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.HyperBatteryTheme
import com.example.util.LocaleHelper

private enum class AppTab(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    OVERVIEW("overview", R.string.tab_overview, Icons.Rounded.BatteryChargingFull),
    PROFILES("profiles", R.string.tab_profiles, Icons.Rounded.Speed),
    TWEAKS("tweaks", R.string.tab_tweaks, Icons.Rounded.Tune),
    ANTIKILL("antikill", R.string.tab_antikill, Icons.Rounded.Shield)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        startMonitorServiceSafely()

        setContent {
            HyperBatteryTheme {
                HyperOptimizerApp(onStartService = { startMonitorServiceSafely() })
            }
        }
    }

    private fun startMonitorServiceSafely() {
        val intent = Intent(this, BatteryMonitorService::class.java).apply {
            action = BatteryMonitorService.ACTION_START
        }
        try {
            ContextCompat.startForegroundService(this, intent)
        } catch (_: Throwable) {
        }
    }
}

@Composable
private fun HyperOptimizerApp(
    onStartService: () -> Unit,
    vm: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val baseContext = LocalContext.current

    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val telemetry by vm.batteryTelemetry.collectAsStateWithLifecycle()
    val automationStatus by vm.automationStatus.collectAsStateWithLifecycle()
    val shizukuStatus by vm.shizukuStatus.collectAsStateWithLifecycle()
    val commandLogs by vm.commandLogs.collectAsStateWithLifecycle()
    val permissions by vm.permissions.collectAsStateWithLifecycle()
    val installedApps by vm.installedApps.collectAsStateWithLifecycle()
    val appsLoading by vm.appsLoading.collectAsStateWithLifecycle()
    val debloatStates by vm.debloatStates.collectAsStateWithLifecycle()
    val debloatLoading by vm.debloatLoading.collectAsStateWithLifecycle()
    val artProgress by vm.artProgress.collectAsStateWithLifecycle()

    val localizedContext = remember(baseContext, prefs.appLanguage) {
        LocaleHelper.localizedContext(baseContext, prefs.appLanguage)
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        vm.refreshPermissionsAndShizuku()
        onStartService()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                baseContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        vm.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refreshPermissionsAndShizuku()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = AmoledBackground,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(containerColor = AmoledSurface) {
                            AppTab.entries.forEach { tab ->
                                val label = stringResource(tab.labelRes)
                                NavigationBarItem(
                                    selected = currentRoute == tab.route,
                                    onClick = { navigateToTab(navController, tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = label) },
                                    label = {
                                        Text(
                                            text = label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_tab_${tab.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = AmoledSurface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            AppTab.entries.forEach { tab ->
                                val label = stringResource(tab.labelRes)
                                NavigationRailItem(
                                    selected = currentRoute == tab.route,
                                    onClick = { navigateToTab(navController, tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = label) },
                                    label = { Text(label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_rail_${tab.route}")
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = AppTab.OVERVIEW.route,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 680.dp)
                        ) {
                            composable(AppTab.OVERVIEW.route) {
                                OverviewBatteryScreen(
                                    telemetry = telemetry,
                                    prefs = prefs,
                                    automationStatus = automationStatus,
                                    onLanguageSelected = vm::setAppLanguage,
                                    onUpperTargetChange = vm::setUpperChargeTarget,
                                    onLowBatteryAlertChange = vm::setLowBatteryAlert,
                                    onTestAlarm = vm::testAlarm,
                                    onDismissAlarm = vm::dismissAlarm,
                                    onSnoozeAlarm = vm::snoozeAlarm
                                )
                            }

                            composable(AppTab.PROFILES.route) {
                                ProfilesScreen(
                                    prefs = prefs,
                                    automationStatus = automationStatus,
                                    installedApps = installedApps,
                                    appsLoading = appsLoading,
                                    onLanguageSelected = vm::setAppLanguage,
                                    onApplyProfileNow = vm::applyProfileNow,
                                    onSaveProfileConfig = vm::saveProfileConfig,
                                    onPerAppToggle = vm::setPerAppTrigger,
                                    onOpenAppSelector = vm::loadInstalledAppsIfNeeded,
                                    onGamingPackagesSave = vm::setGamingPackages,
                                    onChargeCoolToggle = vm::setChargeCoolTrigger,
                                    onLowBatteryRuleChange = vm::setLowBatteryProfileTrigger,
                                    onQuietHoursChange = vm::setQuietHours
                                )
                            }

                            composable(AppTab.TWEAKS.route) {
                                HyperOSTweaksScreen(
                                    prefs = prefs,
                                    shizukuStatus = shizukuStatus,
                                    permissions = permissions,
                                    currentTempC = telemetry.temperatureC,
                                    debloatStates = debloatStates,
                                    debloatLoading = debloatLoading,
                                    installedApps = installedApps,
                                    appsLoading = appsLoading,
                                    artProgress = artProgress,
                                    onLanguageSelected = vm::setAppLanguage,
                                    onRequestShizuku = vm::requestShizukuPermission,
                                    onGrantPersistentPermissions = vm::grantSelfPermissionsViaShizuku,
                                    onRefreshRateChange = vm::setRefreshRate,
                                    onSpeedModeChange = vm::setSpeedMode,
                                    onMiuiHomeAnimRateChange = vm::setMiuiHomeAnimRate,
                                    onAnimationScaleChange = vm::setAnimationScale,
                                    onGameDriverChange = vm::setGameDriverEnabled,
                                    onPhantomGuardChange = vm::setPhantomGuard,
                                    onThermalOverrideChange = vm::setThermalOverride,
                                    onRestoreDefaults = vm::restoreStockDefaults,
                                    onRefreshDebloat = vm::refreshDebloatStates,
                                    onFreezeSafePack = vm::freezeSafePack,
                                    onRestoreAllAppFrozen = vm::restoreAllAppFrozen,
                                    onToggleDebloatPackage = vm::toggleDebloatPackage,
                                    onLoadInstalledApps = vm::loadInstalledAppsIfNeeded,
                                    onTrimCaches = vm::trimSystemCaches,
                                    onRunBgDexopt = vm::runBgDexoptJob,
                                    onCompileAllApps = vm::compileAllUserApps,
                                    onCompileSingleApp = vm::compileSingleApp,
                                    onCancelArtCompile = vm::cancelArtCompilation,
                                    onCachedFreezerChange = vm::setCachedAppFreezer,
                                    onPrivateDnsChange = vm::setPrivateDns,
                                    onWifiScanDisabledChange = vm::setWifiScanDisabled,
                                    onBleScanDisabledChange = vm::setBleScanDisabled,
                                    onToggleBgRestrict = vm::toggleAppBackgroundRestriction
                                )
                            }

                            composable(AppTab.ANTIKILL.route) {
                                AntiKillGuideScreen(
                                    currentLanguage = prefs.appLanguage,
                                    permissions = permissions,
                                    shizukuStatus = shizukuStatus,
                                    commandLogs = commandLogs,
                                    onLanguageSelected = vm::setAppLanguage,
                                    onRequestPostNotifications = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    onRequestShizuku = vm::requestShizukuPermission,
                                    onRefreshStatus = vm::refreshPermissionsAndShizuku,
                                    onClearCommandLogs = vm::clearCommandLogs
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
