package com.f1pulse.app.core

/**
 * Maps Jolpica `Location.country` (English country name) to ISO-3166 alpha-2 code
 * for flag emoji rendering. Covers the ~24 F1 host nations; unknown → null.
 */
object CountryFlags {

    private val byName = mapOf(
        "Bahrain" to "BH",
        "Saudi Arabia" to "SA",
        "Australia" to "AU",
        "Japan" to "JP",
        "China" to "CN",
        "Azerbaijan" to "AZ",
        "USA" to "US",
        "United States" to "US",
        "Monaco" to "MC",
        "Spain" to "ES",
        "Canada" to "CA",
        "Austria" to "AT",
        "UK" to "GB",
        "Great Britain" to "GB",
        "United Kingdom" to "GB",
        "Hungary" to "HU",
        "Belgium" to "BE",
        "Netherlands" to "NL",
        "Italy" to "IT",
        "Singapore" to "SG",
        "Mexico" to "MX",
        "Brazil" to "BR",
        "Qatar" to "QA",
        "UAE" to "AE",
        "United Arab Emirates" to "AE",
        "Argentina" to "AR",
        "Russia" to "RU",
        "Turkey" to "TR",
        "France" to "FR",
        "Germany" to "DE",
        "Malaysia" to "MY",
        "Korea" to "KR",
        "South Korea" to "KR",
        "Thailand" to "TH",
        "Portugal" to "PT",
        "Sweden" to "SE",
        "Switzerland" to "CH",
        "South Africa" to "ZA",
    )

    fun fromLocation(country: String?): String? = country?.let { byName[it] }

    /** Converts an ISO alpha-2 code to a regional-flag emoji (e.g. "GB" → "🇬🇧"). */
    fun toEmoji(code: String?): String? = code?.takeIf { it.length == 2 }?.let {
        val a = Character.codePointAt(it, 0) - 0x41 + 0x1F1E6
        val b = Character.codePointAt(it, 1) - 0x41 + 0x1F1E6
        String(Character.toChars(a)) + String(Character.toChars(b))
    }
}
