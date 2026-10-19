package com.zscreencontrol.data.db

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

/** Konstanta penjadwalan (bitmask hari & jenis aksi). */
object SchedulerConstants {
    /** bit0=Senin ... bit6=Minggu */
    const val DAY_MON = 1 shl 0
    const val DAY_TUE = 1 shl 1
    const val DAY_WED = 1 shl 2
    const val DAY_THU = 1 shl 3
    const val DAY_FRI = 1 shl 4
    const val DAY_SAT = 1 shl 5
    const val DAY_SUN = 1 shl 6
    const val DAYS_ALL = DAY_MON or DAY_TUE or DAY_WED or DAY_THU or DAY_FRI or DAY_SAT or DAY_SUN

    const val ACTION_SCREEN_OFF = "screen_off"
}

@Entity(tableName = "app_timeouts")
data class AppTimeout(
    /** Nama package aplikasi yang dipantau. */
    @PrimaryKey val packageName: String,
    val appName: String,
    /** Batas pemakaian per sesi foreground, dalam detik. */
    val timeoutSeconds: Int
)

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY hour, minute")
    fun observeAll(): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules WHERE enabled = 1")
    suspend fun getEnabled(): List<Schedule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schedule: Schedule): Long

    @Update
    suspend fun update(schedule: Schedule)

    @Delete
    suspend fun delete(schedule: Schedule)

    @Query("UPDATE schedules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}

@Dao
interface AppTimeoutDao {
    @Query("SELECT * FROM app_timeouts ORDER BY appName")
    fun observeAll(): Flow<List<AppTimeout>>

    @Query("SELECT * FROM app_timeouts")
    suspend fun getAll(): List<AppTimeout>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AppTimeout)

    @Query("DELETE FROM app_timeouts WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("UPDATE app_timeouts SET timeoutSeconds = :seconds")
    suspend fun resetAllTo(seconds: Int)
}

@androidx.room.Database(
    entities = [Schedule::class, AppTimeout::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun appTimeoutDao(): AppTimeoutDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zscreen.db"
                ).build().also { instance = it }
            }
    }
}
