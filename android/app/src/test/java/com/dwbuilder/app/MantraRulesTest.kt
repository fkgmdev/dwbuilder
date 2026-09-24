package com.dwbuilder.app

import com.dwbuilder.app.domain.MantraRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MantraRulesTest {

    private val lookup: (String) -> com.dwbuilder.app.domain.model.Mantra? = { TestData.data.mantra(it) }

    @Test
    fun baseSlotsBeforeOath() {
        assertEquals(
            mapOf("Combat" to 3, "Mobility" to 1, "Support" to 1, "Wisp" to 0, "Wildcard" to 1),
            MantraRules.availableSlots(emptyList(), null),
        )
    }

    @Test
    fun oathAddsItsSlots() {
        val blindseer = TestData.data.oath("Blindseer")!!
        val slots = MantraRules.availableSlots(emptyList(), blindseer)
        assertEquals(4, slots["Combat"])  // 3 base + 1 oath
        assertEquals(2, slots["Support"]) // 1 base + 1 oath
        assertEquals(2, slots["Wildcard"]) // 1 base + 1 oath
        assertEquals(1, slots["Mobility"])
    }

    @Test
    fun specialTalentsAddWispAndWildcard() {
        val slots = MantraRules.availableSlots(listOf("Neuroplasticity", "Will o' Wisp", "Chorus of Souls"), null)
        assertEquals(2, slots["Wisp"])
        assertEquals(2, slots["Wildcard"])
    }

    @Test
    fun overflowWhenSlotsAreFull() {
        // take every Combat mantra available in the dataset (far more than the 3 combat + 1 wildcard slots)
        val combatNames = TestData.data.mantras.values.filter { it.category == "Combat" }.map { it.name }
        assertTrue(combatNames.size >= 5, "expected several combat mantras, got ${combatNames.size}")
        val available = combatNames.take(5)
        val slots = MantraRules.availableSlots(emptyList(), null) // 3 combat, 1 wildcard
        val assigned = MantraRules.assign(available, lookup, slots)
        assertEquals(available.size, assigned.counts["Combat"]!! + assigned.counts["Wildcard"]!! + assigned.overflow)
        assertTrue(assigned.overflow >= 1)
    }

    @Test
    fun unknownMantraIsIgnored() {
        val assigned = MantraRules.assign(listOf("Not A Mantra"), lookup, MantraRules.availableSlots(emptyList(), null))
        assertEquals(0, assigned.overflow)
    }
}
