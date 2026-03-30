package ru.fairlak.antialphakid.features.monitor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import ru.fairlak.antialphakid.core.database.AppDatabase
import ru.fairlak.antialphakid.core.database.AppUsageEntity
import ru.fairlak.antialphakid.features.blocker.BlockerManager
import ru.fairlak.antialphakid.features.monitor.data.AppDetector

class MonitoringService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var detector: AppDetector
    private lateinit var blockerManager: BlockerManager
    private val CHANNEL_ID = "monitoring_channel"
    private lateinit var db: AppDatabase


    override fun onCreate() {
        super.onCreate()
        detector = AppDetector(this)
        blockerManager = BlockerManager(this)
        db = AppDatabase.getDatabase(this)


        createNotificationChannel()
        val notification = createNotification()
        startForeground(1, notification)

        startMonitoring()
    }

    private fun startMonitoring() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager


        serviceScope.launch {
            val prefs = getSharedPreferences("anti_alpha_prefs", MODE_PRIVATE)
            while (isActive) {
                try {
                    val isSystemActive = prefs.getBoolean("system_active", true)

                    if (!isSystemActive) {
                        withContext(Dispatchers.Main) {
                            blockerManager.hideOverlay()
                        }
                        delay(5000)
                        continue
                    }


                    if (powerManager.isInteractive) {
                        val allLimits = db.appUsageDao().getAllLimits().first()
                        val trackedPackages = allLimits.map { it.packageName }

                        if (trackedPackages.isNotEmpty()) {
                            val (statsMap, currentApp) = detector.getTodayStatsPackages(trackedPackages)

                            if (currentApp != null) {
                                val appTimeMs = statsMap[currentApp] ?: 0L

                                val appMinutes = appTimeMs / 1000 / 60
                                val limit = allLimits.find { it.packageName == currentApp }?.limitMinutes ?: 30
                                Log.d("AntiAlphaDebug", "Сейчас открыто: $currentApp | Время: $appMinutes мин. | Лимит: $limit")


                                if (appMinutes >= limit) {
                                    withContext(Dispatchers.Main) {
                                        blockerManager.showOverlay()
                                    }
                                }


                                val notification = NotificationCompat.Builder(this@MonitoringService, CHANNEL_ID)
                                    .setContentTitle("Лимит: $limit мин.")
                                    .setContentText("Использовано $currentApp: $appMinutes мин.")
                                    .setSmallIcon(androidx.core.R.drawable.notification_bg)
                                    .setSilent(true)
                                    .build()
                                notificationManager.notify(1, notification)

                            }
                        }

                    }
                } catch (e: Exception) {
                    Log.e("AntiAlphaDebug", "Ошибка: ${e.message}")
                }
                delay(5000)
            }
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Анти-скролл работает")
            .setContentText("Мониторинг запущен")
            .setSmallIcon(androidx.core.R.drawable.notification_bg)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Мониторинг приложений",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
