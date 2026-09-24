package com.dwbuilder.app

import com.dwbuilder.app.domain.Points
import com.dwbuilder.app.domain.ShrineRules
import com.dwbuilder.app.domain.ShrineRules.StatRef
import com.dwbuilder.app.domain.ShrineRules.WithdrawGuard
import com.dwbuilder.app.domain.model.Attributes
import com.dwbuilder.app.domain.model.Build
import com.dwbuilder.app.domain.model.Talent
import com.dwbuilder.app.domain.model.TalentRequirements
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Behavioral tests for the shrine engine beyond the golden fixtures. */
class ShrineRulesTest {

    private fun attrs(
        base: Map<String, Int> = emptyMap(),
        weapon: Map<String, Int> = emptyMap(),
        attunement: Map<String, Int> = emptyMap(),
    ) = Attributes(base, weapon, attunement)

    private val str = StatRef("base", "Strength")

    // ------------------------------------------------------------ apply / load

    @Test
    fun applyOrderSnapshotsRedistributesAndSwitchesMode() {
        val b = Build(attributes = attrs(base = mapOf("Strength" to 100, "Fortitude" to 30, "Agility" to 10)))
        val r = ShrineRules.applyOrder(b, emptyMap())
        assertEquals("post", r.build.shrineMode)
        assertEquals(b.attributes, r.build.preShrine)
        assertEquals(attrs(base = mapOf("Strength" to 75, "Fortitude" to 32, "Agility" to 32)), r.build.attributes)
        // 140 points pre → 139 post (fractional floor loss) = 1 spare
        assertEquals(1, r.sparePoints)
    }

    @Test
    fun applyOrderOnEmptyBuildIsIdentity() {
        val b = Build()
        val r = ShrineRules.applyOrder(b, emptyMap())
        assertEquals(Attributes.empty(), r.build.attributes)
        assertEquals(0, r.sparePoints)
        assertEquals("post", r.build.shrineMode)
    }

    @Test
    fun saveAndLoadPrePostRoundTrip() {
        val b = Build(attributes = attrs(base = mapOf("Strength" to 40)))
        assertNull(ShrineRules.loadPre(b))
        assertNull(ShrineRules.loadPost(b))

        val pre = ShrineRules.savePre(b)!!
        assertEquals("pre", pre.shrineMode)
        assertEquals(b.attributes, pre.preShrine)
        val back = ShrineRules.loadPre(pre)!!
        assertEquals(pre.preShrine, back.attributes)
        assertEquals("pre", back.shrineMode)

        val post = ShrineRules.savePost(b)!!
        assertEquals("post", post.shrineMode)
        assertEquals(b.attributes, post.postShrine)
        val backPost = ShrineRules.loadPost(post)!!
        assertEquals(post.postShrine, backPost.attributes)
        assertEquals("post", backPost.shrineMode)

        // empty attributes refuse to snapshot
        assertNull(ShrineRules.savePre(Build()))
        assertNull(ShrineRules.savePost(Build()))
    }

    // ---------------------------------------------------------------- mastery

    @Test
    fun masteryBudgetIsSharedAcrossPhases() {
        val b = Build(attributes = attrs(base = mapOf("Strength" to 100, "Fortitude" to 100)))
        val one = ShrineRules.applyMasteries(b, mapOf("Strength" to 60), emptyMap(), false, emptyMap())
        assertEquals(60, one.preMastery["Strength"])

        // 60 + 50 = 110 > 100 → refused wholesale, build untouched
        val blocked = ShrineRules.applyMasteries(b, mapOf("Strength" to 60), mapOf("Fortitude" to 50), false, emptyMap())
        assertEquals(b, blocked)

        val ok = ShrineRules.applyMasteries(b, mapOf("Strength" to 60), mapOf("Fortitude" to 40), false, emptyMap())
        assertEquals(60, ok.preMastery["Strength"])
        assertEquals(40, ok.postMastery["Fortitude"])

        // single-phase apply shares the other phase's budget
        val over = ShrineRules.applyMastery(ok, "post", mapOf("Fortitude" to 45))
        assertEquals(40, over.postMastery["Fortitude"])
        val fine = ShrineRules.applyMastery(ok, "post", mapOf("Fortitude" to 40))
        assertEquals(40, fine.postMastery["Fortitude"])
    }

    @Test
    fun sanitizeDropsBogusKeysAndNonPositiveValues() {
        val b = Build(attributes = attrs(base = mapOf("Strength" to 100, "Agility" to 10, "Fortitude" to 5)))
        val dirty = ShrineRules.sanitize(b, mapOf("Strength" to 10, "NotAStat" to 5, "Agility" to 0, "Fortitude" to -3, "Fortitude" to 2))
        assertEquals(mapOf("Strength" to 10, "Fortitude" to 2), dirty)
    }

