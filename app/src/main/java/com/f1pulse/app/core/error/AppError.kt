package com.f1pulse.app.core.error

/**
 * App-level error classification. Mapped to user-facing strings in the UI.
 */
sealed class AppError(open val message: String) {
    data class Network(override val message: String = "Network unavailable") : AppError(message)
    data class Server(val code: Int, override val message: String = "Server error") : AppError(message)
    data class Parse(override val message: String = "Failed to parse data") : AppError(message)
    data class Empty(override val message: String = "No data available") : AppError(message)
    data class Unknown(override val message: String = "Unknown error") : AppError(message)
}

fun Throwable.toAppError(): AppError = when (this) {
    is java.net.UnknownHostException,
    is java.net.SocketTimeoutException,
    is java.io.IOException -> AppError.Network(message ?: "Network unavailable")
    is retrofit2.HttpException -> AppError.Server(code(), message())
    else -> AppError.Unknown(message ?: "Unknown error")
}
