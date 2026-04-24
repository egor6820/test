package com.example.tossday.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tossday.data.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class DailySummaryReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskRepository: TaskRepository

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getIntExtra(DailySummaryScheduler.EXTRA_SUMMARY_TYPE, -1)
        if (type == -1) return

        val pendingResult = goAsync() // Tells Android we are doing async work in BroadcastReceiver

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (type == DailySummaryScheduler.TYPE_MORNING) {
                    val today = LocalDate.now()
                    val tasks = taskRepository.getByDate(today).first().filter { it.status.name == "TODO" }
                    if (tasks.isNotEmpty()) {
                        val taskListStr = tasks.take(3).joinToString("\n") { "• ${it.text}" }
                        val more = if (tasks.size > 3) "\n...та ще ${tasks.size - 3}" else ""
                        NotificationHelper.showSummary(
                            context = context,
                            title = "☀️ План на сьогодні: ${tasks.size} задач",
                            text = "$taskListStr$more",
                            notificationId = type
                        )
                    }
                } else if (type == DailySummaryScheduler.TYPE_EVENING) {
                    val tomorrow = LocalDate.now().plusDays(1)
                    val tasks = taskRepository.getByDate(tomorrow).first().filter { it.status.name == "TODO" }
                    if (tasks.isNotEmpty()) {
                        val taskListStr = tasks.take(3).joinToString("\n") { "• ${it.text}" }
                        val more = if (tasks.size > 3) "\n...та ще ${tasks.size - 3}" else ""
                        NotificationHelper.showSummary(
                            context = context,
                            title = "🌙 Завтра на тебе чекають ${tasks.size} задач",
                            text = "$taskListStr$more",
                            notificationId = type
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