    @Test
    fun resetMasteryClearsCurrentPhase() {
        val b = Build(
            attributes = attrs(base = mapOf("Strength" to 100, "Fortitude" to 100)),
            shrineMode = "post",
            preMastery = mapOf("Strength" to 40),
            postMastery = mapOf("Fortitude" to 20),
        )
        val cleared = ShrineRules.resetMastery(b)
        assertEquals(mapOf("Strength" to 40), cleared.preMastery)
        assertTrue(cleared.postMastery.isEmpty())
    }

    // -------------------------------------------------------------- stepping

    @Test
    fun stepperPinsWhenATakenTalentWouldBreak() {
        val talent = Talent(
            name = "Greatsword Master",
            category = null, rarity = "Rare", description = "",
            requirements = TalentRequirements(stats = mapOf("Strength" to 90)),
            mutualExclusives = emptyList(), additionalInfo = "",
            stats = emptyMap(), roll2able = false, countTowardsTalentTotal = true, vaulted = false,
        )
        // withdrawal 10 → effective STR 90, talent still met
        val b = Build(
            attributes = attrs(base = mapOf("Strength" to 100)),
            talents = listOf("Greatsword Master"),
            preMastery = mapOf("Strength" to 10),
        )
        val talents = mapOf("Greatsword Master" to talent)
        assertEquals(WithdrawGuard.OK, ShrineRules.guard(b, talents, emptyMap(), "pre", str, 10))
        // +1 → effective 89 < 90 while currently satisfied → pinned
        assertEquals(WithdrawGuard.PINNED, ShrineRules.guard(b, talents, emptyMap(), "pre", str, 11))
        val info = ShrineRules.wouldPin(b, talents, emptyMap(), "pre", str, 11)
        assertTrue(info.blocked)
        assertTrue("Greatsword Master" in info.talents)
        // talents that don't count toward the total never pin
        val innate = talent.copy(
            name = "Innate Edge",
            countTowardsTalentTotal = false,
            requirements = TalentRequirements(stats = mapOf("Strength" to 90)),
        )
        val b2 = Build(
            attributes = attrs(base = mapOf("Strength" to 100)),
            talents = listOf("Innate Edge"),
            preMastery = mapOf("Strength" to 10),
        )
        assertEquals(WithdrawGuard.OK, ShrineRules.guard(b2, mapOf("Innate Edge" to innate), emptyMap(), "pre", str, 11))
    }

    @Test
    fun stepperPinsBelowRacialFloor() {
        val raceFloor = mapOf("Strength" to 15)
        // withdrawal 85 → effective 15, exactly the floor → allowed
        val b = Build(attributes = attrs(base = mapOf("Strength" to 100)), preMastery = mapOf("Strength" to 85))
        assertEquals(WithdrawGuard.OK, ShrineRules.guard(b, emptyMap(), raceFloor, "pre", str, 85))
        // +1 → 14 < 15 → pinned
        assertEquals(WithdrawGuard.PINNED, ShrineRules.guard(b, emptyMap(), raceFloor, "pre", str, 86))
        // non-base stats have no racial floor
        val ftd = StatRef("base", "Fortitude")
        val b2 = Build(attributes = attrs(base = mapOf("Fortitude" to 40)), preMastery = mapOf("Fortitude" to 39))
        assertEquals(WithdrawGuard.OK, ShrineRules.guard(b2, emptyMap(), raceFloor, "pre", ftd, 40))
    }

    @Test
    fun phaseLocksMirrorDialogue() {
        val b = Build(attributes = attrs(base = mapOf("Strength" to 40)))
        // no pre-shrine yet: post phase locked, pre phase editable
        assertEquals(WithdrawGuard.POST_LOCKED, ShrineRules.guard(b, emptyMap(), emptyMap(), "post", str, 1))
        assertEquals(WithdrawGuard.OK, ShrineRules.guard(b, emptyMap(), emptyMap(), "pre", str, 1))

        val pre = ShrineRules.savePre(b)!!
        // pre-shrine exists: pre phase needs the re-shrine flag
        assertEquals(WithdrawGuard.PRE_LOCKED, ShrineRules.guard(pre, emptyMap(), emptyMap(), "pre", str, 1))
        assertEquals(WithdrawGuard.OK, ShrineRules.guard(pre, emptyMap(), emptyMap(), "pre", str, 1, reshrine = true))
        assertEquals(WithdrawGuard.OK, ShrineRules.guard(pre, emptyMap(), emptyMap(), "post", str, 1))
    }

