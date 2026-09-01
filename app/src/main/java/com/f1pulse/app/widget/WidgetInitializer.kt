package com.f1pulse.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.f1pulse.app.widget.worker.WidgetRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/** Owns the single WorkManager cadence shared by all installed F1 widgets. */
object WidgetInitializer {
    private const val UNIQUE_NAME = "f1_pulse_widget_refresh"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun scheduleFromSettings(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            val interval = WidgetEntryPointAccess.from(appContext)
                .settingsDataStore()
                .flow
                .first()
                .widgetRefreshMinutes
            schedule(appContext, interval)
        }
    }

    fun schedule(context: Context, intervalMinutes: Int = 30) {
        val workManager = WorkManager.getInstance(context)
        if (!hasAnyWidgets(context)) {
            workManager.cancelUniqueWork(UNIQUE_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            intervalMinutes.coerceIn(15, 240).toLong(),
            TimeUnit.MINUTES,
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build(),
        ).build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancelIfNoWidgets(context: Context) {
        if (!hasAnyWidgets(context)) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }

    private fun hasAnyWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        // Derived from WidgetUpdater's registry so the two can never drift apart.
        return WidgetUpdater.allWidgets.any { entry ->
            manager.getAppWidgetIds(ComponentName(context, entry.receiver)).isNotEmpty()
        }
    }
}