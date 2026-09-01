package com.f1pulse.app.widget

import android.content.Context

/**
 * Stores the user's favorite constructor selection for [FavoriteTeamWidget].
 *
 * Persisted in SharedPreferences so the widget survives process death and reboots.
 */
object FavoriteWidgetStore {
    private const val PREFS = "favorite_widget_team"
    private const val KEY_CONSTRUCTOR_ID = "constructor_id"

    fun read(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CONSTRUCTOR_ID, null)

    fun save(context: Context, constructorId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CONSTRUCTOR_ID, constructorId)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_CONSTRUCTOR_ID)
            .apply()
    }
}

/**
 * Stores the user's favorite driver selection for [FavoriteDriverWidget].
 *
 * Persisted in SharedPreferences so the widget survives process death and reboots.
 */
object FavoriteDriverWidgetStore {
    private const val PREFS = "favorite_widget_driver"
    private const val KEY_DRIVER_CODE = "driver_code"

    fun read(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_DRIVER_CODE, null)

    fun save(context: Context, driverCode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DRIVER_CODE, driverCode)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_DRIVER_CODE)
            .apply()
    }
}
