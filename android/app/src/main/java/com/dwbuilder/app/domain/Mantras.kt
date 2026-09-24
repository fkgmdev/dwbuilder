package com.dwbuilder.app.domain

import com.dwbuilder.app.domain.model.Mantra
import com.dwbuilder.app.domain.model.Oath

/**
 * Mantra slot management — ported from KwKd8cfB.js (`Lb`, `zw`, `Qw`).
 */
object MantraRules {

    val SLOT_CATEGORIES = listOf("Combat", "Mobility", "Support", "Wisp", "Wildcard")

    /** Base slot counts before oath boosts and special talents. */
    val BASE_SLOTS: Map<String, Int> = mapOf(
        "Combat" to 3, "Mobility" to 1, "Support" to 1, "Wisp" to 0, "Wildcard" to 1,
    )

    private val mantraCategories = setOf("Combat", "Mobility", "Support", "Wisp")

    /**
     * Effective slot availability (site `zw`): base + oath slots, plus
     * Neuroplasticity → +1 Wildcard, "Will o' Wisp"/"Chorus of Souls" → +1 Wisp each.
     */
    fun availableSlots(takenTalents: List<String>, oath: Oath?): Map<String, Int> {
        val slots = BASE_SLOTS.toMutableMap()
        val oathSlots = oath?.slots
        if (oathSlots != null) {
            for (key in SLOT_CATEGORIES) slots[key] = slots[key]!! + (oathSlots[key] ?: 0)
        }
        if (takenTalents.contains("Neuroplasticity")) slots["Wildcard"] = slots["Wildcard"]!! + 1
        if (takenTalents.any { it.contains("Will o' Wisp") }) slots["Wisp"] = slots["Wisp"]!! + 1
        if (takenTalents.any { it.contains("Chorus of Souls") }) slots["Wisp"] = slots["Wisp"]!! + 1
        return slots
    }

    data class AssignedSlots(
        val counts: Map<String, Int>,
        val buckets: Map<String, List<Mantra>>,
        val overflow: Int,
    )

    /**
     * Assign taken mantras to their category bucket, spilling Wisp → Support →
     * Wildcard when a category is full (site `Qw`).
     */
    fun assign(takenMantraNames: List<String>, lookup: (String) -> Mantra?, slots: Map<String, Int>): AssignedSlots {
        val counts = slots.keys.associateWith { 0 }.toMutableMap()
        val buckets = slots.keys.associateWith { mutableListOf<Mantra>() }.toMutableMap()
        var overflow = 0
        for (name in takenMantraNames) {
            val mantra = lookup(name) ?: continue
            val category = mantra.category
            if (category !in mantraCategories) continue
            var placed: String?
            placed = if (counts[category]!! < slots[category]!!) {
                category
            } else if (category == "Wisp" && counts["Support"]!! < slots["Support"]!!) {
                "Support"
            } else if (counts["Wildcard"]!! < slots["Wildcard"]!!) {
                "Wildcard"
            } else {
                overflow++
                null
            }
            if (placed != null) {
                counts[placed] = counts[placed]!! + 1
                buckets[placed]!!.add(mantra)
            }
        }
        return AssignedSlots(counts, buckets.mapValues { it.value.toList() }, overflow)
    }
}