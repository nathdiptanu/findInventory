package com.docufind.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderEngine: ReminderEngine
    @Inject lateinit var notificationHelper: ReminderNotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_MARK_ACTIONED -> reminderEngine.completeReminderEvent(reminderId)
                    ACTION_SNOOZE -> reminderEngine.snoozeReminder(reminderId)
                    ACTION_DISMISS -> { /* dismiss only — no reschedule */ }
                }
                notificationHelper.cancelNotification(reminderId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_ACTIONED = "com.docufind.app.reminder.ACTION_MARK_ACTIONED"
        const val ACTION_SNOOZE = "com.docufind.app.reminder.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.docufind.app.reminder.ACTION_DISMISS"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
