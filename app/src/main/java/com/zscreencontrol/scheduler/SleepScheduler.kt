package com.zscreencontrol.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zscreencontrol.data.db.AppDatabase
import com.zscreencontrol.data.db.Schedule
import com.zscreencontrol.data.db.SchedulerConstants
import java.util.Calendar

/**
 * Penjadwal utama: hitung eksekusi berikutnya dari daftar jadwal aktif
 * (Room) dan pasang satu alarm paling dekat.
 */
object SleepScheduler {

    const val ACTION_FIRE = "com.zscreencontrol.action.SCHEDULE_FIRE"

    /** Hari kalender (Calendar.DAY_OF_WEEK) -> bit SchedulerConstants. */
    private fun calendarDayToMask(dayOfWeek: Int): Int = when (dayOfWeek) {
        Calendar.MONDAY -> SchedulerConstants.DAY_MON
        Calendar.TUESDAY -> SchedulerConstants.DAY_TUE
        Calendar.WEDNESDAY -> SchedulerConstants.DAY_WED
        Calendar.THURSDAY -> SchedulerConstants.DAY_THU
        Calendar.FRIDAY -> SchedulerConstants.DAY_FRI
        Calendar.SATURDAY -> SchedulerConstants.DAY_SAT
        Calendar.SUNDAY -> SchedulerConstants.DAY_SUN
        else -> 0
    }

    /** Mask bit Sen..Min -> label "Sen, Rab, Jum" (untuk UI). */
    fun daysMaskToLabel(mask: Int): String? {
        val names = listOf(
            SchedulerConstants.DAY_MON to "Sen",
            SchedulerConstants.DAY_TUE to "Sel",
            SchedulerConstants.DAY_WED to "Rab",
            SchedulerConstants.DAY_THU to "Kam",
            SchedulerConstants.DAY_FRI to "Jum",
            SchedulerConstants.DAY_SAT to "Sab",
            SchedulerConstants.DAY_SUN to "Min"
        )
        val selected = names.filter { (mask and it.first) != 0 }.map { it.second }
        return when {
            selected.size == 7 -> "Setiap hari"
            selected.isEmpty() -> null
            else -> selected.joinToString(", ")
        }
    }

    /**
     * Waktu eksekusi berikutnya untuk satu jadwal (epoch ms), atau null.
     * Dicari maksimal 8 hari ke depan (mencakup minggu penuh).
     */
    fun nextTrigger(schedule: Schedule, now: Calendar = Calendar.getInstance()): Long? {
        if (!schedule.enabled || schedule.daysMask == 0) return null
        for (dayOffset in 0..7) {
            val candidate = now.clone() as Calendar
            candidate.add(Calendar.DAY_OF_YEAR, dayOffset)
            candidate.set(Calendar.HOUR_OF_DAY, schedule.hour)
            candidate.set(Calendar.MINUTE, schedule.minute)
            candidate.set(Calendar.SECOND, 0)
            candidate.set(Calendar.MILLISECOND, 0)
            if (candidate.timeInMillis <= now.timeInMillis) continue
            val dayMask = calendarDayToMask(candidate.get(Calendar.DAY_OF_WEEK))
            if ((schedule.daysMask and dayMask) != 0) {
                return candidate.timeInMillis
            }
        }
        return null
    }

    /** Ganti alarm menjadi eksekusi paling dekat di antara semua jadwal aktif. */
    fun scheduleNext(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context)
        am.cancel(pi)

        val next = kotlinx.coroutines.runBlocking {
            val schedules = AppDatabase.get(context).scheduleDao().getEnabled()
            val now = Calendar.getInstance()
            schedules.mapNotNull { nextTrigger(it, now) }.minOrNull()
        } ?: return

        try {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
            } else {
                am.set(AlarmManager.RTC_WAKEUP, next, pi)
            }
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, next, pi)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).setAction(ACTION_FIRE)
        return PendingIntent.getBroadcast(
            context,
            REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val REQUEST = 31
}
