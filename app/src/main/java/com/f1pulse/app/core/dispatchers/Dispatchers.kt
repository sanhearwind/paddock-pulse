package com.f1pulse.app.core.dispatchers

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Dispatcher(val type: DispatcherType)

enum class DispatcherType { IO, Default, Main }
