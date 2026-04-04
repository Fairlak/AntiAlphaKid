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
    private val CHANNEL_SILENT = "monitoring_silent"
    private val CHANNEL_WARNING = "monitoring_warning"
    private lateinit var db: AppDatabase
    private var lastNotifiedPackage: String? = null
    private var hasShownWarning = false


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
                    val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
                    if (!isSystemActive) {
                        withContext(Dispatchers.Main) {
                            blockerManager.hideOverlay()
                        }
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()

                        break
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
                                val timeLeft = limit - appMinutes

                                if (currentApp != lastNotifiedPackage) {
                                    lastNotifiedPackage = currentApp
                                    hasShownWarning = false
                                }

                                val packageManager = this@MonitoringService.packageManager
                                val truAppName = try {
                                    val appInfo = packageManager.getApplicationInfo(currentApp, 0)
                                    packageManager.getApplicationLabel(appInfo).toString()
                                } catch (_: Exception) {
                                    currentApp
                                }

                                Log.d("AntiAlphaDebug", "Сейчас открыто: $truAppName | Время: $appMinutes мин. | Лимит: $limit")


                                if (timeLeft in 1..5 && !hasShownWarning && notificationsEnabled) {
                                    showWarningNotification(truAppName, timeLeft.toInt())
                                    hasShownWarning = true
                                }

                                if (appMinutes >= limit) {
                                    withContext(Dispatchers.Main) {
                                        blockerManager.showOverlay()
                                    }
                                }

                                val notification = NotificationCompat.Builder(this@MonitoringService, CHANNEL_SILENT)
                                    .setContentTitle("Лимит: $limit мин.")
                                    .setContentText("Использовано $truAppName: $appMinutes мин.")
                                    .setSmallIcon(androidx.core.R.drawable.notification_bg)
                                    .setSilent(true)
                                    .build()
                                notificationManager.notify(1, notification)
                            } else {
                                lastNotifiedPackage = null
                                hasShownWarning = false
                                notificationManager.notify(1, createNotification())
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
        return NotificationCompat.Builder(this, CHANNEL_SILENT)
            .setContentTitle("Анти-скролл работает")
            .setContentText("Система активна")
            .setSmallIcon(androidx.core.R.drawable.notification_bg)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val silentChannel = NotificationChannel(
                CHANNEL_SILENT,
                "Работа системы",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фоновая работа мониторинга"
                setShowBadge(false)
            }

            val warningChannel = NotificationChannel(
                CHANNEL_WARNING,
                "Предупреждения о лимитах",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления за n минут до блокировки"
                enableVibration(true)
                enableLights(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(silentChannel)
            manager.createNotificationChannel(warningChannel)
        }
    }

    private fun showWarningNotification(appName: String, minutesLeft: Int) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val warningNotification = NotificationCompat.Builder(this, CHANNEL_WARNING)
            .setSmallIcon(androidx.core.R.drawable.notification_bg)
            .setContentTitle("Внимание: $appName")
            .setContentText("Осталось всего $minutesLeft мин. до блокировки!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, warningNotification)
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
