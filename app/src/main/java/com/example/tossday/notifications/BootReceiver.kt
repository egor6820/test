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
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            dailySummaryScheduler.scheduleDailySummaries()
            CoroutineScope(Dispatchers.IO).launch {
                val tasks = taskRepository.getAllFutureTasksWithTime()
                tasks.forEach { task ->
                    alarmScheduler.schedule(task)
                }
            }
        }
    }
}
