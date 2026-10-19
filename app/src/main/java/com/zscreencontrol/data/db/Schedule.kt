package com.zscreencontrol.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Jadwal berulang: pada jam tertentu, pada hari tertentu (bitmask),
 * lakukan aksi (mis. matikan layar).
 *
 * daysMask bit: bit0=Senin ... bit6=Minggu (lihat [SchedulerConstants]).
 */
@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val daysMask: Int,
    val enabled: Boolean = true,
    val action: String = SchedulerConstants.ACTION_SCREEN_OFF
)
