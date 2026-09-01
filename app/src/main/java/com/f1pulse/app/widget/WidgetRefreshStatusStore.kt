package com.f1pulse.app.widget

import android.content.Context

data class WidgetRefreshStatus(
    val failed: Boolean,
    val lastUpdatedEpochMillis: Long,
)

/** Persists refresh feedback so every widget instance renders the same result. */
object WidgetRefreshStatusStore {
    private const val PREFS = "widget_refresh_status"
    private const val KEY_FAILED = "failed"
    private const val KEY_UPDATED_AT = "updated_at"

    fun read(context: Context): WidgetRefreshStatus {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return WidgetRefreshStatus(
            failed = prefs.getBoolean(KEY_FAILED, false),
            lastUpdatedEpochMillis = prefs.getLong(KEY_UPDATED_AT, 0L),
        )
    }

    fun record(context: Context, succeeded: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FAILED, !succeeded)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }
}