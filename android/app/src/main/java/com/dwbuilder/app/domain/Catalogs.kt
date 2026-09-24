package com.dwbuilder.app.domain

/**
 * Hardcoded identity catalogs and labels — ported from C74yZVe_.js (`Ms`, `Bs`, `Ps`,
 * oath requirement map `g`, race label formatter `N`, stat abbreviations `D`/`E`).
 */
object Catalogs {

    val MURMURS: List<String> = listOf("Ardour", "Rhythm", "Tacet")

    val ORIGINS: List<String> = listOf(
        "Castaway", "Authority Ensign", "Deepbound", "Ignition Delver",
        "Lone Warrior", "Voidwalker", "Justicar",
    )

    val BELLS: List<String> = listOf(
        "None", "Blood Scourge", "Crazy Slots", "Chorus Divide", "Dimensional Travel",
        "Gravity Field", "Jar Of Souls", "Paralytic Dust", "Payback", "Portals",
        "Preservation", "Resurrection", "Run It Back", "Sacred Field", "Shard Bow",
        "Skeleton Key", "Smite", "Smokescreen", "Teleportation", "Wind Up",
    )

    /** Oath picker stat requirements (site map `g`). */
    val OATH_REQUIREMENTS: Map<String, String> = mapOf(
        "Oathless" to "No Stat Requirement",
        "Arcwarder" to "20 FIR + 20 LTN + 20 FTD",
        "Bladeharper" to "(75 MED or comb. 90 WEP) + (25 STR or AGL)",
        "Blightsurger" to "(comb. 80 STR, FTD, AGL) + (40 WND or LTN)",
        "Blindseer" to "40 WLL",
        "Chainwarden" to "comb. 40 STR, FTD, WLL",
        "Contractor" to "No Stat Requirement",
        "Dawnwalker" to "LVL 15",
        "Fadetrimmer" to "LVL 12",
        "Jetstriker" to "50 AGL",
        "Linkstrider" to "No Stat Requirement",
        "Saintsworn" to "15 FIR + 15 ICE + 15 LTN + 15 WND + 15 SDW",
        "Saltchemist" to "75 INT",
        "Silentheart" to "(comb. 75 WEP) + (25 STR) + (25 AGL or CHA)",
        "Soulbreaker" to "comb. 50 WLL, CHA",
        "Starkindred" to "40 STR",
        "Visionshaper" to "50 CHA",
    )

    /** Oath label, e.g. "Blindseer (40 WLL)". */
    fun oathLabel(oath: String): String {
        val req = OATH_REQUIREMENTS[oath] ?: return oath
        return "$oath ($req)"
    }

    /** Race label with positive stat bonuses, e.g. "Etrean (+2 AGL +3 INT)". */
    fun raceLabel(race: String, statBonuses: Map<String, Int>): String {
        val parts = statBonuses.entries
            .filter { it.value > 0 }
            .map { "+${it.value} ${raceStatAbbrev[it.key] ?: it.key}" }
            .toList()
        return if (parts.isEmpty()) race else "$race (${parts.joinToString(" ")})"
    }

    private val raceStatAbbrev = mapOf(
        "Strength" to "STR", "Fortitude" to "FTD", "Agility" to "AGL",
        "Intelligence" to "INT", "Willpower" to "WIL", "Charisma" to "CHA",
    )

    /** Stat abbreviations used across the UI (site `D` map). */
    val STAT_SHORT: Map<String, String> = mapOf(
        "Strength" to "STR", "Fortitude" to "FTD", "Agility" to "AGI",
        "Intelligence" to "INT", "Willpower" to "WLL", "Charisma" to "CHA",
        "Heavy Wep." to "HVY", "Medium Wep." to "MED", "Light Wep." to "LHT",
        "Flamecharm" to "FLM", "Frostdraw" to "ICE", "Thundercall" to "LTN",
        "Galebreathe" to "WND", "Shadowcast" to "SDW", "Ironsing" to "MTL", "Bloodrend" to "BLD",
    )

