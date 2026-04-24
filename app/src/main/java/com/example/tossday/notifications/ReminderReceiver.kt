package com.example.tossday.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val taskText = intent.getStringExtra(EXTRA_TASK_TEXT) ?: return
        NotificationHelper.show(context, taskId, "Скоро: $taskText")
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TEXT = "task_text"
    }
}
