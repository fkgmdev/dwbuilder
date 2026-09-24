package com.dwbuilder.app.domain

import com.dwbuilder.app.domain.model.Build
import com.dwbuilder.app.domain.model.Mantra
import com.dwbuilder.app.domain.model.Talent

/**
 * Talent eligibility + caps — ported from KwKd8cfB.js (`Kw`) and C74yZVe_.js
 * (TalentsTab `ee`, `hn`/`qw`, `R` faction count).
 */
object TalentRules {

    /**
     * Talent eligibility independent of points (site `Kw`, exported as `a9`/`vt`).
     */
    fun eligible(talent: Talent, build: Build): Boolean {
        val oath = build.oath
        val origin = build.origin
        if (talent.rarity == "Oath" && talent.category != null && talent.category != oath) return false
        // (the site also checks a per-talent `attributes` list; no talents carry it, kept for parity)
        if (talent.requirements?.origin != null && talent.requirements.origin != origin) return false
        if (talent.rarity == "Origin" &&
            talent.requirements?.origin == null &&
            (talent.requirements?.or?.isEmpty() ?: true) &&
            talent.category != null && talent.category != origin
        ) return false
        if (talent.rarity == "Innate" &&
            talent.requirements?.aspect != null && talent.requirements.aspect != build.race
        ) return false
        return true
    }

    /** Does this talent count toward the 76-talent pool (site `qw`, exported as `aa`/`hn`)? */
    fun countsTowardTotal(talent: Talent, oath: String, granted: Set<String>): Boolean =
        talent.countTowardsTalentTotal &&
            talent.name !in granted &&
            !(oath == "Soulbreaker" && (talent.name == "Ardour Scream" || talent.name == "Spotter"))

    /** A mantra only eats talent slots when its type is "Normal" (site `Yw`, exported as `ab`/`gn`). */
    fun mantleEatsSlot(mantra: Mantra): Boolean = mantra.type == "Normal"

    private val slotCategories = setOf("Combat", "Mobility", "Support", "Wisp")

    /** Number of taken, non-exempt mantras (site TalentsTab `he`). */
    fun nonExemptMantraCount(build: Build, lookup: (String) -> Mantra?): Int {
        var count = 0
        for (name in build.mantras) {
            val mantra = lookup(name) ?: continue
            if (mantra.category in slotCategories && !mantleEatsSlot(mantra)) count++
        }
        return count
    }

    /**
     * Talent pool caps. `maxTotal = 52 + (12 − G)·2`, `roll2 = clamp((13 − G)·2, 0, 24)`
     * where G = number of taken non-exempt mantras (site TalentsTab `ee`).
     */
    data class TalentCaps(
        val maxTotal: Int,
        val rollableBase: Int = 50,
        val guaranteedRare: Int = 40,
        val roll2: Int,
    )

    fun caps(nonExemptMantraCount: Int): TalentCaps = TalentCaps(
        maxTotal = 52 + (12 - nonExemptMantraCount) * 2,
        roll2 = ((13 - nonExemptMantraCount) * 2).coerceIn(0, 24),
    )

    /** Number of taken faction talents not auto-granted (site TalentsTab `R`). */
    fun factionCount(build: Build, lookup: (String) -> Talent?, granted: Set<String>): Int =
        build.talents.count { name ->
            name !in granted && lookup(name)?.rarity == "Faction"
        }

    const val MAX_FACTION = 4
    const val MAX_FACTION_COMMAND_DIVISION = 5
}