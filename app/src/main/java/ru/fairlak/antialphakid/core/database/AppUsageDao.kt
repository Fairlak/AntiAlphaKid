package ru.fairlak.antialphakid.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {
    @Query("SELECT * FROM app_limits")
    fun getAllLimits(): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_limits WHERE packageName = :pkg LIMIT 1")
    suspend fun getLimitForApp(pkg: String): AppUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLimit(limit: AppUsageEntity)

    @Delete
    suspend fun deleteLimit(limit: AppUsageEntity)
}