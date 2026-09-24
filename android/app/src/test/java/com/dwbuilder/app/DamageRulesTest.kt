package com.dwbuilder.app

import com.dwbuilder.app.domain.DamageRules
import com.dwbuilder.app.domain.model.Attributes
import com.dwbuilder.app.domain.model.Build
import com.dwbuilder.app.domain.model.TalentRequirements
import com.dwbuilder.app.domain.model.Tuning
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Port checks for the damage engine (`je` and helpers from CkKCXiBo.js). */
class DamageRulesTest {

    private val data get() = TestData.data

    private fun weapon(name: String) = DamageRules.DamageSource.weapon(data.weapon(name)!!)

    private fun attrsOf(
        base: Map<String, Int> = emptyMap(),
        weapon: Map<String, Int> = emptyMap(),
        attunement: Map<String, Int> = emptyMap(),
    ) = Attributes(base = base, weapon = weapon, attunement = attunement)

    @Test
    fun enforcerBladeWithUnmetRequirements() {
        val bd = DamageRules.compute(weapon("Enforcer's Blade"), Build(attributes = attrsOf()), data.mods, tuning = Tuning())
        assertEquals(25.5, bd.baseDamage, 1e-9)
        assertEquals(0.05, bd.basePenetration, 1e-9)
        assertEquals(1.0, bd.proficiencyMultiplier, 1e-9)
        assertEquals(25.5, bd.scaledDamage, 1e-9)
        assertEquals(0.0, bd.totalDamageMultiplier, 1e-9)
        assertEquals(0.0, bd.totalDamageMultiplierRaw, 1e-9)
        assertFalse(bd.damageMultiplierCapped)
        assertEquals(25.5, bd.damageWithMods, 1e-9)
        assertEquals(0.0, bd.bleedRate, 1e-9)
        assertEquals(0.75, bd.reqDebuff, 1e-9)                    // reqs unmet -> 25% damage loss
        assertEquals(25.5 * 0.75, bd.finalDamage, 1e-9)
        assertEquals(0.05, bd.effectivePenetration, 1e-9)
        val resisted = 25.5 * 0.75 * (1 - 0.95 * 0.35)            // resistPct defaults to 35
        assertEquals(resisted, bd.resistedDamage, 1e-9)
        assertEquals(resisted / 0.9, bd.dps, 1e-9)                // attackDuration "0.9s"
    }

    @Test
    fun scalingStarsAndStrength() {
        val build = Build(
            attributes = attrsOf(base = mapOf("Strength" to 50), weapon = mapOf("Heavy Weapon" to 50)),
            traits = mapOf("Proficiency" to 2),
        )
        val tuning = Tuning(starCount = 3, starMod = "PEN%")
        val bd = DamageRules.compute(weapon("Enforcer's Blade"), build, data.mods, tuning = tuning)

        assertEquals(25.5, bd.baseDamage, 1e-9)
        assertEquals(0.05 + 0.15, bd.basePenetration, 1e-9)       // star PEN% at 3 stars = +0.15
        assertEquals(10.0, bd.scalingContributions.single().scalingMultiplier, 1e-9)
        assertEquals(50.0, bd.scalingContributions.single().investment, 1e-9)

        val g = 0.75 * 50 * 10 / 1e3
        val v = 1 + 2 * 0.065
        assertEquals(v, bd.proficiencyMultiplier, 1e-9)
        assertEquals(25.5 * (1 + g * v), bd.scaledDamage, 1e-9)

        assertEquals(0.05 + 0.15 + 0.05 + 0.05, bd.effectivePenetration, 1e-9) // stars + base + STR + Prof

        val fraction = 50.0 / 80.0                                  // Heavy Weapon 50 of 80
        val reqDebuff = 1 - 0.25 * (1 - fraction)
        assertEquals(reqDebuff, bd.reqDebuff, 1e-9)
        assertEquals(bd.scaledDamage * reqDebuff, bd.finalDamage, 1e-9)
    }

    @Test
    fun starDmgMultiplier() {
        val tuning = Tuning(starCount = 2, starMod = "DMG%")
        val bd = DamageRules.compute(weapon("Enforcer's Blade"), Build(attributes = attrsOf()), data.mods, tuning = tuning)
        assertEquals(25.5 * 1.04, bd.baseDamage, 1e-9)
    }

    @Test
    fun gunBulletHelpers() {
        assertTrue(DamageRules.isGun("Pistol"))
        assertTrue(DamageRules.isGun("Rifle"))
        assertTrue(DamageRules.isGun("Guns"))
        assertFalse(DamageRules.isGun("Greatsword"))
        assertEquals(0.0, DamageRules.bulletDamage("None", false).damage, 1e-9)
        assertEquals(-0.05, DamageRules.bulletDamage("Gale", false).damage, 1e-9)
        assertEquals(0.25, DamageRules.bulletDamage("Gale", true).damage, 1e-9)
        assertEquals(0.1, DamageRules.bulletDamage("Iron", false).pen, 1e-9)
    }

