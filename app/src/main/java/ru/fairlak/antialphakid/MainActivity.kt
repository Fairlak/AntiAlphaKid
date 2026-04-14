package ru.fairlak.antialphakid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.fairlak.antialphakid.core.ui.theme.AntiAlphaKidTheme
import ru.fairlak.antialphakid.features.dashboard.ui.DashboardScreen
import ru.fairlak.antialphakid.features.dashboard.ui.hasUsageStatsPermission
import ru.fairlak.antialphakid.features.effects.crtEffect
import ru.fairlak.antialphakid.features.monitor.service.MonitoringService
import ru.fairlak.antialphakid.features.settings.ui.SettingsScreen
import ru.fairlak.antialphakid.features.settings.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val intent = Intent(this, MonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        enableEdgeToEdge()
        setContent {
            AntiAlphaKidTheme {
                val navController = rememberNavController()
                val globalSettingsViewModel: SettingsViewModel = viewModel()
                val activeColor by globalSettingsViewModel.activeColor.collectAsState()
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .crtEffect(activeColor),
                    color = Color.Black
                ) {
                    Box {
                        NavHost(
                            navController = navController,
                            startDestination = "dashboard",
                            enterTransition = { androidx.compose.animation.EnterTransition.None },
                            exitTransition = { androidx.compose.animation.ExitTransition.None }
                        ) {
                            composable("dashboard") {
                                DashboardScreen(
                                    settingsViewModel = globalSettingsViewModel,
                                    onManagePermissions = {
                                        val hasUsageStats =
                                            hasUsageStatsPermission(this@MainActivity)
                                        val hasOverlay =
                                            Settings.canDrawOverlays(this@MainActivity)

                                        if (!hasUsageStats) {
                                            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                        } else if (!hasOverlay) {
                                            val intentOverlay =
                                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                            startActivity(intentOverlay)
                                        }
                                    },
                                    onOpenSettings = {
                                        navController.navigate("settings") {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    viewModel = globalSettingsViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}