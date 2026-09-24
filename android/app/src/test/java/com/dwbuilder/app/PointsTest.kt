package com.dwbuilder.app

import com.dwbuilder.app.domain.Points
import com.dwbuilder.app.domain.model.Attributes
import com.dwbuilder.app.domain.model.Build
import kotlin.test.Test
import kotlin.test.assertEquals

class PointsTest {

    private fun build(
        base: Map<String, Int> = emptyMap(),
        weapon: Map<String, Int> = emptyMap(),
        attunement: Map<String, Int> = emptyMap(),
    ): Build = Build(attributes = Attributes(base, weapon, attunement))

    @Test
    fun emptyBuildHasNoPointsSpent() {
        val b = build()
        assertEquals(0, Points.spent(b))
        assertEquals(0, Points.power(b))
        assertEquals(330, Points.pointsLeft(b))
        // site quirk: at power 0 it reports 15 to next power (Power 1 actually arrives at 30)
        assertEquals(15, Points.toNextPower(b))
    }

    @Test
    fun powerOneArrivesAtThirtyPoints() {
        val b15 = build(base = mapOf("Strength" to 15))
        assertEquals(0, Points.power(b15))
        val b30 = build(base = mapOf("Strength" to 30))
        assertEquals(1, Points.power(b30))
        assertEquals(30, Points.spent(b30))
        // at exactly a power-up boundary the site reports a full step (15) to the *next* boundary
        assertEquals(15, Points.toNextPower(b30))
    }

    @Test
    fun powerFormulaFloorSteps() {
        assertEquals(2, Points.power(build(base = mapOf("Strength" to 45))))
        assertEquals(2, Points.power(build(base = mapOf("Strength" to 59))))
        assertEquals(3, Points.power(build(base = mapOf("Strength" to 60))))
    }

    @Test
    fun pointsToNextPowerCountsDownWithinStep() {
        // t=44 → 1 point to power 2 (next boundary 45)
        assertEquals(1, Points.toNextPower(build(base = mapOf("Strength" to 44))))
        // t=46 → 14 points to power 3 (next boundary 60)
        assertEquals(14, Points.toNextPower(build(base = mapOf("Strength" to 46))))
        // t=1 → 29 points to power 1 (boundary 30)
        assertEquals(29, Points.toNextPower(build(base = mapOf("Strength" to 1))))
        // t=45 → power 2 exactly: full step (15) to 60
        assertEquals(15, Points.toNextPower(build(base = mapOf("Strength" to 45))))
    }

    @Test
    fun secondAttunementCostsOneExtraPoint() {
        // 20 + 20 points in two attunements → 40 − 1 discount = 39 spent
        val b = build(attunement = mapOf("Flamecharm" to 20, "Frostdraw" to 20))
        assertEquals(39, Points.spent(b))
        // a third attunement costs another point
        val b3 = build(attunement = mapOf("Flamecharm" to 20, "Frostdraw" to 20, "Thundercall" to 20))
        assertEquals(58, Points.spent(b3))
        // zeroed attunements don't count
        val bZero = build(attunement = mapOf("Flamecharm" to 20, "Frostdraw" to 0))
        assertEquals(20, Points.spent(bZero))
    }

    @Test
    fun fullBudgetIsPowerTwenty() {
        val b = build(base = mapOf("Strength" to 100, "Fortitude" to 100, "Agility" to 100, "Intelligence" to 30))
        assertEquals(330, Points.spent(b))
        assertEquals(0, Points.pointsLeft(b))
        assertEquals(20, Points.power(b))
        assertEquals(0, Points.toNextPower(b))
    }

    @Test
    fun sumAndMaxHelpers() {
        assertEquals(6, Points.sum(mapOf("a" to 1, "b" to 2, "c" to 3)))
        assertEquals(3, Points.max(mapOf("a" to 1, "b" to 2, "c" to 3)))
        assertEquals(0, Points.max(emptyMap()))
    }
}
