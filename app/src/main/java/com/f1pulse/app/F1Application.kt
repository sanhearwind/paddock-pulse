package com.f1pulse.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.f1pulse.app.data.prefs.SettingsDataStore
import com.f1pulse.app.widget.WidgetInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point. Wires Hilt (which in turn provides WorkerFactory for
 * `@HiltWorker`-annotated workers), schedules the widget refresh WorkManager
 * job once on first launch, and exposes the Coil [ImageLoader] (with SVG
 * decoding) via [ImageLoaderFactory] so `LocalContext.current.imageLoader`
 * returns the configured instance.
 */
@HiltAndroidApp
class F1Application : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader = imageLoader

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            WidgetInitializer.schedule(this@F1Application, settingsDataStore.flow.first().widgetRefreshMinutes)
        }
    }
}
