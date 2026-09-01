package com.f1pulse.app.core.time

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** User-selectable timezone display mode (Settings). */
enum class TimeZoneMode { DEVICE, RACE_LOCAL }

/**
 * Loads the bundled `assets/circuit_timezone.json` (circuitId → IANA zone)
 * and resolves a [ZoneId] for a given circuit. Falls back to UTC.
 */
@Singleton
class CircuitZoneProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile private var cache: Map<String, String>? = null

    suspend fun zoneFor(circuitId: String?): ZoneId {
        if (circuitId == null) return ZoneId.systemDefault()
        val map = cache ?: load().also { cache = it }
        val zoneId = map[circuitId]
        return runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault())
    }

    private suspend fun load(): Map<String, String> = withContext(Dispatchers.IO) {
        runCatching {
            context.assets.open("circuit_timezone.json").bufferedReader().use { reader ->
                val obj = JSONObject(reader.readText())
                val result = HashMap<String, String>()
                for (key in obj.keys()) result[key] = obj.getString(key)
                result
            }
        }.getOrDefault(emptyMap())
    }
}

/**
 * Resolves the display [ZoneId] for a given circuit according to the user's
 * [TimeZoneMode] preference. Used by every UI screen and widget.
 */
class TimeZoneResolver @Inject constructor(
    private val circuitZoneProvider: CircuitZoneProvider,
) {
    suspend fun resolve(mode: TimeZoneMode, circuitId: String?): ZoneId = when (mode) {
        TimeZoneMode.DEVICE -> ZoneId.systemDefault()
        TimeZoneMode.RACE_LOCAL -> circuitZoneProvider.zoneFor(circuitId)
    }
}
