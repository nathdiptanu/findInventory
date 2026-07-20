package com.docufind.app.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.docufind.app.MainActivity
import com.docufind.app.R
import com.docufind.app.data.local.db.entity.Reminder
import com.docufind.app.domain.model.reminder.ReminderImportance
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        manager.deleteNotificationChannel(LEGACY_CHANNEL_V2_ID)
        manager.deleteNotificationChannel(LEGACY_CHANNEL_V3_ID)
        val soundUri = Uri.parse(
            "android.resource://${context.packageName}/${R.raw.docufind_notify}"
        )
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.reminder_channel_desc)
            setSound(soundUri, audioAttributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 120, 60, 140)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun showReminderNotification(reminder: Reminder) {
        if (!canPostNotifications()) return
        ensureChannel()
        val genericBody = context.getString(R.string.reminder_notification_body)
        val notification = buildNotification(
            notificationId = ReminderTriggerCalculator.stableAlarmRequestCode(reminder.id),
            title = context.getString(R.string.reminder_notification_title),
            body = genericBody,
            importance = ReminderImportance.fromStored(reminder.importance),
            reminderId = reminder.id,
            linkedRecordId = reminder.linkedRecordId,
            linkedPetId = reminder.linkedPetId,
            linkedPetRecordId = reminder.linkedPetRecordId,
            includeActions = true
        )
        notifySafely(ReminderTriggerCalculator.stableAlarmRequestCode(reminder.id), notification)
    }

    fun showTestNotification() {
        if (!canPostNotifications()) return
        ensureChannel()
        val notification = buildNotification(
            notificationId = TEST_NOTIFICATION_ID,
            title = context.getString(R.string.reminder_notification_title),
            body = context.getString(R.string.reminder_test_body),
            importance = ReminderImportance.NORMAL,
            reminderId = null,
            linkedRecordId = null,
            linkedPetId = null,
            linkedPetRecordId = null,
            includeActions = false
        )
        notifySafely(TEST_NOTIFICATION_ID, notification)
    }

    fun cancelNotification(reminderId: String) {
        NotificationManagerCompat.from(context).cancel(
            ReminderTriggerCalculator.stableAlarmRequestCode(reminderId)
        )
    }

    fun canPostNotifications(): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun buildNotification(
        notificationId: Int,
        title: String,
        body: String,
        importance: ReminderImportance,
        reminderId: String?,
        linkedRecordId: String?,
        linkedPetId: String?,
        linkedPetRecordId: String?,
        includeActions: Boolean
    ): android.app.Notification {
        val priority = when (importance) {
            ReminderImportance.URGENT -> NotificationCompat.PRIORITY_MAX
            ReminderImportance.IMPORTANT -> NotificationCompat.PRIORITY_HIGH
            ReminderImportance.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
        }
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            reminderId?.let { putExtra(MainActivity.EXTRA_REMINDER_ID, it) }
            linkedRecordId?.let { putExtra(MainActivity.EXTRA_LINKED_RECORD_ID, it) }
            linkedPetId?.let { putExtra(MainActivity.EXTRA_LINKED_PET_ID, it) }
            linkedPetRecordId?.let { putExtra(MainActivity.EXTRA_LINKED_PET_RECORD_ID, it) }
            putExtra(MainActivity.EXTRA_REQUIRES_UNLOCK, true)
        }
        val pendingTap = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(context.getString(R.string.reminder_notification_body))
            .build()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setColor(ContextCompat.getColor(context, R.color.docufind_notification_accent))
            .setLargeIcon(loadBrandIconBitmap())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingTap)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
        if (includeActions && reminderId != null) {
            builder.addAction(
                R.drawable.ic_notification_small,
                context.getString(R.string.reminder_action_view),
                pendingTap
            )
            builder.addAction(
                R.drawable.ic_notification_small,
                context.getString(R.string.reminder_action_actioned),
                actionPendingIntent(ReminderActionReceiver.ACTION_MARK_ACTIONED, reminderId, notificationId + 1)
            )
            builder.addAction(
                R.drawable.ic_notification_small,
                context.getString(R.string.reminder_action_snooze),
                actionPendingIntent(ReminderActionReceiver.ACTION_SNOOZE, reminderId, notificationId + 2)
            )
        }
        return builder.build()
    }

    private fun actionPendingIntent(action: String, reminderId: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notifySafely(id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS revoked at runtime
        }
    }

    private fun loadBrandIconBitmap(): Bitmap? {
        // Same official mark as Home / launcher / PDF export.
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_docufind_mark_raster) ?: return null
        val size = (context.resources.displayMetrics.density * 48).toInt().coerceAtLeast(1)
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
        }
    }

    companion object {
        /** Bump channel id when sound/identity changes — Android freezes channel sound after first create. */
        const val CHANNEL_ID = "docufind_reminders_v4"
        private const val LEGACY_CHANNEL_ID = "docufind_reminders"
        private const val LEGACY_CHANNEL_V2_ID = "docufind_reminders_v2"
        private const val LEGACY_CHANNEL_V3_ID = "docufind_reminders_v3"
        const val TEST_NOTIFICATION_ID = 900_001
    }
}
