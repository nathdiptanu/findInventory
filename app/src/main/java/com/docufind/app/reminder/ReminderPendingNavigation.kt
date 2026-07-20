package com.docufind.app.reminder

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PendingReminderNavigation(
    val reminderId: String,
    val linkedRecordId: String?,
    val linkedPetId: String?,
    val linkedPetRecordId: String?,
    val requiresUnlock: Boolean
)

@Singleton
class ReminderPendingNavigation @Inject constructor() {
    private val _pending = MutableStateFlow<PendingReminderNavigation?>(null)
    val pending: StateFlow<PendingReminderNavigation?> = _pending.asStateFlow()

    fun setPending(nav: PendingReminderNavigation) {
        _pending.value = nav
    }

    fun consume(): PendingReminderNavigation? {
        val value = _pending.value
        _pending.value = null
        return value
    }
}
