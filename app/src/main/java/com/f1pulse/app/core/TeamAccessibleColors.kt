package com.f1pulse.app.core

/**
 * 2026 赛季车队 accessible-colour（深色变体），用于卡片渐变背景、
 * 浅色模式下的 Chip 背景等需要保证文字对比度的场景。
 *
 * 数据来源：F1 官网 team-colour / accessible-colour 配对。
 * 当 team-colour 过亮（如 Haas #DEE1E2、Mercedes #27F4D2）时，
 * accessible-colour 提供可读的深色替代。
 */
object TeamAccessibleColors {

    private val byId = mapOf(
        "ferrari" to "5C0012",
        "red_bull" to "142948",
        "mercedes" to "067E6A",
        "mclaren" to "804000",
        "aston_martin" to "0F4331",
        "alpine" to "004E70",
        "williams" to "082145",
        "rb" to "0038C2",
        "sauber" to "751500",
        "audi" to "751500",
        "haas" to "667175",
        "cadillac" to "58585B",
        // Legacy ids.
        "alphatauri" to "2A3B5A",
        "alfa" to "4A0000",
        "renault" to "807A00",
        "racing_point" to "7A4A64",
        "force_india" to "7A4A64",
    )

    fun hexFor(constructorId: String?): String? = constructorId?.let { byId[it] }

    /**
     * 判断车队色是否为浅色（亮度高），在浅色主题下需要切换到 accessible 变体。
     */
    fun isLightTeamColour(hex: String?): Boolean {
        if (hex.isNullOrBlank()) return false
        val clean = hex.removePrefix("#")
        return runCatching {
            val r = clean.substring(0, 2).toInt(16)
            val g = clean.substring(2, 4).toInt(16)
            val b = clean.substring(4, 6).toInt(16)
            // 相对亮度近似：0.299R + 0.587G + 0.114B
            (0.299 * r + 0.587 * g + 0.114 * b) / 255 > 0.65
        }.getOrDefault(false)
    }
}
