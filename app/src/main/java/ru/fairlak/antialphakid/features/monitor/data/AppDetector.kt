package ru.fairlak.antialphakid.features.monitor.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class AppDetector(private val context: Context) {

    fun getForegroundApp(): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 30

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var lastPackageName: String? = null

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastPackageName = event.packageName
            }
        }

        return lastPackageName
    }
}