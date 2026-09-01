package com.f1pulse.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.f1pulse.app.core.result.Resource
import kotlinx.coroutines.CancellationException

/** Refreshes widget data, persists feedback, then re-renders every widget type. */
class RefreshWidgetsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val result = try {
            WidgetDataLoader.refreshAll(
                context = context,
                scheduleSeason = WidgetDataLoader.scheduleSeason(),
                standingsSeason = WidgetDataLoader.standingsSeason(),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Resource.Error(error.message ?: "Widget refresh failed")
        }
        WidgetRefreshStatusStore.record(context, succeeded = result !is Resource.Error)
        WidgetUpdater.updateAll(context)
    }
}