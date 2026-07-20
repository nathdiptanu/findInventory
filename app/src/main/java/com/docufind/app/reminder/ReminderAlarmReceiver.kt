package com.docufind.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.docufind.app.data.local.db.dao.ReminderDao
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderDao: ReminderDao
    @Inject lateinit var reminderEngine: ReminderEngine
    @Inject lateinit var notificationHelper: ReminderNotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = reminderDao.getById(reminderId)
                if (reminder != null && reminder.status == "ACTIVE") {
                    notificationHelper.showReminderNotification(reminder)
                    reminderEngine.onReminderFired(reminderId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
