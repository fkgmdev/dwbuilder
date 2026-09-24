package com.dwbuilder.app.domain.model

/**
 * A builder build — mirrors the site's `build` object (attributes{base/weapon/attunement},
 * meta, talents, mantras, equipment, boons/flaws).
 */
data class Build(
    val name: String = "",
    val level: Int = 0,
    val race: String = "None",          // meta.Race
    val origin: String = "Castaway",    // meta.Origin
    val oath: String = "Oathless",      // meta.Oath
    val bell: String = "None",          // meta.Bell
    val murmur: String = "",            // meta.Murmur ("Ardour"/"Rhythm"/"Tacet")
    val attributes: Attributes = Attributes.empty(),
    val talents: List<String> = emptyList(),
    val mantras: List<String> = emptyList(),
    val weapon: String = "",        // equipped weapon name (site: build.weapons)
    val enchant: String = "",       // weapon enchant ("" = none)
    val equipment: Map<String, String> = emptyMap(),   // slot ("Head","Arms",...) -> item name ("" = none)
    val outfit: String = "",            // outfit name ("" = none)
    val boons: List<String> = listOf("None", "None"),
    val flaws: List<String> = listOf("None", "None", "None"),
    val multifaceted: Boolean = false,  // "Multifaceted" checkbox (no racial bonus)
    val traits: Map<String, Int> = emptyMap(),   // Vitality/Erudition/Proficiency/Songchant (0-6 each, pool 12)
    val tuning: Tuning = Tuning(),      // weapon-tab damage tuning state
) {
    fun copyWith(patch: BuildPatch): Build = patch(this)

    companion object {
        fun empty(): Build = Build()
    }
}

/**
 * Weapon-tab "tuning" state (site refs `i`/`u` star count+mod, `d` resist slider,
 * `r` rings map, `m` enabled mods, `b`/`P` toggles, bullet picker).
 */
data class Tuning(
    val starCount: Int = 0,               // weapon stars 0-3
    val starMod: String = "",             // "DMG%" | "PEN%" | "WGT%"
    val resistPct: Double = 35.0,         // enemy resistance % for the weapon breakdown
    val rings: Map<String, Boolean> = emptyMap(),  // 5 stat keys + 3 special ring names
    val enabledMods: Set<String> = emptySet(),
    val bullet: String = "None",          // gun ammo ("None","Iron","Gold","Umbrite","Erisore","Irithine","Gale","Frost")
    val airborne: Boolean = false,
)

/**
 * Attribute point storage. Keys use the site's canonical names:
 * base    = Strength, Fortitude, Agility, Intelligence, Willpower, Charisma
 * weapon  = "Heavy Weapon", "Medium Weapon", "Light Weapon"
 * attunement = Flamecharm, Frostdraw, Thundercall, Galebreathe, Shadowcast, Ironsing, Bloodrend
 */
data class Attributes(
    val base: Map<String, Int>,
    val weapon: Map<String, Int>,
    val attunement: Map<String, Int>,
) {
    fun total(): Int = base.values.sum() + weapon.values.sum() + attunement.values.sum()

    /** Point value of a stat display name (site's `Pb`/`Mi`). Returns 0 for unknowns. */
    fun stat(name: String): Int = when (name) {
        in base -> base[name]!!
        in weapon -> weapon[name]!!
        in attunement -> attunement[name]!!
        else -> 0
    }

    companion object {
        val BASE_STATS = listOf("Strength", "Fortitude", "Agility", "Intelligence", "Willpower", "Charisma")
        val WEAPON_STATS = listOf("Heavy Weapon", "Medium Weapon", "Light Weapon")
        val ATTUNEMENT_STATS = listOf(
            "Flamecharm", "Frostdraw", "Thundercall", "Galebreathe", "Shadowcast", "Ironsing", "Bloodrend",
        )

        fun empty(): Attributes = Attributes(
            base = BASE_STATS.associateWith { 0 },
            weapon = WEAPON_STATS.associateWith { 0 },
            attunement = ATTUNEMENT_STATS.associateWith { 0 },
        )
    }
}

/** Functional patcher so UI code can make small build mutations immutably. */
fun interface BuildPatch {
    operator fun invoke(build: Build): Build
}