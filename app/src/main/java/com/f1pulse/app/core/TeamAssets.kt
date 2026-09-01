package com.f1pulse.app.core

/**
 * Maps Jolpica `constructorId` to bundled logo assets in `assets/team_logos/`.
 * Used by the UI's [com.f1pulse.app.ui.components.TeamLogo] composable. Logos are
 * sourced from F1 media CDN (WebP) and bundled locally so no network fetch is
 * required and the widget always renders.
 */
object TeamAssets {

    private val logoAssets = mapOf(
        "ferrari" to "team_logos/ferrari.webp",
        "red_bull" to "team_logos/red_bull.webp",
        "mercedes" to "team_logos/mercedes.webp",
        "mclaren" to "team_logos/mclaren.webp",
        "aston_martin" to "team_logos/aston_martin.webp",
        "alpine" to "team_logos/alpine.webp",
        "williams" to "team_logos/williams.webp",
        "rb" to "team_logos/rb.webp",
        "sauber" to "team_logos/audi.webp",
        "audi" to "team_logos/audi.webp",
        "haas" to "team_logos/haas.webp",
        "cadillac" to "team_logos/cadillac.webp",
        // Legacy ids for historical data.
        "alphatauri" to "team_logos/rb.webp",
        "alfa" to "team_logos/sauber.webp",
        "renault" to "team_logos/alpine.webp",
    )

    /** Returns the `assets/` path for the team logo, or null if unknown. */
    fun logoAsset(constructorId: String?): String? = constructorId?.let { logoAssets[it] }

    /** Returns true if a bundled logo exists for the constructor id. */
    fun hasLogo(constructorId: String?): Boolean = logoAsset(constructorId) != null
}
