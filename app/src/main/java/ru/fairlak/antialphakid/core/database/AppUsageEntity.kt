package ru.fairlak.antialphakid.core.database


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppUsageEntity(
    @PrimaryKey val packageName: String,
    val limitMinutes: Int
)