package com.dwbuilder.app

import com.dwbuilder.app.domain.TalentRules
import com.dwbuilder.app.domain.model.Build
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TalentRulesTest {

    @Test
    fun capsScaleDownWithMantras() {
        assertEquals(TalentRules.TalentCaps(maxTotal = 76, roll2 = 24), TalentRules.caps(0))
        assertEquals(74, TalentRules.caps(1).maxTotal)
        assertEquals(24, TalentRules.caps(1).roll2) // (13 − 1) × 2
        // roll2 floors at 0, maxTotal keeps falling
        assertEquals(0, TalentRules.caps(13).roll2)
        assertEquals(52 + (12 - 13) * 2, TalentRules.caps(13).maxTotal)
    }

    @Test
    fun capsConstants() {
        val c = TalentRules.caps(0)
        assertEquals(50, c.rollableBase)
        assertEquals(40, c.guaranteedRare)
        assertEquals(76, c.maxTotal)
    }

    @Test
    fun soulbreakerExcludesItsBonusTalents() {
        val scream = TestData.data.talent("Ardour Scream") ?: error("missing Ardour Scream")
        assertTrue(scream.countTowardsTalentTotal)
        assertFalse(TalentRules.countsTowardTotal(scream, "Soulbreaker", emptySet()))
        assertTrue(TalentRules.countsTowardTotal(scream, "Oathless", emptySet()))
        val spotter = TestData.data.talent("Spotter") ?: error("missing Spotter")
        assertTrue(TalentRules.countsTowardTotal(spotter, "Oathless", emptySet()))
        assertFalse(TalentRules.countsTowardTotal(spotter, "Soulbreaker", emptySet()))
    }

    @Test
    fun oathTalentOnlyEligibleForMatchingOath() {
        val arcwarder = TestData.data.talent("Oath: Arcwarder") ?: error("missing Oath: Arcwarder")
        assertFalse(TalentRules.eligible(arcwarder, Build(oath = "Oathless")))
        assertTrue(TalentRules.eligible(arcwarder, Build(oath = "Arcwarder")))
    }

    @Test
    fun innateTalentRequiresMatchingRace() {
        val innate = TestData.data.talents.values.first {
            it.rarity == "Innate" && it.requirements?.aspect != null
        }
        val aspect = innate.requirements!!.aspect!!
        assertTrue(TalentRules.eligible(innate, Build(race = aspect)))
        val other = TestData.data.aspects.keys.first { it != aspect }
        assertFalse(TalentRules.eligible(innate, Build(race = other)))
    }

    @Test
    fun factionCountSkipsGranted() {
        val factions = TestData.data.talents.values.filter { it.rarity == "Faction" }.take(3)
        assertEquals(3, factions.size)
        val names = factions.map { it.name }
        val build = Build(talents = names)
        val lookup: (String) -> com.dwbuilder.app.domain.model.Talent? = { TestData.data.talent(it) }
        assertEquals(3, TalentRules.factionCount(build, lookup, emptySet()))
        assertEquals(2, TalentRules.factionCount(build, lookup, setOf(names.first())))
    }

    @Test
    fun normalMantrasEatSlotsOthersDoNot() {
        val burning = TestData.data.mantra("Burning Servants") ?: error("missing Burning Servants")
        assertTrue(TalentRules.mantleEatsSlot(burning)) // type Normal
        val exotic = TestData.data.mantras.values.first { it.type != "Normal" }
        assertFalse(TalentRules.mantleEatsSlot(exotic))
    }
}
