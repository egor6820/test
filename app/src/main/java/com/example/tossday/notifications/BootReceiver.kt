package com.example.tossday.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tossday.data.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var dailySummaryScheduler: DailySummaryScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        dailySummaryScheduler.scheduleDailySummaries()

        // Без goAsync() Android вважає receiver завершеним одразу після onReceive і може
        // вбити процес, що щойно стартував для бродкасту, ще до того як alarms перепланують.
        // pendingResult.finish() повертає процесу нормальний стан після завершення корутини.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = taskRepository.getAllFutureTasksWithTime()
                tasks.forEach { task ->
                    alarmScheduler.schedule(task)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
