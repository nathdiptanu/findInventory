package com.docufind.app.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.domain.model.reminder.CustomReminderRequest
import com.docufind.app.domain.model.reminder.ReminderListItem
import com.docufind.app.domain.model.reminder.ReminderStatus
import com.docufind.app.domain.model.reminder.ReminderTab
import com.docufind.app.domain.repository.PreferencesRepository
import com.docufind.app.domain.repository.ReminderRepository
import com.docufind.app.reminder.NotificationPermissionTracker
import com.docufind.app.reminder.ReminderAlarmScheduler
import com.docufind.app.reminder.ReminderNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RemindersUiState(
    val reminders: List<ReminderListItem> = emptyList(),
    val selectedTab: ReminderTab = ReminderTab.UPCOMING,
    val showAddDialog: Boolean = false,
    val editingReminder: ReminderListItem? = null,
    val notificationsEnabled: Boolean = true,
    val exactAlarmsAvailable: Boolean = true,
    val defaultReminderTimeMinutes: Int = 9 * 60,
    val showDefaultTimeDialog: Boolean = false,
    val showPermissionRationale: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
) {
    private val now: Long get() = System.currentTimeMillis()

    val filteredReminders: List<ReminderListItem>
        get() = when (selectedTab) {
            ReminderTab.UPCOMING -> reminders.filter {
                it.status == ReminderStatus.ACTIVE && it.triggerAt >= now
            }.sortedBy { it.triggerAt }
            ReminderTab.OVERDUE -> reminders.filter {
                it.status == ReminderStatus.ACTIVE && it.triggerAt < now
            }.sortedBy { it.triggerAt }
            ReminderTab.COMPLETED -> reminders.filter {
                it.status == ReminderStatus.COMPLETED
            }.sortedByDescending { it.triggerAt }
        }
}

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val repository: ReminderRepository,
    private val preferencesRepository: PreferencesRepository,
    private val notificationHelper: ReminderNotificationHelper,
    private val notificationPermissionTracker: NotificationPermissionTracker,
    private val alarmScheduler: ReminderAlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemindersUiState())

    val uiState: StateFlow<RemindersUiState> = combine(
        repository.observeAll(),
        _uiState,
        notificationPermissionTracker.refreshTick,
        preferencesRepository.preferences
    ) { reminders, state, _, preferences ->
        state.copy(
            reminders = reminders,
            defaultReminderTimeMinutes = preferences.defaultReminderTimeMinutes,
            notificationsEnabled = notificationHelper.canPostNotifications(),
            exactAlarmsAvailable = alarmScheduler.canScheduleExactAlarms()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersUiState())

    init {
        notificationHelper.ensureChannel()
    }

    fun refreshNotificationStatus() {
        notificationPermissionTracker.refresh()
        _uiState.update { it.copy(exactAlarmsAvailable = alarmScheduler.canScheduleExactAlarms()) }
    }

    fun onTabSelected(tab: ReminderTab) = _uiState.update { it.copy(selectedTab = tab) }

    fun showAddDialog() = _uiState.update { it.copy(showAddDialog = true, editingReminder = null) }

    fun showEditDialog(reminder: ReminderListItem) =
        _uiState.update { it.copy(editingReminder = reminder, showAddDialog = false) }

    fun dismissAddDialog() = _uiState.update { it.copy(showAddDialog = false) }

    fun dismissEditDialog() = _uiState.update { it.copy(editingReminder = null) }

    fun showPermissionRationale() = _uiState.update { it.copy(showPermissionRationale = true) }

    fun dismissPermissionRationale() = _uiState.update { it.copy(showPermissionRationale = false) }

    fun showDefaultTimeDialog() = _uiState.update { it.copy(showDefaultTimeDialog = true) }

    fun dismissDefaultTimeDialog() = _uiState.update { it.copy(showDefaultTimeDialog = false) }

    fun updateDefaultReminderTime(minutes: Int) {
        viewModelScope.launch {
            runCatching {
                preferencesRepository.setDefaultReminderTimeMinutes(minutes)
                repository.updateDefaultTimeForLinkedReminders(minutes).getOrThrow()
            }.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(showDefaultTimeDialog = false, infoMessage = "default_time_saved")
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            showDefaultTimeDialog = false,
                            errorMessage = e.message ?: "Could not update reminder time"
                        )
                    }
                }
            )
        }
    }

    fun onPermissionResult(granted: Boolean) {
        notificationPermissionTracker.refresh()
        _uiState.update {
            it.copy(
                notificationsEnabled = notificationHelper.canPostNotifications(),
                showPermissionRationale = !granted
            )
        }
    }

    fun sendTestNotification() {
        if (!notificationHelper.canPostNotifications()) {
            _uiState.update { it.copy(infoMessage = "permission_disabled") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(infoMessage = "test_scheduled") }
            delay(5_000)
            notificationHelper.showTestNotification()
        }
    }

    fun saveCustomReminder(request: CustomReminderRequest) {
        viewModelScope.launch {
            repository.saveCustomReminder(request).fold(
                onSuccess = { dismissAddDialog() },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Could not save reminder") }
                }
            )
        }
    }

    fun updateReminder(request: CustomReminderRequest) {
        val id = request.id ?: return
        viewModelScope.launch {
            repository.updateReminder(id, request).fold(
                onSuccess = { dismissEditDialog() },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Could not update reminder") }
                }
            )
        }
    }

    fun markCompleted(reminderId: String) {
        viewModelScope.launch { repository.markCompleted(reminderId) }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch { repository.deleteReminder(reminderId) }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun clearInfoMessage() = _uiState.update { it.copy(infoMessage = null) }
}
