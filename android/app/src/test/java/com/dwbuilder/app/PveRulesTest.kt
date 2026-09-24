package com.dwbuilder.app

import com.dwbuilder.app.domain.PveRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Port checks for the PvE calculator (`Lr`, `rn`, `Ir`, `un`, `xr`, `qt` from C74yZVe_.js). */
class PveRulesTest {

    private val data get() = TestData.data

    @Suppress("UNCHECKED_CAST")
    private fun hitInput(overrides: Map<String, Any?> = emptyMap()): PveRules.HitInput {
        val base: PveRules.HitInput = PveRules.HitInput(
            weaponDamage = 100.0,
            power = 0.0,
            dvmPct = 0.0,
            dvmEffectiveness = 1.0,
            monsterScaling = true,
            health = listOf(1000.0, 1000.0),
            resistance = listOf(25.0, 25.0),
            staggered = false,
            penetration = 0.5,
            astral = false,
            magmaGuard = false,
            attunement = "None",
            damageTypes = listOf("Slash"),
        )
        var out = base
        for ((k, v) in overrides) {
            out = when (k) {
                "weaponDamage" -> out.copy(weaponDamage = v as Double)
                "power" -> out.copy(power = v as Double)
                "dvmPct" -> out.copy(dvmPct = v as Double)
                "dvmEffectiveness" -> out.copy(dvmEffectiveness = v as Double)
                "monsterScaling" -> out.copy(monsterScaling = v as Boolean)
                "health" -> out.copy(health = v as List<Double>)
                "resistance" -> out.copy(resistance = v as List<Double>?)
                "staggered" -> out.copy(staggered = v as Boolean)
                "penetration" -> out.copy(penetration = v as Double)
                "astral" -> out.copy(astral = v as Boolean)
                "magmaGuard" -> out.copy(magmaGuard = v as Boolean)
                "attunement" -> out.copy(attunement = v as String)
                "damageTypes" -> out.copy(damageTypes = v as List<String>)
                else -> error("unknown override $k")
            }
        }
        return out
    }

    @Test
    fun powerMultiplierScalesLinearly() {
        assertEquals(2.7, PveRules.powerMultiplier(1.0), 1e-9)
        assertEquals(2.7 + 9.0 / 19 * 4.86, PveRules.powerMultiplier(10.0), 1e-9)
        assertEquals(2.7 + 4.86, PveRules.powerMultiplier(20.0), 1e-9)
        assertEquals(2.7, PveRules.powerMultiplier(0.0), 1e-9)      // clamped to 1
        assertEquals(2.7, PveRules.powerMultiplier(Double.NaN), 1e-9) // non-finite -> 1
    }

    @Test
    fun effectiveDamageTypes() {
        assertEquals(listOf("Slash"), PveRules.effectiveDamageTypes(listOf("Slash", "Bleed", "Wither")))
        assertEquals(listOf("Flamecharm"), PveRules.effectiveDamageTypes(listOf("Flamecharm", "Bleed")))
        assertEquals(listOf("Blunt"), PveRules.effectiveDamageTypes(listOf("Bleed", "Wither")))
    }

    @Test
    fun attunementMatchups() {
        // flavor u[0] resisted, u[1] super-effective, others neutral
        assertEquals(0.5, PveRules.matchupMultiplier("Flamewreathed", listOf("Flamecharm")), 1e-9)
        assertEquals(2.0, PveRules.matchupMultiplier("Flamewreathed", listOf("Galebreathe")), 1e-9)
        assertEquals(1.0, PveRules.matchupMultiplier("Flamewreathed", listOf("Slash")), 1e-9)
        assertEquals(2.0, PveRules.matchupMultiplier("Frostmantle", listOf("Flamecharm")), 1e-9)
        assertEquals(0.5, PveRules.matchupMultiplier("Thunderstruck", listOf("Thundercall")), 1e-9)
        // multiple damage types: minimum wins
        assertEquals(0.5, PveRules.matchupMultiplier("Flamewreathed", listOf("Flamecharm", "Galebreathe")), 1e-9)
        // Shadowmeld: physical resisted, elemental super-effective
        assertEquals(0.5, PveRules.matchupMultiplier("Shadowmeld", listOf("Slash")), 1e-9)
        assertEquals(0.5, PveRules.matchupMultiplier("Shadowmeld", listOf("Blunt")), 1e-9)
        assertEquals(2.0, PveRules.matchupMultiplier("Shadowmeld", listOf("Flamecharm")), 1e-9)
        // no flavor -> neutral
        assertEquals(1.0, PveRules.matchupMultiplier("None", listOf("Slash")), 1e-9)
    }

