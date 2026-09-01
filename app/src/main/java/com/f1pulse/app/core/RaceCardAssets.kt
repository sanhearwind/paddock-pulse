package com.f1pulse.app.core

/**
 * Maps circuitId to race card image slugs in `assets/race_cards/`.
 * v1.4 新增：赛事卡片图，用于比赛详情 Header 和赛历缩略图。
 *
 * 注意：部分 slug 与 circuitId 不同（如 Abu Dhabi 是 abu-dhabi 非 united-arab-emirates）。
 */
object RaceCardAssets {

    private val cardSlugs = mapOf(
        "albert_park" to "albert_park",
        "bahrain" to "bahrain",
        "jeddah" to "jeddah",
        "suzuka" to "suzuka",
        "shanghai" to "shanghai",
        "miami" to "miami",
        "monaco" to "monaco",
        "villeneuve" to "villeneuve",
        "catalunya" to "catalunya",
        "red_bull_ring" to "red_bull_ring",
        "silverstone" to "silverstone",
        "hungaroring" to "hungaroring",
        "spa" to "spa",
        "zandvoort" to "zandvoort",
        "monza" to "monza",
        "baku" to "baku",
        "marina_bay" to "marina_bay",
        "americas" to "americas",
        "rodriguez" to "rodriguez",
        "interlagos" to "interlagos",
        "vegas" to "vegas",
        "losail" to "losail",
        "yas_marina" to "yas_marina",
        "madrid" to "madrid",
    )

    fun cardSlug(circuitId: String?): String? = circuitId?.let { cardSlugs[it] }

    fun cardAsset(circuitId: String?): String? = cardSlug(circuitId)?.let { "race_cards/$it.webp" }
}
