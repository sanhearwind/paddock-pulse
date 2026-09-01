package com.f1pulse.app.widget.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.f1pulse.app.core.result.Resource
import com.f1pulse.app.domain.usecase.RefreshAllForWidgetUseCase
import com.f1pulse.app.widget.WidgetDataLoader
import com.f1pulse.app.widget.WidgetRefreshStatusStore
import com.f1pulse.app.widget.WidgetUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Periodic background worker that refreshes widget-relevant data (schedule + standings)
 * and forces all installed F1 widgets to re-render. Scheduled by
 * [com.f1pulse.app.widget.WidgetInitializer] at the configured interval when network
 * is connected.
 *
 * Hilt generates the assisted factory automatically via `@HiltWorker` + `@AssistedInject`
 * (requires `androidx.hilt.compiler` KSP processor, already configured in build.gradle.kts).
 */
@HiltWorker
class WidgetRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val refreshAllForWidgetUseCase: RefreshAllForWidgetUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val result: Resource<Unit> = try {
            refreshAllForWidgetUseCase(
                scheduleSeason = WidgetDataLoader.scheduleSeason(),
                standingsSeason = WidgetDataLoader.standingsSeason(),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            Resource.Error(t.message ?: "Widget refresh failed")
        }
        WidgetRefreshStatusStore.record(applicationContext, succeeded = result !is Resource.Error)
        WidgetUpdater.updateAll(applicationContext)
        return if (result is Resource.Error) Result.retry() else Result.success()
    }
}