    @Test
    fun pistolWithGaleBulletAirborne() {
        val build = Build(attributes = attrsOf(base = mapOf("Strength" to 30)))
        val bd = DamageRules.compute(
            weapon("Silversix"), build, data.mods, tuning = Tuning(bullet = "Gale", airborne = true),
        )
        assertEquals(10.0, bd.baseDamage, 1e-9)
        assertEquals(0.25, bd.totalDamageMultiplierRaw, 1e-9)      // Gale airborne dmg override
        assertEquals(10.0 * 1.25, bd.damageWithMods, 1e-9)
        assertEquals(1.0, bd.reqDebuff, 1e-9)                      // Silversix has no requirements
        assertEquals(10.0 * 1.25 * (1 - 0.97 * 0.35), bd.resistedDamage, 1e-9)
        // dps uses swingSpeed 1.17 when attackDuration is absent
        assertEquals(bd.resistedDamage * 1.17, bd.dps, 1e-9)
    }

    @Test
    fun grimModRequiresMatchingEnchant() {
        val build = Build(attributes = attrsOf(), enchant = "Grim")
        val tuning = Tuning(enabledMods = setOf("Grim"))
        val bd = DamageRules.compute(weapon("Enforcer's Blade"), build, data.mods, tuning = tuning)
        assertEquals(0.25, bd.totalDamageMultiplierRaw, 1e-9)
        assertEquals(25.5 * 1.25 * 0.75, bd.finalDamage, 1e-9)

        // wrong enchant -> Grim gated out; Deferred still adds its +7%
        val other = Build(attributes = attrsOf(), enchant = "Deferred")
        val bd2 = DamageRules.compute(weapon("Enforcer's Blade"), other, data.mods, tuning = tuning)
        assertEquals(0.07, bd2.totalDamageMultiplierRaw, 1e-9)
    }

    @Test
    fun mantraSourceComputesLikeTheSite() {
        val m = data.mantra("Burning Servants")!!
        val src = DamageRules.DamageSource.mantra(m, m.damage.first().levels.first())
        assertEquals(44.0, src.damage, 1e-9)
        val build = Build(attributes = attrsOf(attunement = mapOf("Flamecharm" to 60)))
        val bd = DamageRules.compute(src, build, data.mods, tuning = Tuning())
        assertEquals(44.0, bd.baseDamage, 1e-9)
        val g = 0.75 * (60 * 2.75) / 1e3
        assertEquals(44.0 * (1 + g), bd.scaledDamage, 1e-9)
        assertEquals(1.0, bd.reqDebuff, 1e-9)                     // Flamecharm 1 requirement met
        assertEquals(44.0 * (1 + g) * (1 - 0.35), bd.resistedDamage, 1e-9)
        assertEquals(0.0, bd.dps, 1e-9)
    }

    @Test
    fun khanBonusReducesRequirements() {
        val req12 = TalentRequirements(stats = mapOf("Strength" to 12))
        val plain = Build(attributes = attrsOf(base = mapOf("Strength" to 9)))
        val khan = plain.copy(race = "Khan")
        assertEquals(9.0 / 12.0, DamageRules.reqFraction(plain, req12, emptyMap()).fraction, 1e-9)
        assertFalse(DamageRules.reqFraction(plain, req12, emptyMap()).met)
        assertEquals(1.0, DamageRules.reqFraction(khan, req12, emptyMap()).fraction, 1e-9)  // need 12-3=9
        assertTrue(DamageRules.reqFraction(khan, req12, emptyMap()).met)
    }

    @Test
    fun orGroupRequirementsPickBestAlternative() {
        val or = TalentRequirements(
            or = listOf(
                TalentRequirements(stats = mapOf("Heavy Weapon" to 80)),
                TalentRequirements(stats = mapOf("Light Weapon" to 20)),
            ),
        )
        val build = Build(attributes = attrsOf(weapon = mapOf("Light Weapon" to 20)))
        val res = DamageRules.reqFraction(build, or, emptyMap())
        assertTrue(res.met)
        assertEquals(1.0, res.fraction, 1e-9)
    }

    @Test
    fun modAvailabilityGates() {
        val grim = data.mod("Grim")!!
        assertTrue(DamageRules.modAvailable(grim, emptySet(), emptySet(), "Grim", "", ""))
        assertFalse(DamageRules.modAvailable(grim, emptySet(), emptySet(), "Sear", "", ""))
        val ardour = data.mod("Ardour Weapon")!!
        assertTrue(DamageRules.modAvailable(ardour, emptySet(), emptySet(), null, "Ardour", ""))
        assertFalse(DamageRules.modAvailable(ardour, emptySet(), emptySet(), null, "Rhythm", ""))
    }

    @Test
    fun multiplierOnlyPanelCapsAtFiftyPercent() {
        val enabled = setOf("Grim", "Speed Demon")
        val rings = mapOf("Isshin's Ring" to true)
        val mr = DamageRules.multiplierOnly(data.mods, enabled, rings)
        assertEquals(0.25 + 0.15, mr.raw, 1e-9)
        assertEquals(0.25 + 0.15 / 2, mr.total, 1e-9)   // 0.4 > .25 -> halve the excess
        assertFalse(mr.capped)

        val empty = DamageRules.multiplierOnly(data.mods, emptySet(), emptyMap())
        assertEquals(0.0, empty.raw, 1e-9)
        assertEquals(0.0, empty.total, 1e-9)
        assertFalse(empty.capped)
    }
}