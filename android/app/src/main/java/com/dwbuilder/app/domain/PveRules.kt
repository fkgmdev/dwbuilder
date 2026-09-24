package com.dwbuilder.app.domain

import com.dwbuilder.app.domain.model.Enemy
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * PvE calculator rules — exact ports from C74yZVe_.js:
 * `xr` (enemy analyze), `qt` (variant HP lookup), `rn` (power multiplier),
 * `un` (effective damage types), `qr`/`Ir` (attunement matchup), `Lr` (hit math).
 */
object PveRules {

    /** Monster-scaling enemy classes (site `Nr`): these use the power multiplier. */
    val MONSTER_SCALING_CLASSES = setOf(
        "Boss", "Mini-Boss", "World Boss", "Angel", "Bounder", "Brainsucker", "Carbuncle",
        "Construct", "Crab", "Gigamed", "Golem", "Knight", "Lionfish", "Megalodaunt",
        "Mudskipper", "Nautilodaunt", "Owl", "Terrapod", "Thresher",
    )

    /** Monster attunement flavors (site `Dr`); "None" is the default option. */
    val ATTUNEMENT_FLAVORS = listOf(
        "None", "Flamewreathed", "Frostmantle", "Galeforce", "Thunderstruck", "Shadowmeld",
    )

    /** Flavor -> [resisted damage type, effective damage type] (site `Ir` map). */
    private val FLAVOR_PAIRS = mapOf(
        "Flamewreathed" to listOf("Flamecharm", "Galebreathe"),
        "Frostmantle" to listOf("Frostdraw", "Flamecharm"),
        "Galeforce" to listOf("Galebreathe", "Thundercall"),
        "Thunderstruck" to listOf("Thundercall", "Frostdraw"),
    )

    /** site `xr` additive clause, e.g. "225,000 (Corrupted)". */
    private val ADDITIVES = Regex(
        "(?:^|,\\s*)((?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\.\\d+)?)\\s*\\(([^()]+)\\)(?=\\s*(?:,|$))",
    )

    data class EnemyVariant(val id: String, val label: String, val health: Double)

    data class EnemyInfo(
        val name: String,
        val aliases: List<String>,
        val hp: List<Double>?,
        val variants: List<EnemyVariant>,
        val note: String,
        val monsterScaling: Boolean,
    )

    private fun String.trimDelims(): String = trim { it == ',' || it.isWhitespace() }

    /** site `xr`: parse an enemy into HP range, HP variants, remaining note. */
    fun analyze(enemy: Enemy): EnemyInfo {
        val variants = mutableListOf<EnemyVariant>()
        val note = ADDITIVES.replace(enemy.healthAdditives) { m ->
            val health = m.groupValues[1].replace(",", "").toDoubleOrNull()
            if (health != null && health > 0 && health.isFinite()) {
                variants.add(EnemyVariant("variant-${variants.size}", m.groupValues[2].trim(), health))
            }
            ""
        }.trimDelims()
        val hp = if (enemy.health.isFinite() && enemy.health > 0) listOf(enemy.health, enemy.health) else null
        return EnemyInfo(
            name = enemy.name.trim(),
            aliases = enemy.aliases,
            hp = hp,
            variants = variants,
            note = note,
            monsterScaling = enemy.className in MONSTER_SCALING_CLASSES,
        )
    }

    /** site `qt`: HP pair for an enemy, using the given variant's health when selected. */
    fun hpFor(enemy: EnemyInfo, variantId: String?): List<Double>? {
        val v = enemy.variants.find { it.id == variantId } ?: return enemy.hp
        return listOf(v.health, v.health)
    }

    /** site `rn`: monster-scaling damage multiplier for a power value. */
    fun powerMultiplier(power: Double): Double {
        val p = if (power.isFinite()) power.coerceIn(1.0, 20.0) else 1.0
        return 2.7 + (p - 1) / 19 * 4.86
    }

    /** site `un`: the damage types that matter for the matchup (Bleed/Wither filtered). */
    fun effectiveDamageTypes(damageTypes: List<String>): List<String> {
        val filtered = damageTypes.filter { it != "Bleed" && it != "Wither" }
        return if (filtered.isNotEmpty()) filtered else listOf("Blunt")
    }

    private fun isPhysical(damageType: String): Boolean = damageType == "Slash" || damageType == "Blunt"

    /** site `Ir`: damage multiplier from the monster's attunement vs the weapon's damage types. */
    fun matchupMultiplier(attunement: String, damageTypes: List<String>): Double =
        effectiveDamageTypes(damageTypes).minOf { t ->
            if (attunement == "Shadowmeld") {
                if (isPhysical(t)) 0.5 else 2.0
            } else {
                val pair = FLAVOR_PAIRS[attunement]
                when {
                    pair == null -> 1.0
                    t == pair[0] -> 0.5
                    t == pair[1] -> 2.0
                    else -> 1.0
                }
            }
        }

    // ---------- site `Lr` ----------

    data class HitInput(
        val weaponDamage: Double,
        val power: Double,
        val dvmPct: Double,
        val dvmEffectiveness: Double,
        val monsterScaling: Boolean,
        val health: List<Double>,
        val resistance: List<Double>?,
        val staggered: Boolean,
        val penetration: Double,
        val astral: Boolean,
        val magmaGuard: Boolean,
        val attunement: String,
        val damageTypes: List<String>,
    )

    data class HitResult(
        val powerMultiplier: Double,
        val effectiveDvm: Double,
        val rawHit: Double,
        /** [min, max] damage per hit across the resistance range. */
        val damage: List<Double>,
        /** [min, max] hits needed to kill across the health/resistance ranges. */
        val hits: List<Double>,
    )

    fun hit(input: HitInput): HitResult? {
        val numeric = listOf(input.weaponDamage, input.power, input.dvmPct, input.penetration) + input.health
        if (numeric.any { !it.isFinite() }) return null
        if (input.weaponDamage <= 0) return null
        if (input.health.size < 2 || input.health[0] <= 0 || input.health[1] < input.health[0]) return null
        if (input.resistance == null && !input.staggered) return null

        val a = if (input.monsterScaling) powerMultiplier(input.power) else 1.0
        val i = if (input.magmaGuard || !input.monsterScaling) 0.0
        else max(0.0, input.dvmPct) * max(0.0, input.dvmEffectiveness)
        val u = input.weaponDamage * a * (1 + i / 100) * if (input.astral && input.monsterScaling) 1.2 else 1.0
        val d = min(1.0, max(0.0, input.penetration))
        val resistance = if (input.staggered) listOf(0.0, 0.0) else input.resistance!!
        if (resistance.any { !it.isFinite() }) return null
        val matchup = matchupMultiplier(input.attunement, input.damageTypes)
        val b = if (input.staggered) max(1.0, matchup) else matchup

        fun damageAt(resist: Double): Double =
            u * (1 - min(100.0, max(0.0, resist)) / 100 * (1 - d)) * b

        val lo = damageAt(resistance.max())
        val hi = damageAt(resistance.min())

        fun hitsToKill(hp: Double, dmg: Double): Double =
            if (dmg > 0) max(1.0, ceil(hp / dmg - 1e-10)) else Double.POSITIVE_INFINITY

        return HitResult(
            powerMultiplier = a,
            effectiveDvm = i,
            rawHit = u,
            damage = listOf(lo, hi),
            hits = listOf(hitsToKill(input.health[0], hi), hitsToKill(input.health[1], lo)),
        )
    }
}