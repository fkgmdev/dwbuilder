package com.dwbuilder.app.domain

import com.dwbuilder.app.domain.model.Attributes
import com.dwbuilder.app.domain.model.Build
import com.dwbuilder.app.domain.model.Mantra
import com.dwbuilder.app.domain.model.MantraLevel
import com.dwbuilder.app.domain.model.Modifier
import com.dwbuilder.app.domain.model.TalentRequirements
import com.dwbuilder.app.domain.model.Tuning
import com.dwbuilder.app.domain.model.Weapon
import kotlin.math.max
import kotlin.math.min

/**
 * Weapon/mantra damage breakdown — exact port of `je()` from CkKCXiBo.js
 * (exported as `c`, imported by the site as `Bt`), plus its helpers
 * (`Pe`/`multiplierOnly`, `$`/`reqFraction`, `Se`, `Me`, `be`, `te`, `ye`, `Re`,
 * `de` star tables, `ge` bleed-cap set, `Ye`/`modAvailable`, `Z` bullets, `pe`, `he`).
 */
object DamageRules {

    /** Star-upgrade tables (site `de`), indexed by stars-1 (stars 1..3). */
    private val STAR_MODS = mapOf(
        "DMG%" to doubleArrayOf(0.02, 0.04, 0.06),
        "PEN%" to doubleArrayOf(0.05, 0.10, 0.15),
        "WGT%" to doubleArrayOf(0.04, 0.08, 0.12),
    )

    /** Letter-grade scaling values (site `Me`). */
    private val GRADES = mapOf(
        "S" to 1.5, "A" to 1.25, "B" to 1.0, "C" to 0.75, "D" to 0.5, "E" to 0.25, "F" to 0.0,
    )

    /** Weapon-stat aliases (site `be`). */
    private val ALIASES = mapOf(
        "Heavy Weapon" to "Heavy Wep.", "Medium Weapon" to "Medium Wep.", "Light Weapon" to "Light Wep.",
        "Heavy Wep." to "Heavy Wep.", "Medium Wep." to "Medium Wep.", "Light Wep." to "Light Wep.",
    )

    /** Stat keys whose ring scaling counts (site `ne`). */
    private val RING_STATS = listOf("Strength", "Agility", "Intelligence", "Willpower", "Charisma")

    /** Special rings with flat damage-multiplier effects (site `Ee`). */
    val SPECIAL_RINGS = listOf("Isshin's Ring", "Ring of Casters", "Blindseer's Ring")

    /** Mods that lift the penetration cap to 100% (site `ge`). */
    private val BLEED_CAP_MODS = setOf("Million Ton Piercer", "Ether Overdrive")

    /** Bullet ammo table (site `Z`). */
    data class Bullet(val name: String, val damage: Double, val pen: Double, val speed: Double, val note: String)

    val BULLETS = listOf(
        Bullet("None", 0.0, 0.0, 1.0, ""),
        Bullet("Iron", 0.0, 0.1, 1.0, "+10% PEN."),
        Bullet("Gold", -0.1, 0.0, 1.0, "Slows targets; -10% damage."),
        Bullet("Umbrite", 0.1, 0.0, 0.75, "Applies wither; +10% damage, 25% slower firing."),
        Bullet("Erisore", -0.2, 0.0, 1.0, "50% anti-heal for 12 seconds; -20% damage."),
        Bullet("Irithine", -0.2, 0.0, 1.0, "Increases shaky block; -20% damage."),
        Bullet("Gale", -0.05, 0.0, 1.0, "-5% damage on the ground; +25% while airborne."),
        Bullet("Frost", -0.05, 0.0, 1.0, "Freezes or detonates crystals after 7 hits; -5% damage."),
    )

    private val GUN_RE = Regex("^(Guns?|Pistols?|Rifles?)$", RegexOption.IGNORE_CASE)

