package com.docufind.app.data.local.db.migration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

class DatabaseMigrationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

@Singleton
class DatabaseOpenState @Inject constructor() {
    private val _failure = MutableStateFlow<DatabaseMigrationException?>(null)
    val failure: StateFlow<DatabaseMigrationException?> = _failure.asStateFlow()

    fun reportFailure(error: DatabaseMigrationException) {
        _failure.value = error
    }

    val hasFailure: Boolean get() = _failure.value != null
}
