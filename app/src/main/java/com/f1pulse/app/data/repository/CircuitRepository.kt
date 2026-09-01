package com.f1pulse.app.data.repository

import android.content.Context
import com.f1pulse.app.core.dispatchers.Dispatcher
import com.f1pulse.app.core.dispatchers.DispatcherType
import com.f1pulse.app.domain.model.CircuitMeta
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads bundled `assets/circuit_meta.json` (array of circuit metadata) and exposes
 * per-circuit lookup. In-memory cached after first load.
 */
@Singleton
class CircuitRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(DispatcherType.IO) private val io: CoroutineDispatcher,
) {
    @Volatile private var cache: Map<String, CircuitMeta>? = null

    private suspend fun load(): Map<String, CircuitMeta> = withContext(io) {
        runCatching {
            context.assets.open("circuit_meta.json").bufferedReader().use { reader ->
                val arr = JSONArray(reader.readText())
                val result = HashMap<String, CircuitMeta>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.getString("circuitId")
                    val meta = CircuitMeta(
                        svgAsset = o.optString("svgAsset").ifEmpty { null },
                        timeZone = o.optString("timeZone").ifEmpty { null },
                        lengthKm = o.optDouble("lengthKm", Double.NaN).takeIf { !it.isNaN() },
                        corners = o.optInt("corners", -1).takeIf { it >= 0 },
                        drsZones = o.optInt("drsZones", -1).takeIf { it >= 0 },
                        firstHeld = o.optInt("firstHeld", -1).takeIf { it >= 0 },
                        lapRecord = o.optString("lapRecord").ifEmpty { null },
                    )
                    result[id] = meta
                }
                result
            }
        }.getOrDefault(emptyMap())
    }

    suspend fun getCircuitMeta(circuitId: String): CircuitMeta? {
        val map = cache ?: load().also { cache = it }
        return map[circuitId]
    }
}