    /** site `he`: is the weapon type a gun (bullet-using)? */
    fun isGun(type: String?): Boolean =
        type?.split(Regex("\\s*/\\s*"))?.any { GUN_RE.matches(it) } == true

    /** site `pe`: bullet stat block, replacing Gale's damage while airborne. */
    fun bulletDamage(bullet: String, airborne: Boolean): Bullet {
        val base = BULLETS.find { it.name == bullet } ?: BULLETS[0]
        return if (bullet == "Gale" && airborne) base.copy(damage = 0.25) else base
    }

    /** site `Se`: numeric value of a scaling factor (number or letter grade). */
    fun grade(value: Any?): Double = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: (GRADES[value.uppercase()] ?: 0.0)
        else -> 0.0
    }

    private fun ye(base: Map<String, Int>, racial: Map<String, Int>): Double =
        maxOf(
            (base["Strength"] ?: 0).toDouble() + (racial["Strength"] ?: 0),
            (base["Agility"] ?: 0).toDouble() + (racial["Agility"] ?: 0),
            (base["Fortitude"] ?: 0).toDouble() + (racial["Fortitude"] ?: 0),
        )

    private fun re(base: Map<String, Int>, racial: Map<String, Int>): Double =
        maxOf(
            (base["Intelligence"] ?: 0).toDouble() + (racial["Intelligence"] ?: 0),
            (base["Willpower"] ?: 0).toDouble() + (racial["Willpower"] ?: 0),
            (base["Charisma"] ?: 0).toDouble() + (racial["Charisma"] ?: 0),
        )

    /** site `te`: stat value used for scaling lookups (Body/Mind resolve through racial). */
    fun statForScaling(attrs: Attributes, stat: String, racial: Map<String, Int>): Double = when (stat) {
        "Body" -> ye(attrs.base, racial)
        "Mind" -> re(attrs.base, racial)
        else -> {
            val key = ALIASES[stat] ?: stat
            when {
                key in attrs.weapon -> (attrs.weapon[key] ?: 0).toDouble()
                stat in attrs.weapon -> (attrs.weapon[stat] ?: 0).toDouble()
                stat in attrs.attunement -> (attrs.attunement[stat] ?: 0).toDouble()
                stat in attrs.base -> (attrs.base[stat] ?: 0).toDouble()
                else -> 0.0
            }
        }
    }

    private val COMPUTED_STATS = setOf("Power", "Weapon", "Weapons", "Attunement")

    /** site `$`: requirements-met fraction used for the 25% req debuff. */
    fun reqFraction(build: Build, req: TalentRequirements?, racial: Map<String, Int>): ReqResult {
        if (req == null) return ReqResult(met = true, metaMet = true, fraction = 1.0)
        var a = 1.0
        for ((stat, need) in req.stats) {
            val g = max(0, need - Stats.bonusToward(build, stat, forWeapons = true))
            val current = if (stat in COMPUTED_STATS) Stats.statValue(build, stat).toDouble()
            else statForScaling(build.attributes, stat, racial)
            if (g > 0) a = min(a, max(0.0, current / g))
        }
        val equipped = build.equipment.values.filter { it.isNotEmpty() }
        var meta = req.unobtainable != true &&
            (req.resonance.isEmpty() || req.resonance.contains(build.bell)) &&
            (req.origin == null || build.origin == req.origin) &&
            (req.aspect == null || build.race == req.aspect) &&
            (req.murmur == null || build.murmur == req.murmur) &&
            (req.outfit == null || build.outfit == req.outfit) &&
            (req.weapon == null || build.weapon == req.weapon) &&
            (req.equipment == null || equipped.contains(req.equipment)) &&
            req.talents.all { it in build.talents } &&
            req.mantras.all { it in build.mantras }
        if (req.or.isNotEmpty()) {
            val l = req.or.map { reqFraction(build, it, racial) }
            val h = l.filter { it.metaMet }
            val best = (if (h.isNotEmpty()) h else l).maxByOrNull { it.fraction }!!
            meta = meta && best.metaMet
            a = min(a, best.fraction)
        }
        return ReqResult(met = meta && a >= 1.0, metaMet = meta, fraction = a)
    }

    data class ReqResult(val met: Boolean, val metaMet: Boolean, val fraction: Double)

    /** site `Ye`: whether a damage mod unlock requirement is satisfied. */
    fun modAvailable(mod: Modifier, takenTalents: Set<String>, takenMantras: Set<String>, enchant: String?, murmur: String, oath: String): Boolean {
        if (mod.requiresEnchant != null) return mod.requiresEnchant == enchant
        if (mod.alwaysAvailable) return true
        for (r in mod.requires) {
            if (r.startsWith("Mantra: ")) { if (r.substring(8) in takenMantras) return true } else if (r in takenTalents) return true
        }
        return (mod.requiresMetaMurmur.isNotEmpty() && murmur in mod.requiresMetaMurmur) ||
            (mod.requiresMetaOath.isNotEmpty() && oath in mod.requiresMetaOath)
    }

    // ---------- damage source ----------

    /** The item being computed (site `t`): a weapon or a mantra level. */
    data class DamageSource(
        val name: String,
        val type: String,
        val damage: Double,
        val penetration: Double,
        val scaling: Map<String, Double>,
        /** null = field absent in source data — enchant effects still apply (site JS semantics). */
        val enchantable: Boolean?,
        val damageTypes: List<String>,
        val attackDuration: String?,
        val swingSpeed: Double,
        val endlag: String?,
        val requirements: TalentRequirements?,
    ) {
        companion object {
            fun weapon(w: Weapon): DamageSource = DamageSource(
                name = w.name, type = w.type, damage = w.damage, penetration = w.penetration,
                scaling = w.scaling, enchantable = w.enchantable, damageTypes = w.damageTypes,
                attackDuration = w.attackDuration, swingSpeed = w.swingSpeed, endlag = w.endlag,
                requirements = w.requirements,
            )

            fun mantra(m: Mantra, level: MantraLevel): DamageSource = DamageSource(
                name = m.name, type = m.type, damage = level.damage ?: 0.0, penetration = 0.0,
                scaling = m.scaling, enchantable = null, damageTypes = emptyList(),
                attackDuration = null, swingSpeed = 0.0, endlag = null, requirements = m.requirements,
            )
        }
    }

    // ---------- result types ----------

    data class ScalingContribution(
        val stat: String,
        val scalingMultiplier: Double,
        val investment: Double,
        val rawTerm: Double,
        var contribution: Double,
    )

    data class RingContribution(
        val stat: String,
        val rank: Int,
        val investment: Double,
        var contribution: Double,
    )

    data class Breakdown(
        val baseDamage: Double,
        val basePenetration: Double,
        val scalingContributions: List<ScalingContribution>,
        val ringContributions: List<RingContribution>,
        val proficiencyMultiplier: Double,
        val scaledDamage: Double,
        val totalDamageMultiplier: Double,
        val totalDamageMultiplierRaw: Double,
        val damageMultiplierCapped: Boolean,
        val damageWithMods: Double,
        val bleedRate: Double,
        val bleedDamage: Double,
        val totalDamage: Double,
        val reqDebuff: Double,
        val finalDamage: Double,
        val effectivePenetration: Double,
        val resistedDamage: Double,
        val dps: Double,
    )

    data class MultiplierResult(val total: Double, val raw: Double, val capped: Boolean)

    private fun jsParseFloat(s: String?): Double {
        if (s.isNullOrBlank()) return 0.0
        return Regex("^[-+]?\\d*\\.?\\d+").find(s.trim())?.value?.toDoubleOrNull() ?: 0.0
    }

    // ---------- main compute (`je`) ----------

    fun compute(source: DamageSource, build: Build, mods: List<Modifier>, racial: Map<String, Int> = emptyMap(), tuning: Tuning): Breakdown {
        var a = source.damage
        var r = source.penetration
        val star = tuning.starCount
        if (star > 0 && tuning.starMod.isNotEmpty()) {
            val w = STAR_MODS[tuning.starMod]?.getOrNull(star - 1) ?: 0.0
            if (tuning.starMod == "DMG%") a *= 1 + w else if (tuning.starMod == "PEN%") r += w
        }
        val scaling = mutableListOf<ScalingContribution>()
        var h = 0.0
        for ((stat, raw) in source.scaling) {
            val factor = grade(raw)
            if (factor == 0.0) continue
            val investment = statForScaling(build.attributes, stat, racial)
            val term = investment * factor
            h += term
            scaling.add(ScalingContribution(stat, factor, investment, term, 0.0))
        }
        val g = 0.75 * h / 1e3
        val rings = mutableListOf<RingContribution>()
        var d = 0.0
        val active = RING_STATS
            .filter { tuning.rings[it] == true }
            .map { it to ((build.attributes.base[it] ?: 0) + (racial[it] ?: 0)) }
            .sortedByDescending { it.second }
        active.forEachIndexed { idx, (stat, w) ->
            val rank = idx + 1
            val c = 1.2 * w / (Math.pow(2.0, idx.toDouble()) * 1e3)
            d += c
            rings.add(RingContribution(stat, rank, w.toDouble(), c))
        }
        val proficiency = (build.traits["Proficiency"] ?: 0).toDouble()
        val v = 1 + proficiency * 0.065
        val scaled = a * (1 + (g + d) * v)
        for (sc in scaling) sc.contribution = a * (0.75 * sc.rawTerm / 1e3) * v
        for (rc in rings) rc.contribution = a * rc.contribution * v

        val strength = (build.attributes.base["Strength"] ?: 0) + (racial["Strength"] ?: 0)
        val bullet = if (isGun(source.type)) bulletDamage(tuning.bullet, tuning.airborne) else BULLETS[0]
        var m = bullet.damage
        var s = strength / 10 * 0.01 + proficiency * 0.025
        s += bullet.pen
        var o = 1.0
        var u = 0.0
        var y = false
        val weaponApplies = { mod: Modifier -> mod.weapons.isEmpty() || source.type in mod.weapons }

        for (mod in mods) {
            if (mod.name !in tuning.enabledMods || !weaponApplies(mod)) continue
            val gated = mod.requiresEnchant != null && (source.enchantable == false || mod.requiresEnchant != build.enchant)
            if (gated) continue
            if (mod.bucket == "Normal") {
                if (mod.effectDmg != 0.0) m += mod.effectDmg
                if (mod.effectPen != 0.0) { if (mod.multiplicative) o += mod.effectPen else s += mod.effectPen }
                if (mod.effectBleed != 0.0) u = max(u, mod.effectBleed)
                if (mod.name in BLEED_CAP_MODS) y = true
            }
        }
        val ringsMap = tuning.rings
        if (ringsMap["Isshin's Ring"] == true) m += 0.15
        if (ringsMap["Ring of Casters"] == true) m -= 0.05
        if (ringsMap["Blindseer's Ring"] == true) m -= 0.15

        val outfit = build.outfit
        if (outfit == "Navaen Warchief" && source.type == "Fists") s += 0.25
        else if ((outfit == "Legion Centurion" || outfit == "Justicar's Armour") && source.type == "Fists") m += 0.1
        else if (outfit == "Royal Etrean Guard" && (source.name == "Katana" || source.name == "Alloyed Katana")) m += 0.25
        else if (outfit == "Royal Etrean Guard" && source.name == "Shattered Katana") m += 0.05

        val appliedEnchant = if (source.enchantable == false) "" else build.enchant
        when (appliedEnchant) {
            "Deferred" -> m += 0.07
            "Curse of the Bloodthirsty" -> m += 0.1
            "Heroism" -> m += 0.2
            "Sear" -> r += 0.05
        }

        val rawMult = m
        var capped = false
        if (m > 0.25) {
            m = 0.25 + (m - 0.25) / 2
            if (m > 0.5) { m = 0.5; capped = true }
        }
        var D = r + s
        if (D > 0.5 && !y) D = 0.5
        if (D > 1.0) D = 1.0
        D = min(if (y) 1.0 else 0.5, D * o)

        for (mod in mods) {
            if (mod.bucket == "Normal" || mod.name !in tuning.enabledMods || !weaponApplies(mod)) continue
            val gated = mod.requiresEnchant != null && (source.enchantable == false || mod.requiresEnchant != build.enchant)
            if (gated) continue
            if (mod.effectDmg != 0.0) m += mod.effectDmg
            if (mod.effectPen != 0.0 && (mod.requiresEnchant == null || mod.requiresEnchant == appliedEnchant)) D += mod.effectPen
        }
        if (source.damageTypes.any { it == "Bleed" }) u = max(u, 0.15)

        val x = scaled * u
        val withMods = scaled * (1 + m)
        val total = withMods + x
        val fraction = reqFraction(build, source.requirements, racial).fraction
        val reqDebuff = 1 - 0.25 * (1 - fraction)
        val final = total * reqDebuff
        val effectivePen = min(1.0, max(0.0, D))
        val resist = max(0.0, min(100.0, tuning.resistPct)) / 100
        val resisted = final * (1 - (1 - effectivePen) * resist)
        val attackDuration = jsParseFloat(source.attackDuration)
        val ce = jsParseFloat(source.endlag)
        val perAttack = if (attackDuration > 0) attackDuration
        else if (source.swingSpeed > 0) 1 / source.swingSpeed + ce else 0.0
        val dps = if (perAttack > 0) resisted / perAttack * bullet.speed else 0.0

        return Breakdown(
            baseDamage = a,
            basePenetration = r,
            scalingContributions = scaling,
            ringContributions = rings,
            proficiencyMultiplier = v,
            scaledDamage = scaled,
            totalDamageMultiplier = m,
            totalDamageMultiplierRaw = rawMult,
            damageMultiplierCapped = capped,
            damageWithMods = withMods,
            bleedRate = u,
            bleedDamage = x,
            totalDamage = total,
            reqDebuff = reqDebuff,
            finalDamage = final,
            effectivePenetration = effectivePen,
            resistedDamage = resisted,
            dps = dps,
        )
    }

    /** site `Pe`: damage-multiplier panel when no weapon is selected. */
    fun multiplierOnly(mods: List<Modifier>, enabledMods: Set<String>, rings: Map<String, Boolean>): MultiplierResult {
        var t = 0.0
        for (r in mods) {
            if (r.name !in enabledMods) continue
            if (r.weapons.isNotEmpty() || r.requiresEnchant != null || (r.bucket == "Normal" && r.effectDmg != 0.0)) {
                if (r.bucket == "Normal" && r.effectDmg != 0.0) t += r.effectDmg
            }
        }
        if (rings["Isshin's Ring"] == true) t += 0.15
        if (rings["Ring of Casters"] == true) t -= 0.05
        if (rings["Blindseer's Ring"] == true) t -= 0.15
        val raw = t
        var capped = false
        if (t > 0.25) {
            t = 0.25 + (t - 0.25) / 2
            if (t > 0.5) { t = 0.5; capped = true }
        }
        for (r in mods) {
            if (r.bucket == "Normal" || r.name !in enabledMods) continue
            if (r.weapons.isNotEmpty() || r.effectDmg != 0.0) {
                if (r.effectDmg != 0.0) t += r.effectDmg
            }
        }
        return MultiplierResult(total = t, raw = raw, capped = capped)
    }
}