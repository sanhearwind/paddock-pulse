package com.f1pulse.app.data.repository

import com.f1pulse.app.database.entity.RaceEntity

/**
 * Resolves an OpenF1 meeting/session (identified only by free-text `country_name` and
 * `location`) to the Jolpica round it belongs to.
 *
 * The two APIs disagree on spelling in both directions, so a single comparison cannot work:
 *
 * | Round | Jolpica locality / country | OpenF1 location / country_name |
 * |-------|----------------------------|--------------------------------|
 * | Miami | `Miami` / `USA`            | `Miami Gardens` / `United States` |
 * | Canada| `Montreal` / `Canada`      | `Montréal` / `Canada`          |
 * | Belgium| `Spa` / `Belgium`         | `Spa-Francorchamps` / `Belgium`|
 * | Abu Dhabi| `Abu Dhabi` / `UAE`     | `Yas Marina` / `United Arab Emirates` |
 *
 * The previous implementation used `country == x || locality == y` with `firstOrNull`, which
 * meant the *first* race in a country absorbed every later race in that same country. In 2026
 * that mis-assigned Madrid (round 14) onto Barcelona (round 7), and matched Miami to nothing.
 *
 * Strategy, most-specific first:
 *  1. exact locality match
 *  2. country match, but only when that country hosts exactly one race that season
 *  3. locality containment (either direction), but only when it resolves to exactly one race
 *
 * Anything still ambiguous returns null — dropping the enrichment is strictly better than
 * attaching it to the wrong round.
 */
internal class RaceMatcher(private val races: List<RaceEntity>) {

    private val byLocality: Map<String, List<RaceEntity>> =
        races.groupBy { it.locality.normalise() }

    private val byCountry: Map<String, List<RaceEntity>> =
        races.groupBy { it.country.canonicalCountry() }

    fun match(countryName: String?, location: String?): RaceEntity? {
        val loc = location?.normalise().orEmpty()
        val country = countryName?.canonicalCountry().orEmpty()

        if (loc.isNotEmpty()) {
            byLocality[loc]?.singleOrNull()?.let { return it }
        }
        if (country.isNotEmpty()) {
            byCountry[country]?.singleOrNull()?.let { return it }
        }
        if (loc.isNotEmpty()) {
            val contained = races.filter { race ->
                val l = race.locality.normalise()
                l.isNotEmpty() && (l.contains(loc) || loc.contains(l))
            }
            contained.singleOrNull()?.let { return it }
        }
        return null
    }
}

/** Lower-cases, trims and strips accents so `Montréal` and `Montreal` compare equal. */
private fun String.normalise(): String =
    java.text.Normalizer.normalize(trim().lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")

/**
 * Jolpica abbreviates several country names that OpenF1 spells out. Without this, a round like
 * Abu Dhabi (`UAE` vs `United Arab Emirates`, locality `Abu Dhabi` vs `Yas Marina`) matches on
 * neither field and loses its enrichment entirely.
 */
private val COUNTRY_ALIASES = mapOf(
    "united arab emirates" to "uae",
    "united states" to "usa",
    "united states of america" to "usa",
    "united kingdom" to "uk",
    "great britain" to "uk",
    "korea" to "south korea",
)

private fun String.canonicalCountry(): String {
    val n = normalise()
    return COUNTRY_ALIASES[n] ?: n
}
