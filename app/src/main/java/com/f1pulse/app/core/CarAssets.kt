package com.f1pulse.app.core

/**
 * Maps constructorId to bundled car images in `assets/cars/`.
 * v1.4 新增：赛车右侧图，用于车手详情页赛车展示区。
 */
object CarAssets {

    private val carImages = mapOf(
        "mercedes" to "cars/mercedes.webp",
        "ferrari" to "cars/ferrari.webp",
        "mclaren" to "cars/mclaren.webp",
        "red_bull" to "cars/red_bull.webp",
        "alpine" to "cars/alpine.webp",
        "rb" to "cars/rb.webp",
        "haas" to "cars/haas.webp",
        "williams" to "cars/williams.webp",
        "sauber" to "cars/sauber.webp",
        "audi" to "cars/sauber.webp",
        "aston_martin" to "cars/aston_martin.webp",
        "cadillac" to "cars/cadillac.webp",
        // Legacy ids.
        "alphatauri" to "cars/rb.webp",
        "alfa" to "cars/sauber.webp",
        "renault" to "cars/alpine.webp",
    )

    fun carAsset(constructorId: String?): String? = constructorId?.let { carImages[it] }
}
