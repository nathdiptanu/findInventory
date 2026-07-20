package com.docufind.app.ui.screens.splash



import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope

import com.docufind.app.data.local.db.DocuFindDatabaseFactory

import com.docufind.app.data.local.db.migration.DatabaseOpenState

import com.docufind.app.domain.repository.PreferencesRepository

import com.docufind.app.domain.repository.SearchRepository

import com.docufind.app.reminder.ReminderEngine

import com.docufind.app.reminder.ReminderNotificationHelper

import com.docufind.app.ui.navigation.DocuFindRoutes

import dagger.Lazy

import dagger.hilt.android.lifecycle.HiltViewModel

import javax.inject.Inject

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.first

import kotlinx.coroutines.launch



enum class DatabaseStartupState {

    LOADING,

    READY,

    FAILED

}



@HiltViewModel

class SplashViewModel @Inject constructor(

    private val databaseFactory: DocuFindDatabaseFactory,

    private val databaseOpenState: DatabaseOpenState,

    private val preferencesRepository: PreferencesRepository,

    private val lazySearchRepository: Lazy<SearchRepository>,

    private val lazyReminderEngine: Lazy<ReminderEngine>,

    private val notificationHelper: ReminderNotificationHelper

) : ViewModel() {



    private val _destination = MutableStateFlow<String?>(null)

    val destination: StateFlow<String?> = _destination.asStateFlow()



    private val _startupState = MutableStateFlow(DatabaseStartupState.LOADING)

    val startupState: StateFlow<DatabaseStartupState> = _startupState.asStateFlow()



    val migrationError = databaseOpenState.failure



    private var animationComplete = false

    private var pendingRoute: String? = null

    private var dbReady = false



    init {

        viewModelScope.launch {

            runCatching { databaseFactory.get() }

                .onSuccess {

                    notificationHelper.ensureChannel()

                    runCatching { lazySearchRepository.get().reindexAllRecords() }

                    runCatching { lazyReminderEngine.get().rescheduleAllActive() }

                    dbReady = true

                    _startupState.value = DatabaseStartupState.READY

                    resolveRoute()

                    maybeNavigate()

                }

                .onFailure {

                    _startupState.value = DatabaseStartupState.FAILED

                }

        }

    }



    fun onAnimationComplete() {

        animationComplete = true

        maybeNavigate()

    }



    fun onNavigated() {

        _destination.value = null

    }



    private suspend fun resolveRoute() {

        val prefs = preferencesRepository.preferences.first()

        pendingRoute = when {

            !prefs.onboardingCompleted -> DocuFindRoutes.ONBOARDING

            !prefs.profileCompleted -> DocuFindRoutes.PROFILE_SETUP

            else -> DocuFindRoutes.MAIN

        }

    }



    private fun maybeNavigate() {

        if (

            animationComplete &&

            dbReady &&

            pendingRoute != null &&

            _startupState.value == DatabaseStartupState.READY

        ) {

            _destination.value = pendingRoute

            pendingRoute = null

        }

    }

}


