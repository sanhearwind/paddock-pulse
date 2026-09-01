package com.f1pulse.app.data.repository

import com.f1pulse.app.database.entity.RaceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Jolpica and OpenF1 spell the same venue differently in both directions, so matching an
 * OpenF1 meeting/session onto a Jolpica round needs more than one comparison.
 *
 * The strings below are the real 2026 values from both APIs.
 */
class RaceMatcherTest {

    private fun race(round: Int, locality: String, country: String) = RaceEntity(
        season = 2026,
        round = round,
        raceName = "R$round",
        circuitId = "c$round",
        circuitName = "C$round",
        locality = locality,
        country = country,
        lat = 0.0,
        lng = 0.0,
        raceDateUtc = 0L,
        raceTimeUtc = null,
        fp1Utc = null,
        fp2Utc = null,
        fp3Utc = null,
        qualifyingUtc = null,
        sprintUtc = null,
        sprintQualifyingUtc = null,
        openF1MeetingKey = null,
        circuitSvgAsset = null,
        countryFlagCode = null,
    )

    // A representative slice of the 2026 calendar, including both repeated-country cases.
    private val miami = race(4, "Miami", "USA")
    private val barcelona = race(7, "Barcelona", "Spain")
    private val monza = race(13, "Monza", "Italy")
    private val madrid = race(14, "Madrid", "Spain")
    private val austin = race(17, "Austin", "USA")
    private val vegas = race(20, "Las Vegas", "USA")
    private val montreal = race(5, "Montreal", "Canada")
    private val spa = race(10, "Spa", "Belgium")
    private val abuDhabi = race(22, "Abu Dhabi", "UAE")

    private val matcher = RaceMatcher(
        listOf(miami, montreal, barcelona, spa, monza, madrid, austin, vegas, abuDhabi),
    )

    @Test
    fun spanishRoundsAreNotCollapsedOntoTheFirstOne() {
        // The regression this class exists for: both are country "Spain", and a
        // country-first match sent Madrid's sessions to Barcelona's round.
        assertEquals(barcelona, matcher.match("Spain", "Barcelona"))
        assertEquals(madrid, matcher.match("Spain", "Madrid"))
    }

    @Test
    fun americanRoundsMatchByLocalityDespiteTheCountryStringsDiffering() {
        // Jolpica says "USA", OpenF1 says "United States" — country can never match here.
        assertEquals(austin, matcher.match("United States", "Austin"))
        assertEquals(vegas, matcher.match("United States", "Las Vegas"))
    }

    @Test
    fun miamiMatchesThroughLocalityContainment() {
        // Jolpica "Miami" vs OpenF1 "Miami Gardens"; three USA rounds so country cannot help.
        assertEquals(miami, matcher.match("United States", "Miami Gardens"))
    }

    @Test
    fun accentDifferencesDoNotBlockAMatch() {
        assertEquals(montreal, matcher.match("Canada", "Montréal"))
    }

    @Test
    fun uniqueCountryRescuesADivergentLocality() {
        // OpenF1 "Spa-Francorchamps" / "Yas Marina" vs Jolpica "Spa" / "Abu Dhabi".
        assertEquals(spa, matcher.match("Belgium", "Spa-Francorchamps"))
        assertEquals(abuDhabi, matcher.match("United Arab Emirates", "Yas Marina"))
    }

    @Test
    fun unknownVenuesMatchNothing() {
        // Pre-season testing at Sakhir, with no Bahrain round on the 2026 calendar.
        assertNull(matcher.match("Bahrain", "Sakhir"))
        assertNull(matcher.match(null, null))
    }

    @Test
    fun ambiguityIsDroppedRatherThanGuessed() {
        // Two rounds sharing a locality and a country: nothing distinguishes them, so
        // attaching the enrichment to either would be a guess.
        val a = race(1, "Springfield", "Nowhere")
        val b = race(2, "Springfield", "Nowhere")
        assertNull(RaceMatcher(listOf(a, b)).match("Nowhere", "Springfield"))
    }

    @Test
    fun containmentStillResolvesWhenOnlyOneRoundCanMatch() {
        val a = race(1, "Springfield", "Nowhere")
        val b = race(2, "Shelbyville", "Nowhere")
        assertEquals(a, RaceMatcher(listOf(a, b)).match("Nowhere", "Springfield East"))
    }
}
