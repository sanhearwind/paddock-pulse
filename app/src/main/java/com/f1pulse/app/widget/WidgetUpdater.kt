package com.f1pulse.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CancellationException

/**
 * Single source of truth for the full set of F1 Glance widgets.
 *
 * Each entry pairs the widget with its receiver so re-rendering ([updateAll]) and
 * "is any widget installed?" ([WidgetInitializer]) read from the same list. They used to be
 * two hand-maintained lists, which meant adding a widget to one and forgetting the other
 * silently broke background refresh for anyone who only installed the new widget.
 */
object WidgetUpdater {

    data class Entry(
        val widget: GlanceAppWidget,
        val receiver: Class<out GlanceAppWidgetReceiver>,
    )

    val allWidgets: List<Entry> = listOf(
        Entry(CountdownWidget(), CountdownWidgetReceiver::class.java),
        Entry(NextRaceInfoWidget(), NextRaceInfoWidgetReceiver::class.java),
        Entry(DriverStandingsWidget(), DriverStandingsWidgetReceiver::class.java),
        Entry(ConstructorStandingsWidget(), ConstructorStandingsWidgetReceiver::class.java),
        Entry(FavoriteTeamWidget(), FavoriteTeamWidgetReceiver::class.java),
        Entry(FavoriteDriverWidget(), FavoriteDriverWidgetReceiver::class.java),
        Entry(LastSessionWidget(), LastSessionWidgetReceiver::class.java),
    )

    /**
     * Re-renders every widget instance of every type. A failure on one widget type
     * is swallowed so the remaining widgets still update.
     */
    suspend fun updateAll(context: Context) {
        allWidgets.forEach { entry ->
            try {
                entry.widget.updateAll(context)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // One widget failure must not prevent the others from updating.
            }
        }
    }
}