    /** Canonical base/weapon/attunement stat names used by the attribute store. */
    val BASE_STATS: List<String> = listOf("Strength", "Fortitude", "Agility", "Intelligence", "Willpower", "Charisma")
    val WEAPON_STATS: List<String> = listOf("Heavy Weapon", "Medium Weapon", "Light Weapon")
    val ATTUNEMENT_STATS: List<String> = listOf(
        "Flamecharm", "Frostdraw", "Thundercall", "Galebreathe", "Shadowcast", "Ironsing", "Bloodrend",
    )

    /** Equipment slot names (site uses these as the 7 gear categories). */
    val EQUIPMENT_SLOTS: List<String> = listOf("Head", "Arms", "Legs", "Torso", "Face", "Earrings", "Rings")

    /**
     * The 8 weapon enchants from the weapon breakdown picker (CkKCXiBo.js `Z`).
     */
    data class WeaponEnchant(val name: String, val effect: String)

    val WEAPON_ENCHANTS: List<WeaponEnchant> = listOf(
        WeaponEnchant("None", ""),
        WeaponEnchant("Iron", "+10% PEN"),
        WeaponEnchant("Gold", "−10% dmg, slow"),
        WeaponEnchant("Umbrite", "+10% dmg, wither, 25% slower"),
        WeaponEnchant("Erisore", "−20% dmg, 50% anti-heal"),
        WeaponEnchant("Irithine", "−20% dmg, shaky block"),
        WeaponEnchant("Gale", "−5% ground / +25% air"),
        WeaponEnchant("Frost", "−5% dmg, freeze after 7 hits"),
    )

    /** Mantra gem catalog (C74yZVe_.js `sn`). */
    data class Gem(val name: String, val color: String, val effect: String)

    val GEMS: List<Gem> = listOf(
        Gem("None", "", ""),
        Gem("Aegis", "#9489c7", "damage reduction on hit"),
        Gem("Blessed", "#67d8c0", "reduces cooldown on hit"),
        Gem("Bloodless", "#9b4f4f", "grants lifesteal, −20% mantra damage"),
        Gem("Blue", "#3a85c7", "reduces ether cost"),
        Gem("Insignia", "#1a3df0", "reduces windup of next cast"),
        Gem("Kyrsan", "#a96fb8", "stacking slow on hit"),
        Gem("Might", "#e08d5e", "increases posture damage"),
        Gem("Nocturnal", "#5a3a1f", "extra hit after 1s delay"),
        Gem("Warped Blue", "#a8b3b8", "reduced ether cost while swimming"),
        Gem("Wayward", "#1f6a7a", "teleports to opponent after 1s"),
        Gem("Wrath", "#d63a3a", "crit damage scaling on combat tags"),
        Gem("Wind", "#70d58b", "speed boost on hit"),
    )

    /** Mantra spark catalog (C74yZVe_.js `ln`). */
    data class Spark(val name: String, val effect: String)

    val SPARKS: List<Spark> = listOf(
        Spark("None", ""), Spark("Blast", ""), Spark("Reversal", ""), Spark("Multiplying", ""),
        Spark("Magnet", ""), Spark("Spring", ""), Spark("Tornado", ""), Spark("Round", ""),
    )

    /** Base traits editable on the stats tab (KwKd8cfB.js `e0`). */
    val TRAITS: List<String> = listOf("Vitality", "Erudition", "Proficiency", "Songchant")

    /**
     * Mandatory trait-effect constants (site, verified on the live page):
     * Vitality +10 HP/point, Songchant +6.5% mantra scaling/point. Erudition/Proficiency map
     * onto the Knowledge/XP systems (no direct stat effect).
     */
    const val VITALITY_HP_PER_POINT: Int = 10
    const val SONGCHANT_SCALING_PER_POINT: Double = 0.065
}