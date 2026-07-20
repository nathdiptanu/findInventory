package com.docufind.app.insights

import com.docufind.app.data.local.db.dao.ActivityEventDao
import com.docufind.app.data.local.db.entity.ActivityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityInsightsTracker @Inject constructor(
    private val activityEventDao: ActivityEventDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sessionStartedAt: Long? = null
    private var lastScreen: String? = null

    fun onAppForegrounded() {
        val now = System.currentTimeMillis()
        sessionStartedAt = now
        record(ActivityEventType.APP_OPENED, timestamp = now)
        record(ActivityEventType.SESSION_STARTED, timestamp = now)
    }

    fun onAppBackgrounded() {
        val startedAt = sessionStartedAt ?: return
        val now = System.currentTimeMillis()
        sessionStartedAt = null
        record(
            type = ActivityEventType.SESSION_ENDED,
            timestamp = now,
            durationMs = (now - startedAt).coerceAtLeast(0L)
        )
    }

    fun trackScreenView(route: String?) {
        val normalized = route?.substringBefore("/{")?.takeIf { it.isNotBlank() } ?: return
        if (lastScreen == normalized) return
        lastScreen = normalized
        record(ActivityEventType.SCREEN_VIEW, screen = normalized)
    }

    fun trackDocumentAdded(category: String) {
        record(ActivityEventType.DOCUMENT_ADDED, category = category)
    }

    fun trackReminderCreated(category: String) {
        record(ActivityEventType.REMINDER_CREATED, category = category)
    }

    fun trackReminderCompleted(category: String?) {
        record(ActivityEventType.REMINDER_COMPLETED, category = category)
    }

    fun trackVaultOpened() {
        record(ActivityEventType.VAULT_OPENED)
    }

    fun trackSearchUsed() {
        record(ActivityEventType.SEARCH_USED)
    }

    private fun record(
        type: ActivityEventType,
        timestamp: Long = System.currentTimeMillis(),
        durationMs: Long? = null,
        screen: String? = null,
        category: String? = null
    ) {
        scope.launch {
            runCatching {
                activityEventDao.insert(
                    ActivityEvent(
                        id = UUID.randomUUID().toString(),
                        type = type.name,
                        timestamp = timestamp,
                        durationMs = durationMs,
                        screen = screen,
                        category = category
                    )
                )
            }
        }
    }
}
