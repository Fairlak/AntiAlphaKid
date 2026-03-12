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
import ru.fairlak.antialphakid.features.blocker.BlockerManager
import ru.fairlak.antialphakid.features.monitor.data.AppDetector

class MonitoringService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var detector: AppDetector
    private lateinit var blockerManager: BlockerManager
    private val CHANNEL_ID = "monitoring_channel"


    override fun onCreate() {
        super.onCreate()
        detector = AppDetector(this)
        blockerManager = BlockerManager(this)

        createNotificationChannel()
        val notification = createNotification()
        startForeground(1, notification)

        startMonitoring()
    }

    private fun startMonitoring() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        val trackedPackages = listOf(
            "com.zhiliaoapp.musically",
            "com.google.android.youtube"
        )

        serviceScope.launch {
            while (isActive) {
                try {
                    if (powerManager.isInteractive) {
                        val statsMap = detector.getTodayStatsForPackages(trackedPackages)
                        Log.d("AntiAlphaDebug", "Статистика: $statsMap")
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
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
