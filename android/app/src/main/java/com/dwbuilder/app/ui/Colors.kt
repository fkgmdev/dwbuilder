package com.dwbuilder.app.ui

import androidx.compose.ui.graphics.Color

/** Attunement accents keyed by canonical attunement name (site palette). */
val attunementColors: Map<String, Color> = mapOf(
    "Flamecharm" to Color(0xFFFFA046),
    "Frostdraw" to Color(0xFF5CFFFF),
    "Thundercall" to Color(0xFFFBFF00),
    "Galebreathe" to Color(0xFFA7FF7E),
    "Shadowcast" to Color(0xFF9B5CFF),
    "Ironsing" to Color(0xFF90CAF9),
    "Bloodrend" to Color(0xFFF2555A),
)

/** Rarity accents used for chips/badges across the catalog lists. */
private val rarityColors: Map<String, Color> = mapOf(
    "Common" to Color(0xFFB0B4AB),
    "Rare" to Color(0xFF4A9EFF),
    "Advanced" to Color(0xFFB388FF),
    "Faction" to Color(0xFFFFB74D),
    "Oath" to Color(0xFFFFD54F),
    "Quest" to Color(0xFFF48FB1),
    "Murmur" to Color(0xFFCE93D8),
    "Origin" to Color(0xFF4DD0E1),
    "Innate" to Color(0xFFAED581),
    "Equipment" to Color(0xFFBCAAA4),
)

fun rarityColor(rarity: String): Color = rarityColors[rarity] ?: Color(0xFF9AA093)