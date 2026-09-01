package com.f1pulse.app.core

/**
 * Fallback team colours keyed by Jolpica constructorId. Hex stored WITHOUT leading `#`,
 * matching OpenF1's `team_colour` convention.
 *
 * v1.4 更新：对齐 F1 官网 2026 赛季车队色板。
 * - Ferrari: DC0000 → E8002D
 * - Alpine: 0093CC → 00A1E8
 * - Williams: 64C4FF → 1868DB
 * - Sauber→Audi: 52E252 → FF2D00
 * - Haas: B6BABD → DEE1E2
 * - 新增 Cadillac: AAAAAD
 */
object TeamColors {

    private val byId = mapOf(
        "ferrari" to "E8002D",
        "red_bull" to "3671C6",
        "mercedes" to "27F4D2",
        "mclaren" to "FF8000",
        "aston_martin" to "229971",
        "alpine" to "00A1E8",
        "williams" to "1868DB",
        "rb" to "6692FF",
        "sauber" to "FF2D00",
        "audi" to "FF2D00",
        "haas" to "DEE1E2",
        "cadillac" to "AAAAAD",
        // Legacy ids for historical data.
        "alphatauri" to "4E6FA8",
        "alfa" to "900000",
        "renault" to "FFF500",
        "racing_point" to "F596C8",
        "force_india" to "F596C8",
    )

    fun hexFor(constructorId: String?): String? = constructorId?.let { byId[it] }
}
