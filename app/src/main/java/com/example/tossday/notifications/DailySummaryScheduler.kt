package com.example.tossday.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailySummaryScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleDailySummaries() {
        if (alarmManager == null) return

        scheduleSummary(9, 0, TYPE_MORNING)
        scheduleSummary(21, 0, TYPE_EVENING)
    }

    private fun scheduleSummary(hour: Int, minute: Int, type: Int) {
        val intent = Intent(context, DailySummaryReceiver::class.java).apply {
            putExtra(EXTRA_SUMMARY_TYPE, type)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            type, // Use type as requestCode to distinguish morning/evening
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If time has passed for today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // setInexactRepeating is highly optimized for battery life (Android can batch wakeups)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    companion object {
        const val EXTRA_SUMMARY_TYPE = "summary_type"
        const val TYPE_MORNING = 1001
        const val TYPE_EVENING = 1002
    }
}
