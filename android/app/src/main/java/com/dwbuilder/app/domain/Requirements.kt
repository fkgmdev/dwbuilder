package com.dwbuilder.app.domain

import com.dwbuilder.app.domain.model.Build
import com.dwbuilder.app.domain.model.Talent
import com.dwbuilder.app.domain.model.TalentRequirements
import com.dwbuilder.app.domain.model.Weapon

/** Result of a requirement check (site: `{hardMet, warnings}`). */
data class RequirementResult(
    val hardMet: Boolean,
    val warnings: List<String>,
) {
    companion object {
        val MET = RequirementResult(true, emptyList())
    }
}

/**
 * Immutable context for evaluating requirements against a build.
 */
class BuildContext(
    val build: Build,
    val talents: Map<String, Talent>,
    val weapons: Map<String, Weapon>,
    val grantedTalents: Set<String> = emptySet(),
) {
    val takenTalents: Set<String> = build.talents.toSet()
    fun statValue(name: String): Int = Stats.statValue(build, name)
    fun talent(name: String): Talent? = talents[name]
    fun weapon(name: String): Weapon? = weapons[name]
}

/**
 * Stat value lookup — exact port of `Pb`/`Mi` from KwKd8cfB.js:
 *  Power → computed power; Body → max(STR,AGL,FTD); Mind → max(INT,WLL,CHA);
 *  Weapon/Weapons → max weapon value; Attunement → max attunement value; else stored points.
 */
object Stats {
    fun statValue(build: Build, name: String): Int {
        val a = ShrineRules.effectiveAttributes(build)
        return when (name) {
            "Power" -> Points.power(build)
            "Body" -> maxOf(a.base["Strength"] ?: 0, a.base["Agility"] ?: 0, a.base["Fortitude"] ?: 0)
            "Mind" -> maxOf(a.base["Intelligence"] ?: 0, a.base["Willpower"] ?: 0, a.base["Charisma"] ?: 0)
            "Weapon", "Weapons" -> Points.max(a.weapon)
            "Attunement" -> Points.max(a.attunement)
            else -> a.stat(name)
        }
    }

    private val weaponStats = setOf(
        "Heavy Weapon", "Medium Weapon", "Light Weapon",
        "Heavy Wep.", "Medium Wep.", "Light Wep.",
    )

    /**
     * Bonus points a build grants toward a stat outside attribute points (site's `Ib`).
     * Khan grants +3 to everything except Power; Silentheart grants +25 to weapon skills.
     * `forWeapons` selects the mode (site: n === "weapon").
     */
    fun bonusToward(build: Build, stat: String, forWeapons: Boolean): Int {
        if (!forWeapons) return 0
        var bonus = 0
        if (build.race == "Khan" && stat != "Power") bonus += 3
        if (build.oath == "Silentheart" && stat in weaponStats) bonus += 25
        return bonus
    }
}

/**
 * The requirement checker — exact port of `nr()` from KwKd8cfB.js.
 * `hardMet` is only false for stats/or/talents prerequisites (early returns);
 * origin/murmur/aspect/weaponType only add soft warnings. Talents gated on
 * those meta keys are still rejected by eligibility (`Kw`).
 */
object Requirements {

    fun check(requirements: TalentRequirements?, ctx: BuildContext): RequirementResult =
        check(requirements, ctx, HashSet())

    fun check(
        requirements: TalentRequirements?,
        ctx: BuildContext,
        recursion: MutableSet<String>,
        forWeapons: Boolean = false,
    ): RequirementResult {
        if (requirements == null) return RequirementResult.MET
        val warnings = mutableListOf<String>()

        // -- stats --
        if (requirements.stats.isNotEmpty()) {
            for ((stat, need) in requirements.stats) {
                val adjusted = (need - Stats.bonusToward(ctx.build, stat, forWeapons)).coerceAtLeast(0)
                val current = ctx.statValue(stat)
                if (current < adjusted) {
                    warnings += "$stat: requires $adjusted, currently $current (need ${adjusted - current} more)"
                }
            }
            if (warnings.isNotEmpty()) return RequirementResult(false, warnings)
        }

        // -- or: any alternative group --
        if (requirements.or.isNotEmpty()) {
            var chosen: RequirementResult? = null
            val failures = mutableListOf<String>()
            val candidates =
                if (ctx.statValue("Attunement") > 0 &&
                    requirements.or.any { (it.stats["Attunement"] ?: 0) > 0 }
                ) {
                    requirements.or.filter { (it.stats["Attunement"] ?: 0) > 0 }
                } else {
                    requirements.or
                }
            for (c in candidates) {
                val f = check(c, ctx, recursion, forWeapons)
                if (!f.hardMet) {
                    failures += f.warnings.joinToString("; ").ifEmpty { "Requirements not met" }
                }
                if (f.hardMet) {
                    if (chosen == null || f.warnings.size < chosen.warnings.size) chosen = f
                    if (f.warnings.isEmpty()) break
                }
            }
            if (chosen == null) {
                return RequirementResult(false, listOf("Requires one alternative: ${failures.joinToString(" OR ")}"))
            }
            warnings += chosen.warnings
        }

        // -- talents (recursive, with cycle guard) --
        for (required in requirements.talents) {
            val taken = ctx.takenTalents.contains(required)
            if (required in ctx.grantedTalents) continue
            if (required in recursion) {
                if (!taken) warnings += "Requires talent: $required"
                continue
            }
            val t = ctx.talent(required)
            if (t != null) {
                recursion.add(required)
                val inner = check(t.requirements, ctx, recursion, forWeapons)
                recursion.remove(required)
                if (!inner.hardMet) {
                    val why = inner.warnings.joinToString("; ").ifEmpty { "Requirements not met" }
                    return RequirementResult(false, listOf("$required: $why"))
                }
            }
            if (!taken) warnings += "Requires talent: $required"
        }

        // -- meta checks --
        if (requirements.origin != null && ctx.build.origin != requirements.origin) {
            warnings += "Requires Origin: ${requirements.origin}"
        }
        if (requirements.murmur != null && ctx.build.murmur != requirements.murmur) {
            warnings += "Requires Murmur: ${requirements.murmur}"
        }
        if (requirements.aspect != null && ctx.build.race != requirements.aspect) {
            warnings += "Requires Race: ${requirements.aspect}"
        }
        if (requirements.weaponType != null) {
            val w = ctx.build.weapon.takeIf { it.isNotEmpty() }?.let { ctx.weapon(it) }
            val types = mutableSetOf<String>()
            for (k in w?.scaling?.keys ?: emptySet()) {
                if (k == "Heavy Weapon" || k == "Medium Weapon" || k == "Light Weapon") types += k
            }
            w?.type?.split(Regex("\\s*/\\s*"))?.forEach { t ->
                types += t
                if (t == "Rifle") types += "Rifles"
                if (t == "Fists") types += "Fist"
            }
            if (w?.type == "Fist") types += "Fists"
            if (!types.contains(requirements.weaponType)) {
                warnings += "Requires Weapon Type: ${requirements.weaponType}"
            }
        }

        return RequirementResult(true, warnings) // site: `return {hardMet:true, warnings:o}` — soft warnings only
    }
}