    @Test
    fun stepperRespectsBoundsAndBudget() {
        val base = attrs(base = mapOf("Strength" to 100, "Fortitude" to 100))
        val b = Build(
            attributes = base,
            preShrine = base, // post-phase editing requires a pre-shrine to exist
            preMastery = mapOf("Strength" to 60),
            postMastery = mapOf("Fortitude" to 39),
        )
        val ftd = StatRef("base", "Fortitude")
        // matches the current value
        assertEquals(WithdrawGuard.OK, ShrineRules.guard(b, emptyMap(), emptyMap(), "post", ftd, 39))
        // 99 combined + 1 = 100 → exactly at the limit
        assertEquals(WithdrawGuard.OK, ShrineRules.guard(b, emptyMap(), emptyMap(), "post", ftd, 40))
        // would push the shared budget to 101
        assertEquals(WithdrawGuard.BUDGET_EXCEEDED, ShrineRules.guard(b, emptyMap(), emptyMap(), "post", ftd, 41))
        // decrement restores budget, never blocked
        assertEquals(WithdrawGuard.OK, ShrineRules.guard(b, emptyMap(), emptyMap(), "post", ftd, 38))
        // can't go negative or past the stat's own value
        assertEquals(WithdrawGuard.BELOW_ZERO, ShrineRules.guard(b, emptyMap(), emptyMap(), "post", ftd, -1))
        assertEquals(WithdrawGuard.ABOVE_STAT, ShrineRules.guard(b, emptyMap(), emptyMap(), "post", ftd, 101))
    }

    @Test
    fun setWithdrawalDropsKeyAtZero() {
        val b = Build(attributes = attrs(base = mapOf("Strength" to 40)), preMastery = mapOf("Strength" to 3))
        assertEquals(mapOf("Strength" to 3), ShrineRules.setWithdrawal(b, "pre", str, 3).preMastery)
        val cleared = ShrineRules.setWithdrawal(b, "pre", str, 0)
        assertFalse("Strength" in cleared.preMastery)
    }

    @Test
    fun applyMasteriesWithReshrineReRunsShrineOfOrder() {
        val b = Build(attributes = attrs(base = mapOf("Strength" to 100, "Fortitude" to 30, "Agility" to 10)))
        val ran = ShrineRules.applyOrder(b, emptyMap()).build
        assertEquals(attrs(base = mapOf("Strength" to 75, "Fortitude" to 32, "Agility" to 32)), ran.attributes)

        // restore the pre-shrine snapshot and redistribute with the new pre-mastery
        val after = ShrineRules.applyMasteries(
            ran, mapOf("Strength" to 10), emptyMap(), reshrine = true, raceBonuses = emptyMap(),
        )
        // effective pre points {90,30,10} → mean 43⅓, STR pinned at 65, rest at 32
        assertEquals(attrs(base = mapOf("Strength" to 65, "Fortitude" to 32, "Agility" to 32)), after.attributes)
        assertEquals(attrs(base = mapOf("Strength" to 100, "Fortitude" to 30, "Agility" to 10)), after.preShrine)
        assertEquals("post", after.shrineMode)
        assertEquals(mapOf("Strength" to 10), after.preMastery)
    }

    // --------------------------------------------------------- effective stats

    @Test
    fun masteryWithdrawalsReduceEffectiveStatsPowerAndBudget() {
        val b = Build(
            attributes = attrs(base = mapOf("Strength" to 100, "Fortitude" to 30, "Agility" to 10)),
            preMastery = mapOf("Strength" to 25),
        )
        assertEquals(75, ShrineRules.effectiveAttributes(b).base["Strength"])
        assertEquals(115, Points.spent(b))
        assertEquals(6, Points.power(b))
        assertEquals(215, Points.pointsLeft(b))

        val clean = Build(attributes = attrs(base = mapOf("Strength" to 100, "Fortitude" to 30, "Agility" to 10)))
        assertEquals(140, Points.spent(clean))
        // post mode applies the post map instead of the pre map
        val post = Build(
            attributes = attrs(base = mapOf("Strength" to 100, "Fortitude" to 30, "Agility" to 10)),
            shrineMode = "post",
            preMastery = mapOf("Strength" to 25),
            postMastery = mapOf("Agility" to 10),
        )
        val postEff = ShrineRules.effectiveAttributes(post)
        assertEquals(100, postEff.base["Strength"]) // pre mastery ignored in post mode
        assertEquals(0, postEff.base["Agility"])    // post mastery applied
        assertEquals(130, Points.spent(post))
    }
}