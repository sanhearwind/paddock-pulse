package com.f1pulse.app.core.result

/**
 * Generic UI-facing resource state. ViewModels expose this via StateFlow.
 */
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val message: String, val cached: Nothing? = null) : Resource<Nothing>
}

inline fun <T> resource(block: () -> T): Resource<T> = try {
    Resource.Success(block())
} catch (t: Throwable) {
    Resource.Error(t.message ?: "Unknown error")
}
