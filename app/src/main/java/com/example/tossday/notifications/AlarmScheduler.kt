package com.example.tossday.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.tossday.domain.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(task: Task) {
        val date = task.date ?: return
        val time = task.time ?: return

        val triggerAt = LocalDateTime.of(date, time)
            .minusMinutes(10) // Нагадування за 10 хвилин до події
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerAt <= System.currentTimeMillis()) return

        val pendingIntent = buildPendingIntent(task) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager?.canScheduleExactAlarms() == false) {
            // Fallback to inexact alarm if exact not permitted
            alarmManager?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancel(task: Task) {
        buildPendingIntent(task)?.let { alarmManager?.cancel(it) }
    }

    private fun buildPendingIntent(task: Task): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id)
            putExtra(ReminderReceiver.EXTRA_TASK_TEXT, task.text)
        }
        return PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