    @Test
    fun hitBaseline() {
        val r = assertNotNull(PveRules.hit(hitInput()))
        assertEquals(2.7, r.powerMultiplier, 1e-9)                  // power 0 clamps to 1
        assertEquals(0.0, r.effectiveDvm, 1e-9)
        assertEquals(270.0, r.rawHit, 1e-9)                         // 100 * 2.7
        // 25% resist, 50% pen -> 25 * (1-0.5) = 12.5% effective resist
        val expected = 270.0 * (1 - 0.125)
        assertEquals(expected, r.damage[0], 1e-9)
        assertEquals(expected, r.damage[1], 1e-9)
        assertEquals(5.0, r.hits[0], 1e-9)                          // ceil(1000/236.25)
        assertEquals(5.0, r.hits[1], 1e-9)
    }

    @Test
    fun hitUsesBestDamageAgainstMinHealth() {
        // staggered removes the 0.5 matchup penalty against a Flamewreathed monster
        val r = assertNotNull(
            PveRules.hit(hitInput(mapOf("staggered" to true, "attunement" to "Flamewreathed", "damageTypes" to listOf("Flamecharm")))),
        )
        // matchup would be 0.5 but staggered floors it at 1, and resistance becomes 0
        assertEquals(270.0, r.damage[0], 1e-9)
        assertEquals(270.0, r.damage[1], 1e-9)
        assertEquals(4.0, r.hits[0], 1e-9)                          // ceil(1000/270 - eps)
    }

    @Test
    fun hitAstralMultipliesOnlyMonsterScaling() {
        val astral = assertNotNull(PveRules.hit(hitInput(mapOf("astral" to true))))
        assertEquals(270.0 * 1.2, astral.rawHit, 1e-9)
        val nonScaling = assertNotNull(
            PveRules.hit(hitInput(mapOf("astral" to true, "monsterScaling" to false))),
        )
        assertEquals(100.0, nonScaling.rawHit, 1e-9)                // no power mult, no astral
        assertEquals(1.0, nonScaling.powerMultiplier, 1e-9)
    }

    @Test
    fun hitDvmAndMagmaGuard() {
        val dvm = assertNotNull(PveRules.hit(hitInput(mapOf("dvmPct" to 25.0))))
        assertEquals(25.0, dvm.effectiveDvm, 1e-9)
        assertEquals(270.0 * 1.25, dvm.rawHit, 1e-9)
        // magma guard zeroes the DVM bonus
        val guard = assertNotNull(PveRules.hit(hitInput(mapOf("dvmPct" to 25.0, "magmaGuard" to true))))
        assertEquals(0.0, guard.effectiveDvm, 1e-9)
        assertEquals(270.0, guard.rawHit, 1e-9)
    }

    @Test
    fun hitRejectsInvalidInput() {
        assertNull(PveRules.hit(hitInput(mapOf("weaponDamage" to 0.0))))
        assertNull(PveRules.hit(hitInput(mapOf("health" to listOf(0.0, 1000.0)))))
        assertNull(PveRules.hit(hitInput(mapOf("health" to listOf(2000.0, 1000.0)))))
        assertNull(PveRules.hit(hitInput(mapOf("resistance" to null))))
        assertNull(PveRules.hit(hitInput(mapOf("damageTypes" to listOf("Slash"), "weaponDamage" to Double.NaN))))
        // staggered supplies its own zero resistance
        assertNotNull(PveRules.hit(hitInput(mapOf("resistance" to null, "staggered" to true))))
    }

    @Test
    fun analyzeParsesCorruptedVariants() {
        val bonekeeper = data.enemies.first { it.name == "Bonekeeper" }
        val info = PveRules.analyze(bonekeeper)
        val hp = info.hp ?: error("missing hp")
        assertEquals(9500.0, hp[0], 1e-9)
        assertEquals(9500.0, hp[1], 1e-9)
        assertEquals(1, info.variants.size)
        val v = info.variants[0]
        assertEquals("variant-0", v.id)
        assertEquals("Corrupted", v.label)
        assertEquals(24000.0, v.health, 1e-9)
        assertEquals("", info.note)
        assertTrue(info.monsterScaling)                             // Mini-Boss in Nr
        assertEquals(listOf(24000.0, 24000.0), PveRules.hpFor(info, "variant-0"))
        assertEquals(listOf(9500.0, 9500.0), PveRules.hpFor(info, null))
    }

    @Test
    fun analyzeKeepsUnparseableAdditivesAsNote() {
        val heart = data.enemies.first { it.name == "Heart of Enmity" }
        val info = PveRules.analyze(heart)
        assertTrue(info.variants.isEmpty())
        assertTrue(info.note.contains("30k per player"))
        assertFalse(info.monsterScaling == false && info.name == "Heart of Enmity") // still a World Boss
        assertTrue(info.monsterScaling)
    }

    @Test
    fun analyzeMarksNonScalingEnemies() {
        val plain = data.enemies.first { it.className !in PveRules.MONSTER_SCALING_CLASSES }
        assertFalse(PveRules.analyze(plain).monsterScaling)
    }

    @Test
    fun flavorListOrder() {
        assertEquals("None", PveRules.ATTUNEMENT_FLAVORS.first())
        assertEquals(6, PveRules.ATTUNEMENT_FLAVORS.size)
    }
}