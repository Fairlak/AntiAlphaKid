package ru.fairlak.antialphakid.features.monitor.data

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

class AppDetector(private val context: Context) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager


    fun getTodayStatsPackages(packages: List<String>): Pair<Map<String, Long>, String?> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = android.app.usage.UsageEvents.Event()

        val statsResult = mutableMapOf<String, Long>()
        val lastResumedTime = mutableMapOf<String, Long>()
        var currentForegroundApp: String? = null

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val pkg = event.packageName

            if (!packages.contains(pkg)) continue

            when (event.eventType) {
                android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {
                    lastResumedTime[pkg] = event.timeStamp
                    currentForegroundApp = pkg
                }
                android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED,
                android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val resumedTime = lastResumedTime[pkg]
                    if (resumedTime != null) {
                        val duration = event.timeStamp - resumedTime
                        statsResult[pkg] = (statsResult[pkg] ?: 0L) + duration
                        lastResumedTime.remove(pkg)
                    }
                    if (currentForegroundApp == pkg) {
                        currentForegroundApp = null
                    }
                }
            }
        }

        val currentTime = System.currentTimeMillis()
        lastResumedTime.forEach { (pkg, resumedTime) ->
            val duration = currentTime - resumedTime
            statsResult[pkg] = (statsResult[pkg] ?: 0L) + duration
        }

        val finalStats = packages.associateWith { statsResult[it] ?: 0L }
        return Pair(finalStats, currentForegroundApp)
    }
}