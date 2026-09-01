package com.f1pulse.app.ui

import com.f1pulse.app.core.result.Resource

/**
 * Standard UI state used by every screen ViewModel.
 * - [Loading]: initial fetch in progress, no data yet
 * - [Success]: data available; [isOffline] flags cached-only refresh
 * - [Error]: nothing to show
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T, val isOffline: Boolean = false) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun <T> Resource<T>.toErrorOrNull(): String? = when (this) {
    is Resource.Error -> message
    else -> null
}
