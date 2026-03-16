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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ru.fairlak.antialphakid.core.ui.theme.AntiAlphaKidTheme
import ru.fairlak.antialphakid.features.dashboard.ui.DashboardScreen
import ru.fairlak.antialphakid.features.monitor.service.MonitoringService

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
                DashboardScreen(
                    onManagePermissions = {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this@MainActivity)) {
                            val intentOverlay = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            startActivity(intentOverlay)
                        }
                    }
                )
            }
        }
    }
}