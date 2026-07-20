package com.docufind.app.reminder

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Notifies UI when notification permission or system settings change. */
@Singleton
class NotificationPermissionTracker @Inject constructor(
    private val notificationHelper: ReminderNotificationHelper
) {
    private val _refreshTick = MutableStateFlow(0)
    val refreshTick: StateFlow<Int> = _refreshTick.asStateFlow()

    fun refresh() {
        _refreshTick.value++
    }

    fun areNotificationsEnabled(): Boolean = notificationHelper.canPostNotifications()
}
