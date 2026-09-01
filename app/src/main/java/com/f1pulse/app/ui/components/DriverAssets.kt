package com.f1pulse.app.ui.components

/** Returns a bundled headshot only when the driver code is explicitly known. */
internal fun driverHeadshotAsset(code: String?): String? = when (code?.lowercase()) {
    "rus" -> "george_russell.webp"
    "ant" -> "kimi_antonelli.webp"
    "lec" -> "charles_leclerc.webp"
    "ham" -> "lewis_hamilton.webp"
    "nor" -> "lando_norris.webp"
    "pia" -> "oscar_piastri.webp"
    "ver" -> "max_verstappen.webp"
    "had" -> "isack_hadjar.webp"
    "gas" -> "pierre_gasly.webp"
    "col" -> "franco_colapinto.webp"
    "law" -> "liam_lawson.webp"
    "lin" -> "arvid_lindblad.webp"
    "oco" -> "esteban_ocon.webp"
    "bea" -> "oliver_bearman.webp"
    "sai" -> "carlos_sainz.webp"
    "alb" -> "alexander_albon.webp"
    "hul" -> "nico_hulkenberg.webp"
    "bor" -> "gabriel_bortoleto.webp"
    "alo" -> "fernando_alonso.webp"
    "str" -> "lance_stroll.webp"
    "per" -> "sergio_perez.webp"
    "bot" -> "valtteri_bottas.webp"
    else -> null
}?.let { "file:///android_asset/driver_headshots/$it" }

/** 返回已裁好的 200x200 方形大头照素材路径（用于排行榜等小尺寸场景）。 */
internal fun driverHeadshotSquareAsset(code: String?): String? = when (code?.lowercase()) {
    "rus" -> "george_russell.webp"
    "ant" -> "kimi_antonelli.webp"
    "lec" -> "charles_leclerc.webp"
    "ham" -> "lewis_hamilton.webp"
    "nor" -> "lando_norris.webp"
    "pia" -> "oscar_piastri.webp"
    "ver" -> "max_verstappen.webp"
    "had" -> "isack_hadjar.webp"
    "gas" -> "pierre_gasly.webp"
    "col" -> "franco_colapinto.webp"
    "law" -> "liam_lawson.webp"
    "lin" -> "arvid_lindblad.webp"
    "oco" -> "esteban_ocon.webp"
    "bea" -> "oliver_bearman.webp"
    "sai" -> "carlos_sainz.webp"
    "alb" -> "alexander_albon.webp"
    "hul" -> "nico_hulkenberg.webp"
    "bor" -> "gabriel_bortoleto.webp"
    "alo" -> "fernando_alonso.webp"
    "str" -> "lance_stroll.webp"
    "per" -> "sergio_perez.webp"
    "bot" -> "valtteri_bottas.webp"
    else -> null
}?.let { "file:///android_asset/driver_headshots_square/$it" }